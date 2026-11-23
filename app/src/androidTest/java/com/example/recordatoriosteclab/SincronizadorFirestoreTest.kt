package com.example.recordatoriosteclab

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Room real y remoto determinista en memoria: nunca contacta Firebase. */
class SincronizadorFirestoreTest {
    private class Remoto : RemotoRecordatorios {
        val documentos = mutableMapOf<String, RecordatorioEntity>()
        var offline = false
        var perderConfirmacion = false
        var duranteEnvio: (suspend () -> Unit)? = null
        override suspend fun enviar(entidad: RecordatorioEntity) {
            check(!offline)
            validarEnvio(entidad, documentos[entidad.claveSincronizacion])
            documentos[entidad.claveSincronizacion] = entidad
            duranteEnvio?.invoke()
            if (perderConfirmacion) { perderConfirmacion = false; error("ACK perdido") }
        }
        override suspend fun descargar(): List<RecordatorioEntity> {
            check(!offline)
            return documentos.values.toList()
        }
    }
    private fun caso(test: suspend (RecordatoriosDatabase, Remoto) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            RecordatoriosDatabase::class.java).build()
        try { test(db, Remoto()) } finally { db.close() }
    }
    private fun entidad() = RecordatorioEntity(titulo = "Prueba local", categoria = DatosSimulados.categorias.first().nombre,
        prioridad = "ALTA", estado = "PENDIENTE")

    @Test fun offlineReintentoSinDuplicadosYEliminacion() = caso { db, remoto ->
        val dao = db.recordatorioDao()
        val e = entidad()
        val id = dao.insertar(e)
        val sync = SincronizadorFirestore(db, remoto)
        remoto.offline = true
        assertTrue(sync.sincronizar() is ResultadoSincronizacion.Error)
        assertEquals(1, dao.obtenerPendientes().size)
        remoto.offline = false
        remoto.perderConfirmacion = true
        assertTrue(sync.sincronizar() is ResultadoSincronizacion.Error)
        assertTrue(sync.sincronizar() is ResultadoSincronizacion.Exito)
        assertEquals(1, remoto.documentos.size)
        assertTrue(dao.obtenerPendientes().isEmpty())
        dao.marcarComoEliminado(id, System.currentTimeMillis())
        sync.sincronizar()
        assertTrue(remoto.documentos.getValue(e.claveSincronizacion).eliminado)
        assertTrue(dao.obtenerTodos().isEmpty())
        assertEquals(1, remoto.documentos.size)
    }

    @Test fun edicionDuranteEnvioNoSeMarcaConfirmada() = caso { db, remoto ->
        val dao = db.recordatorioDao()
        val e = entidad()
        val id = dao.insertar(e)
        remoto.duranteEnvio = { dao.actualizar(e.copy(id = id, titulo = "Edición concurrente", versionLocal = 2)) }
        SincronizadorFirestore(db, remoto).sincronizar()
        assertEquals("Edición concurrente", dao.obtenerPorId(id)?.titulo)
        assertEquals(1, dao.obtenerPendientes().size)
        remoto.duranteEnvio = null
        SincronizadorFirestore(db, remoto).sincronizar()
        assertEquals(2L, remoto.documentos.getValue(e.claveSincronizacion).versionLocal)
    }

    @Test fun conflictoConservaPendienteYDescargaOtros() = caso { db, remoto ->
        val dao = db.recordatorioDao()
        val e = entidad()
        val id = dao.insertar(e)
        remoto.documentos[e.claveSincronizacion] = e.copy(titulo = "Remoto nuevo", versionLocal = 3)
        val otro = entidad()
        remoto.documentos[otro.claveSincronizacion] = otro.copy(eliminado = true)
        assertTrue(SincronizadorFirestore(db, remoto).sincronizar() is ResultadoSincronizacion.Error)
        assertEquals(e.titulo, dao.obtenerPorId(id)?.titulo)
        assertEquals(1, dao.obtenerPendientes().size)
        assertTrue(dao.obtenerPorClave(otro.claveSincronizacion)!!.eliminado)
    }

    @Test fun descargaActualizaSinReinsertarNiRetrocederVersion() = caso { db, remoto ->
        val dao = db.recordatorioDao()
        val e = entidad()
        remoto.documentos[e.claveSincronizacion] = e
        val sync = SincronizadorFirestore(db, remoto)
        sync.sincronizar()
        val id = dao.obtenerTodos().single().id
        remoto.documentos[e.claveSincronizacion] = e.copy(titulo = "Modificación remota", versionLocal = 2)
        sync.sincronizar()
        assertEquals(id, dao.obtenerTodos().single().id)
        assertEquals(2L, dao.obtenerPorId(id)!!.versionLocal)
        remoto.documentos[e.claveSincronizacion] = e
        sync.sincronizar()
        assertEquals(2L, dao.obtenerPorId(id)!!.versionLocal)
    }

    @Test fun serializacionValidaIdentidad() {
        val e = entidad()
        assertEquals(e.toDto(), desdeMapa(e.claveSincronizacion, e.aMapa()).toDto())
        try { desdeMapa("otro", e.aMapa()); fail("Aceptó un ID diferente") }
        catch (_: IllegalArgumentException) { }
    }
}
