

package com.example.recordatoriosteclab

// Datos intercambiados con el servidor
data class RecordatorioDto(

    val claveSincronizacion: String?,

    val titulo: String?,

    val descripcion: String?,

    val categoria: String?,

    val prioridad: String?,

    val estado: String?,

    val fechaCreacion: Long?,

    val fechaModificacion: Long?,

    val ubicacion: String?,

    val aviso: String?,

    val versionLocal: Long?,

    val eliminado: Boolean?
)

// Respuesta del endpoint /salud
data class SaludDto(

    val estado: String?,

    val modo: String?
)

// Convertir una entidad Room en un DTO
fun RecordatorioEntity.toDto(): RecordatorioDto {

    return RecordatorioDto(

        claveSincronizacion = claveSincronizacion,

        titulo = titulo,

        descripcion = descripcion,

        categoria = categoria,

        prioridad = prioridad,

        estado = estado,

        fechaCreacion = fechaCreacion,

        fechaModificacion = fechaModificacion,

        ubicacion = ubicacion,

        aviso = aviso,

        versionLocal = versionLocal,

        eliminado = eliminado
    )
}
