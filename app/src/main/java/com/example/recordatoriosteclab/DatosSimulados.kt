package com.example.recordatoriosteclab


// Objeto que contiene los datos de ejemplo
object DatosSimulados {

    // Creamos las categorías disponibles
    private val estudio = Categoria("Estudio")
    private val trabajo = Categoria("Trabajo")
    private val compras = Categoria("Compras")

    // LIST: contiene nuestros recordatorios
    val recordatorios: List<Recordatorio> = listOf(

        Recordatorio(
            id = 1L,
            titulo = "Estudiar Kotlin",
            descripcion = "Repasar el módulo 1",
            categoria = estudio,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Pendiente
        ),

        Recordatorio(
            id = 2L,
            titulo = "Comprar mercadería",
            categoria = compras,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Pendiente
        ),

        Recordatorio(
            id = 3L,
            titulo = "Enviar informe",
            categoria = trabajo,
            prioridad = Prioridad.Alta,
            estado = EstadoRecordatorio.Completado
        ),

        Recordatorio(
            id = 4L,
            titulo = "Pagar factura",
            categoria = compras,
            prioridad = Prioridad.Baja,
            estado = EstadoRecordatorio.Pendiente
        )
    )

    // SET: obtiene categorías sin repetir
    val categorias: Set<Categoria> =
        recordatorios.map { it.categoria }.toSet()

    // MAP: asocia categorías con etiquetas
    val etiquetasPorCategoria: Map<Categoria, String> = mapOf(
        estudio to "Estudio",
        trabajo to "Trabajo",
        compras to "Compras"
    )

    // FILTER: obtiene los recordatorios pendientes
    val pendientes: List<Recordatorio> =
        recordatorios.filter {
            it.estado == EstadoRecordatorio.Pendiente
        }

    // FILTER: obtiene los recordatorios de prioridad alta
    val altaPrioridad: List<Recordatorio> =
        recordatorios.filter {
            it.prioridad == Prioridad.Alta
        }
}
