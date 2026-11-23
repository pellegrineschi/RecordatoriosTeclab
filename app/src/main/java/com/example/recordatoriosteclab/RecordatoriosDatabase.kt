

package com.example.recordatoriosteclab

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ==========================================
// BASE DE DATOS PRINCIPAL
// ==========================================

@Database(
    entities = [RecordatorioEntity::class],
    version = 1,
    exportSchema = true
)

abstract class RecordatoriosDatabase : RoomDatabase() {

    // Acceso al DAO de recordatorios
    abstract fun recordatorioDao(): RecordatorioDao


    // ======================================
    // INSTANCIA ÚNICA DE LA BASE DE DATOS
    // ======================================

    companion object {

        private val instancias = mutableMapOf<String, RecordatoriosDatabase>()

        @Synchronized
        fun obtenerBaseDeDatos(context: Context, uid: String): RecordatoriosDatabase {
            val nombre = "recordatorios_${claveCuenta(uid)}.db"
            // recordatorios.db (Actividad 3) se conserva sin atribuir su dueño.
            return instancias.getOrPut(nombre) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RecordatoriosDatabase::class.java,
                    nombre
                ).build()
            }
        }
    }
}
