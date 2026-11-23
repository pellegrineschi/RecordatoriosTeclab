
package com.example.recordatoriosteclab

// ==========================================
// CATEGORÍA
// ==========================================

data class Categoria(
    val nombre: String
)


// ==========================================
// PRIORIDADES
// ==========================================

sealed class Prioridad {

    data object Alta : Prioridad()

    data object Media : Prioridad()

    data object Baja : Prioridad()
}


// ==========================================
// ESTADO DEL RECORDATORIO
// ==========================================

sealed class EstadoRecordatorio {

    data object Pendiente : EstadoRecordatorio()

    data object Completado : EstadoRecordatorio()
}


// ==========================================
// ESTADO DE SINCRONIZACIÓN
// ==========================================

enum class EstadoSincronizacion {

    PENDIENTE,

    SINCRONIZADO,

    ERROR
}


// ==========================================
// MODELO DE DOMINIO
// ==========================================

data class Recordatorio(

    val id: Long,

    val titulo: String,

    val descripcion: String? = null,

    val categoria: Categoria,

    val prioridad: Prioridad,

    val estado: EstadoRecordatorio,

    val ubicacion: String? = null,

    val aviso: String? = null,

    val fechaCreacion: Long = 0L,

    val fechaModificacion: Long = fechaCreacion,

    val estadoSincronizacion: EstadoSincronizacion =
        EstadoSincronizacion.PENDIENTE

) {

    init {

        require(id > 0) {
            "El ID debe ser positivo"
        }

        require(titulo.isNotBlank()) {
            "El título no puede estar vacío"
        }
    }
}
