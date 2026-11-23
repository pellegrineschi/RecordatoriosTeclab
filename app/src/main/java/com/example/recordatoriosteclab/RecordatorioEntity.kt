
package com.example.recordatoriosteclab

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// ==========================================
// ENTIDAD DE LA BASE DE DATOS
// ==========================================

@Entity(
    tableName = "recordatorios",

    indices = [
        Index(
            value = ["claveSincronizacion"],
            unique = true
        )
    ]
)

data class RecordatorioEntity(

    // Identificador local autogenerado
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Datos principales
    val titulo: String,

    val descripcion: String? = null,

    val categoria: String,

    val prioridad: String,

    val estado: String,


    // ======================================
    // FECHAS
    // ======================================

    // Fecha de creación en milisegundos
    val fechaCreacion: Long =
        System.currentTimeMillis(),

    // Fecha de la última modificación
    val fechaModificacion: Long =
        fechaCreacion,


    // ======================================
    // CAMPOS OPCIONALES
    // ======================================

    val ubicacion: String? = null,

    val aviso: String? = null,


    // ======================================
    // SINCRONIZACIÓN
    // ======================================

    // Identificador único para comunicarnos
    // con el servidor sin generar duplicados
    val claveSincronizacion: String =
        UUID.randomUUID().toString(),

    // PENDIENTE, SINCRONIZADO o ERROR
    val estadoSincronizacion: String =
        "PENDIENTE",

    // Versión para identificar modificaciones
    val versionLocal: Long = 1L,

    // Permite sincronizar eliminaciones
    val eliminado: Boolean = false
)
