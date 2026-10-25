package com.example.recordatoriosteclab



import org.junit.Assert.assertEquals
import org.junit.Test

class RecordatorioTest {

    // Prueba 1: No permitir títulos vacíos
    @Test(expected = IllegalArgumentException::class)
    fun tituloVacioNoPermitido() {

        Recordatorio(
            id = 1L,
            titulo = "",
            categoria = Categoria("Estudio"),
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente
        )
    }

    // Prueba 2: No permitir ID inválidos
    @Test(expected = IllegalArgumentException::class)
    fun idInvalidoNoPermitido() {

        Recordatorio(
            id = -1L,
            titulo = "Estudiar Kotlin",
            categoria = Categoria("Estudio"),
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Pendiente
        )
    }

    // Prueba 3: Comprobar las colecciones y filtros
    @Test
    fun comprobarFiltros() {

        assertEquals(4, DatosSimulados.recordatorios.size)
        assertEquals(3, DatosSimulados.pendientes.size)
        assertEquals(2, DatosSimulados.altaPrioridad.size)
        assertEquals(3, DatosSimulados.categorias.size)
    }
}
