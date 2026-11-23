
package com.example.recordatoriosteclab

// ==========================================
// CONVERTIR PRIORIDAD A TEXTO
// ==========================================

fun Prioridad.aValorRoom(): String {

    return when (this) {

        Prioridad.Alta -> "ALTA"

        Prioridad.Media -> "MEDIA"

        Prioridad.Baja -> "BAJA"
    }
}


// ==========================================
// CONVERTIR ESTADO A TEXTO
// ==========================================

fun EstadoRecordatorio.aValorRoom(): String {

    return when (this) {

        EstadoRecordatorio.Pendiente ->
            "PENDIENTE"

        EstadoRecordatorio.Completado ->
            "COMPLETADO"
    }
}


// ==========================================
// CONVERTIR ENTITY A MODELO DE DOMINIO
// ==========================================

fun RecordatorioEntity.toRecordatorio(): Recordatorio {

    return Recordatorio(

        id = id,

        titulo = titulo,

        descripcion = descripcion,

        categoria = Categoria(categoria),

        prioridad = when (prioridad) {

            "ALTA" -> Prioridad.Alta

            "MEDIA" -> Prioridad.Media

            "BAJA" -> Prioridad.Baja

            else -> error(
                "Prioridad desconocida: $prioridad"
            )
        },

        estado = when (estado) {

            "PENDIENTE" ->
                EstadoRecordatorio.Pendiente

            "COMPLETADO" ->
                EstadoRecordatorio.Completado

            else -> error(
                "Estado desconocido: $estado"
            )
        },

        ubicacion = ubicacion,

        aviso = aviso,

        fechaCreacion = fechaCreacion,

        fechaModificacion = fechaModificacion,

        estadoSincronizacion =
            EstadoSincronizacion.valueOf(
                estadoSincronizacion
            )
    )
}


// ==========================================
// CONVERTIR UN RECORDATORIO NUEVO A ENTITY
// ==========================================

fun Recordatorio.toNuevaEntity(): RecordatorioEntity {

    val ahora = System.currentTimeMillis()

    return RecordatorioEntity(

        // Room generará el ID automáticamente
        id = 0L,

        titulo = titulo.trim(),

        descripcion = descripcion,

        categoria = categoria.nombre,

        prioridad = prioridad.aValorRoom(),

        estado = estado.aValorRoom(),

        fechaCreacion = ahora,

        fechaModificacion = ahora,

        ubicacion = ubicacion,

        aviso = aviso,

        estadoSincronizacion = "PENDIENTE",

        versionLocal = 1L,

        eliminado = false
    )
}


// ==========================================
// ACTUALIZAR UNA ENTITY EXISTENTE
// ==========================================

fun RecordatorioEntity.actualizarDesde(
    recordatorio: Recordatorio
): RecordatorioEntity {

    require(id == recordatorio.id) {
        "Los identificadores deben coincidir"
    }

    return copy(

        titulo = recordatorio.titulo.trim(),

        descripcion = recordatorio.descripcion,

        categoria = recordatorio.categoria.nombre,

        prioridad =
            recordatorio.prioridad.aValorRoom(),

        estado =
            recordatorio.estado.aValorRoom(),

        ubicacion = recordatorio.ubicacion,

        aviso = recordatorio.aviso,

        fechaModificacion =
            System.currentTimeMillis(),

        estadoSincronizacion = "PENDIENTE",

        versionLocal = versionLocal + 1L
    )
}

