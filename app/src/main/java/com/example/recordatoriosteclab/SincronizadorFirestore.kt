package com.example.recordatoriosteclab

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout

/** Room conserva la cola incluso si el proceso termina o no hay Internet. */
class SincronizadorFirestore(
    private val db: RecordatoriosDatabase,
    private val remoto: RemotoRecordatorios
) {
    private val dao = db.recordatorioDao()
    private val mutex = Mutex()

    suspend fun sincronizar(): ResultadoSincronizacion {
        if (!mutex.tryLock()) return ResultadoSincronizacion.EnCurso
        try {
            var enviados = 0
            var error: String? = null
            for (pendiente in dao.obtenerPendientes()) {
                try {
                    withTimeout(20_000) { remoto.enviar(pendiente) }
                    db.withTransaction {
                        val actual = dao.obtenerPorClave(pendiente.claveSincronizacion)
                        // Un ACK tardío no confirma una edición realizada durante el envío.
                        if (actual?.toDto() == pendiente.toDto()) {
                            dao.actualizar(actual!!.copy(estadoSincronizacion = "SINCRONIZADO"))
                        }
                    }
                    enviados++
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    error = if (e is ConflictoRemoto) e.message else
                        "No se pudo enviar a Firestore. Los cambios siguen en Room; se reintentará."
                    db.withTransaction {
                        val actual = dao.obtenerPorClave(pendiente.claveSincronizacion)
                        if (actual?.toDto() == pendiente.toDto())
                            dao.actualizar(actual!!.copy(estadoSincronizacion = "ERROR"))
                    }
                }
            }
            val descargados = withTimeout(20_000) { remoto.descargar() }
            db.withTransaction {
                for (remoto in descargados) {
                    val local = dao.obtenerPorClave(remoto.claveSincronizacion)
                    if (local == null) dao.insertar(remoto.copy(id = 0, estadoSincronizacion = "SINCRONIZADO"))
                    else if (local.estadoSincronizacion == "SINCRONIZADO" && remoto.versionLocal > local.versionLocal)
                        dao.actualizar(remoto.copy(id = local.id, estadoSincronizacion = "SINCRONIZADO"))
                }
            }
            return error?.let { ResultadoSincronizacion.Error(it) } ?: ResultadoSincronizacion.Exito(enviados)
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            return ResultadoSincronizacion.Error("No se pudo sincronizar con Firestore. Revisá conexión, sesión y reglas. Los datos locales se conservan.")
        } finally { mutex.unlock() }
    }
}
