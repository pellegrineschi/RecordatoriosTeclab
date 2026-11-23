

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

        @Volatile
        private var INSTANCIA: RecordatoriosDatabase? = null


        fun obtenerBaseDeDatos(
            context: Context
        ): RecordatoriosDatabase {

            return INSTANCIA ?: synchronized(this) {

                INSTANCIA ?: Room.databaseBuilder(

                    context.applicationContext,

                    RecordatoriosDatabase::class.java,

                    "recordatorios.db"

                )
                    .build()
                    .also { baseDeDatos ->

                        INSTANCIA = baseDeDatos
                    }
            }
        }
    }
}
