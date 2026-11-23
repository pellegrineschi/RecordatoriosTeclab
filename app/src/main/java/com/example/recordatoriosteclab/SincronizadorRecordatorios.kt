
package com.example.recordatoriosteclab

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.UUID

// ==========================================
// RESULTADOS DE LA SINCRONIZACIÓN
// ==========================================

sealed interface ResultadoSincronizacion {

    data class Exito(
        val enviados: Int
    ) : ResultadoSincronizacion

    data class Error(
        val mensaje: String
    ) : ResultadoSincronizacion

    data object EnCurso : ResultadoSincronizacion
}


// ==========================================
// ERROR HTTP
// ==========================================

private class ErrorHttp(
    val codigo: Int
) : Exception("HTTP $codigo")


// ==========================================
// MOTOR DE SINCRONIZACIÓN
// ==========================================

class SincronizadorRecordatorios(

    private val baseDeDatos: RecordatoriosDatabase,

    private val api: RecordatoriosApi

) {

    private val dao = baseDeDatos.recordatorioDao()

    // Evita sincronizaciones simultáneas
    private val mutex = Mutex()


    // ======================================
    // SINCRONIZAR
    // ======================================

    suspend fun sincronizar():
            ResultadoSincronizacion {

        // Si ya existe una sincronización,
        // no iniciamos otra
        if (!mutex.tryLock()) {

            return ResultadoSincronizacion.EnCurso
        }

        try {

            // Recuperar operaciones pendientes
            val pendientes = dao.obtenerPendientes()

            var enviados = 0

            // Enviar cada recordatorio al servidor
            for (entidad in pendientes) {

                enviarRecordatorio(entidad)

                enviados++
            }

            // Recuperar los registros remotos
            descargarRecordatorios()

            return ResultadoSincronizacion.Exito(
                enviados
            )

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            return ResultadoSincronizacion.Error(
                interpretarError(e)
            )

        } finally {

            // Liberar el bloqueo
            mutex.unlock()
        }
    }


    // ======================================
    // ENVIAR UN RECORDATORIO
    // ======================================

    private suspend fun enviarRecordatorio(
        entidad: RecordatorioEntity
    ) {

        try {

            // Transformamos Room en DTO
            val dto = entidad.toDto()

            // Enviamos utilizando el UUID
            val respuesta = api.guardarRecordatorio(

                clave = entidad.claveSincronizacion,

                recordatorio = dto
            )

            val confirmado = obtenerContenido(
                respuesta
            )

            // Verificamos que el servidor confirme
            // exactamente los datos enviados
            require(confirmado == dto) {

                "Respuesta remota diferente al registro enviado"
            }

            val entidadConfirmada =
                confirmado.aEntidadRemota(entidad.id)

            // Actualizar Room de forma segura
            baseDeDatos.withTransaction {

                val actual = dao.obtenerPorClave(
                    entidad.claveSincronizacion
                )

                // Evitar sobrescribir modificaciones
                // realizadas durante la sincronización
                if (actual != null &&
                    actual.versionLocal ==
                    entidad.versionLocal
                ) {

                    check(
                        dao.actualizar(
                            entidadConfirmada.copy(

                                estadoSincronizacion =
                                    "SINCRONIZADO"
                            )
                        ) == 1
                    )
                }
            }

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            // Conservamos el registro local
            marcarError(entidad)

            throw e
        }
    }


    // ======================================
    // DESCARGAR REGISTROS DEL SERVIDOR
    // ======================================

    private suspend fun descargarRecordatorios() {

        val respuesta =
            api.obtenerRecordatorios()

        val remotos = obtenerContenido(respuesta)

        // Validamos todos los DTO antes
        // de modificar la base de datos
        val entidades = remotos.map {

            it.aEntidadRemota()
        }

        baseDeDatos.withTransaction {

            for (remoto in entidades) {

                val local = dao.obtenerPorClave(
                    remoto.claveSincronizacion
                )

                if (local == null) {

                    // Registro que todavía no existe
                    // en nuestra base local
                    dao.insertar(remoto)

                } else if (

                    local.estadoSincronizacion ==
                    "SINCRONIZADO" &&

                    remoto.versionLocal >
                    local.versionLocal

                ) {

                    // Actualizamos conservando
                    // el identificador local
                    dao.actualizar(

                        remoto.copy(
                            id = local.id
                        )
                    )
                }

                // Los cambios locales pendientes
                // nunca son sobrescritos.
            }
        }
    }


    // ======================================
    // MARCAR ERROR DE SINCRONIZACIÓN
    // ======================================

    private suspend fun marcarError(
        entidad: RecordatorioEntity
    ) {

        baseDeDatos.withTransaction {

            val actual = dao.obtenerPorClave(
                entidad.claveSincronizacion
            )

            if (actual != null &&
                actual.versionLocal ==
                entidad.versionLocal
            ) {

                dao.actualizar(

                    actual.copy(
                        estadoSincronizacion = "ERROR"
                    )
                )
            }
        }
    }


    // ======================================
    // INTERPRETAR RESPUESTAS HTTP
    // ======================================

    private fun <T> obtenerContenido(
        respuesta: Response<T>
    ): T {

        if (!respuesta.isSuccessful) {

            throw ErrorHttp(respuesta.code())
        }

        return requireNotNull(respuesta.body()) {

            "El servidor devolvió una respuesta vacía"
        }
    }


    // ======================================
    // INTERPRETAR ERRORES
    // ======================================

    private fun interpretarError(
        error: Exception
    ): String {

        return when (error) {

            is ErrorHttp -> {

                when (error.codigo) {

                    400 ->
                        "Solicitud inválida (HTTP 400)"

                    409 ->
                        "Conflicto de versiones (HTTP 409)"

                    in 500..599 ->
                        "Error del servidor (HTTP ${error.codigo})"

                    in 400..499 ->
                        "Error de solicitud (HTTP ${error.codigo})"

                    else ->
                        "Error HTTP ${error.codigo}"
                }
            }

            is InterruptedIOException ->

                "Tiempo de espera agotado"

            is IOException ->

                "No se pudo conectar con el servidor"

            is SQLiteException ->

                "Error de almacenamiento local"

            is JsonParseException ->

                "Respuesta JSON inválida"

            is IllegalArgumentException ->

                "El servidor devolvió datos inválidos"

            else ->

                "No se pudo completar la sincronización"
        }
    }
}


// ==========================================
// CONVERTIR DTO REMOTO EN ENTITY
// ==========================================

private fun RecordatorioDto.aEntidadRemota(

    idLocal: Long = 0L

): RecordatorioEntity {

    val clave = requireNotNull(
        claveSincronizacion
    )

    // Validar el UUID
    UUID.fromString(clave)

    val tituloValido = requireNotNull(titulo)

    require(tituloValido.trim().length >= 4)

    require((descripcion?.length ?: 0) <= 200)

    val categoriaValida = requireNotNull(categoria)

    require(
        Categoria(categoriaValida) in
                DatosSimulados.categorias
    )

    val prioridadValida = requireNotNull(prioridad)

    require(
        prioridadValida in
                listOf("ALTA", "MEDIA", "BAJA")
    )

    val estadoValido = requireNotNull(estado)

    require(
        estadoValido in
                listOf("PENDIENTE", "COMPLETADO")
    )

    val creacion = requireNotNull(fechaCreacion)

    val modificacion = requireNotNull(
        fechaModificacion
    )

    val version = requireNotNull(versionLocal)

    require(creacion > 0)

    require(modificacion > 0)

    require(version > 0)


    return RecordatorioEntity(

        id = idLocal,

        titulo = tituloValido,

        descripcion = descripcion,

        categoria = categoriaValida,

        prioridad = prioridadValida,

        estado = estadoValido,

        fechaCreacion = creacion,

        fechaModificacion = modificacion,

        ubicacion = ubicacion,

        aviso = aviso,

        claveSincronizacion = clave,

        estadoSincronizacion = "SINCRONIZADO",

        versionLocal = version,

        eliminado = requireNotNull(eliminado)
    )
}
