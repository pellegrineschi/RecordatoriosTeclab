


package com.example.recordatoriosteclab

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

// ==========================================
// DAO DE RECORDATORIOS
// ==========================================

@Dao
interface RecordatorioDao {

    // ======================================
    // CONSULTAR TODOS LOS RECORDATORIOS
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE eliminado = 0
        ORDER BY fechaCreacion DESC
    """)
    fun observarTodos(): Flow<List<RecordatorioEntity>>


    // ======================================
    // CONSULTAR POR IDENTIFICADOR
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE id = :id
        AND eliminado = 0
        LIMIT 1
    """)
    fun observarPorId(
        id: Long
    ): Flow<RecordatorioEntity?>


    // ======================================
    // FILTRAR POR ESTADO
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE estado = :estado
        AND eliminado = 0
        ORDER BY fechaCreacion DESC
    """)
    fun observarPorEstado(
        estado: String
    ): Flow<List<RecordatorioEntity>>


    // ======================================
    // FILTRAR POR PRIORIDAD
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE prioridad = :prioridad
        AND eliminado = 0
        ORDER BY fechaCreacion DESC
    """)
    fun observarPorPrioridad(
        prioridad: String
    ): Flow<List<RecordatorioEntity>>


    // ======================================
    // INSERTAR UN RECORDATORIO
    // ======================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertar(
        recordatorio: RecordatorioEntity
    ): Long


    // ======================================
    // ACTUALIZAR UN RECORDATORIO
    // ======================================

    @Update
    suspend fun actualizar(
        recordatorio: RecordatorioEntity
    ): Int


    // ======================================
    // ELIMINAR DEFINITIVAMENTE
    // ======================================

    @Delete
    suspend fun eliminar(
        recordatorio: RecordatorioEntity
    ): Int


    // ======================================
    // OBTENER TODOS UNA SOLA VEZ
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE eliminado = 0
        ORDER BY fechaCreacion DESC
    """)
    suspend fun obtenerTodos():
            List<RecordatorioEntity>


    // ======================================
    // OBTENER POR ID UNA SOLA VEZ
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE id = :id
        AND eliminado = 0
        LIMIT 1
    """)
    suspend fun obtenerPorId(
        id: Long
    ): RecordatorioEntity?


    // ======================================
    // BUSCAR POR CLAVE DE SINCRONIZACIÓN
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE claveSincronizacion = :clave
        LIMIT 1
    """)
    suspend fun obtenerPorClave(
        clave: String
    ): RecordatorioEntity?


    // ======================================
    // OBTENER OPERACIONES PENDIENTES
    // ======================================

    @Query("""
        SELECT * FROM recordatorios
        WHERE estadoSincronizacion
        IN ('PENDIENTE', 'ERROR')
        ORDER BY fechaModificacion ASC
    """)
    suspend fun obtenerPendientes():
            List<RecordatorioEntity>


    // ======================================
    // ELIMINACIÓN LÓGICA
    // ======================================

    @Query("""
        UPDATE recordatorios    
        SET eliminado = 1,
            estadoSincronizacion = 'PENDIENTE',
            fechaModificacion = :fecha,
            versionLocal = versionLocal + 1
        WHERE id = :id
        AND eliminado = 0
    """)
    suspend fun marcarComoEliminado(
        id: Long,
        fecha: Long
    ): Int
}
