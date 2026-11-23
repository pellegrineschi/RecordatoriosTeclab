
package com.example.recordatoriosteclab

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ==========================================
// REPOSITORIO DE RECORDATORIOS CON ROOM
// ==========================================

class RecordatoriosRepositoryRoom(
    private val baseDeDatos: RecordatoriosDatabase,
    api: RecordatoriosApi? = null
) {

    private val dao = baseDeDatos.recordatorioDao()

    private val sincronizador =
        api?.let { SincronizadorRecordatorios(baseDeDatos, it) }


    // ======================================
    // SINCRONIZAR CON EL SERVIDOR
    // ======================================

    suspend fun sincronizar(): ResultadoSincronizacion {

        return sincronizador?.sincronizar() ?: ResultadoSincronizacion.Error(
            "Sincronización HTTP pendiente: el servidor de Actividad 3 no aísla cuentas. Los cambios se guardan en este dispositivo."
        )
    }


    // ======================================
    // OBSERVAR TODOS LOS RECORDATORIOS
    // ======================================

    fun observarTodos(): Flow<List<Recordatorio>> {

        return dao.observarTodos().map { entidades ->

            entidades.map { entidad ->

                entidad.toRecordatorio()
            }
        }
    }


    // ======================================
    // OBSERVAR UN RECORDATORIO POR ID
    // ======================================

    fun observarPorId(
        id: Long
    ): Flow<Recordatorio?> {

        return dao.observarPorId(id).map { entidad ->

            entidad?.toRecordatorio()
        }
    }


    // ======================================
    // FILTRAR POR ESTADO
    // ======================================

    fun observarPorEstado(
        estado: EstadoRecordatorio
    ): Flow<List<Recordatorio>> {

        return dao.observarPorEstado(
            estado.aValorRoom()
        ).map { entidades ->

            entidades.map {

                it.toRecordatorio()
            }
        }
    }


    // ======================================
    // FILTRAR POR PRIORIDAD
    // ======================================

    fun observarPorPrioridad(
        prioridad: Prioridad
    ): Flow<List<Recordatorio>> {

        return dao.observarPorPrioridad(
            prioridad.aValorRoom()
        ).map { entidades ->

            entidades.map {

                it.toRecordatorio()
            }
        }
    }


    // ======================================
    // OBTENER TODOS UNA SOLA VEZ
    // ======================================

    suspend fun obtenerTodos(): List<Recordatorio> {

        return dao.obtenerTodos().map {

            it.toRecordatorio()
        }
    }


    // ======================================
    // OBTENER POR IDENTIFICADOR
    // ======================================

    suspend fun obtenerPorId(
        id: Long
    ): Recordatorio? {

        return dao.obtenerPorId(id)
            ?.toRecordatorio()
    }


    // ======================================
    // CREAR UN RECORDATORIO
    // ======================================

    suspend fun crear(
        titulo: String,
        descripcion: String?,
        categoria: Categoria,
        prioridad: Prioridad
    ): Long {

        require(titulo.trim().length >= 4) {

            "El título debe tener al menos 4 caracteres"
        }

        require((descripcion?.length ?: 0) <= 200) {

            "La descripción no puede superar los 200 caracteres"
        }

        val nuevaEntidad = RecordatorioEntity(

            titulo = titulo.trim(),

            descripcion = descripcion
                ?.trim()
                ?.ifBlank { null },

            categoria = categoria.nombre,

            prioridad = prioridad.aValorRoom(),

            estado = "PENDIENTE",

            estadoSincronizacion = "PENDIENTE"
        )

        return dao.insertar(nuevaEntidad)
    }


    // ======================================
    // ACTUALIZAR UN RECORDATORIO
    // ======================================

    suspend fun actualizar(
        recordatorio: Recordatorio
    ) {

        baseDeDatos.withTransaction {

            val existente = checkNotNull(
                dao.obtenerPorId(recordatorio.id)
            ) {

                "El recordatorio no existe"
            }

            val actualizado = existente.actualizarDesde(
                recordatorio
            )

            check(dao.actualizar(actualizado) == 1) {

                "No se pudo actualizar el recordatorio"
            }
        }
    }


    // ======================================
    // ELIMINAR UN RECORDATORIO
    // ======================================

    suspend fun eliminar(
        id: Long
    ) {

        val filasAfectadas = dao.marcarComoEliminado(

            id = id,

            fecha = System.currentTimeMillis()
        )

        check(filasAfectadas == 1) {

            "No se pudo eliminar el recordatorio"
        }
    }
}
