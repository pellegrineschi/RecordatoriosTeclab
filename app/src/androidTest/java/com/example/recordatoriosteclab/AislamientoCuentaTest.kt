package com.example.recordatoriosteclab

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AislamientoCuentaTest {
    @Test fun roomYPreferenciasNoSeCompartenEntreCuentas() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val a = "test-${UUID.randomUUID()}"
        val b = "test-${UUID.randomUUID()}"
        val dbA = RecordatoriosDatabase.obtenerBaseDeDatos(context, a)
        val dbB = RecordatoriosDatabase.obtenerBaseDeDatos(context, b)
        try {
            val repoA = RecordatoriosRepositoryRoom(dbA)
            val repoB = RecordatoriosRepositoryRoom(dbB)
            val id = repoA.crear("Tarea privada", null, DatosSimulados.categorias.first(), Prioridad.Alta)
            assertEquals(1, repoA.obtenerTodos().size)
            assertTrue(repoB.obtenerTodos().isEmpty())
            assertNull(repoB.obtenerPorId(id))
            repoA.actualizar(requireNotNull(repoA.obtenerPorId(id)).copy(titulo = "Tarea editada"))
            assertEquals("Tarea editada", repoA.obtenerPorId(id)?.titulo)
            assertTrue(repoB.obtenerTodos().isEmpty())
            val preferenciasA = PreferenciasRepository(context, a)
            val preferenciasB = PreferenciasRepository(context, b)
            preferenciasA.cambiarTemaOscuro(true)
            assertTrue(preferenciasA.preferencias.first().temaOscuro)
            assertFalse(preferenciasB.preferencias.first().temaOscuro)
            assertEquals(1, RecordatoriosRepositoryRoom(
                RecordatoriosDatabase.obtenerBaseDeDatos(context, a)
            ).obtenerTodos().size)
            assertTrue(repoA.sincronizar() is ResultadoSincronizacion.Error)
            repoA.eliminar(id)
            assertTrue(repoA.obtenerTodos().isEmpty())
        } finally {
            dbA.close()
            dbB.close()
            context.deleteDatabase("recordatorios_${claveCuenta(a)}.db")
            context.deleteDatabase("recordatorios_${claveCuenta(b)}.db")
        }
    }
}
