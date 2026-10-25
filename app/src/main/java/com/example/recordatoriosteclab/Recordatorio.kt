package com.example.recordatoriosteclab



// Representa la categoría de una tarea
data class Categoria(
    val nombre: String
)

// Define las prioridades permitidas
sealed class Prioridad {
    data object Alta : Prioridad()
    data object Media : Prioridad()
    data object Baja : Prioridad()
}

// Define los estados de un recordatorio
sealed class EstadoRecordatorio {
    data object Pendiente : EstadoRecordatorio()
    data object Completado : EstadoRecordatorio()
}

// Representa un recordatorio completo
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio,
    val ubicacion: String? = null,
    val aviso: String? = null
) {
    // Impide crear tareas sin título o con ID inválido
    init {
        require(id > 0) {
            "El ID debe ser positivo"
        }
        require(titulo.isNotBlank()) {
            "El título no puede estar vacío"
        }
    }
}
