package com.example.recordatoriosteclab




import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// ==========================================
// INSTANCIA ÚNICA DE DATASTORE
// ==========================================

private val Context.dataStore by preferencesDataStore(
    name = "preferencias_recordatorios"
)


// ==========================================
// OPCIONES DE ORDENAMIENTO
// ==========================================

enum class OrdenListado {
    RECIENTES,
    TITULO
}


// ==========================================
// MODELO DE PREFERENCIAS
// ==========================================

data class PreferenciasUsuario(

    val temaOscuro: Boolean = false,

    val mostrarCompletados: Boolean = true,

    val ordenListado: OrdenListado =
        OrdenListado.RECIENTES
)


// ==========================================
// REPOSITORIO DE PREFERENCIAS
// ==========================================

class PreferenciasRepository(context: Context) {

    private val dataStore =
        context.applicationContext.dataStore


    // --------------------------------------
    // CLAVES DE ALMACENAMIENTO
    // --------------------------------------

    private object Claves {

        val TEMA_OSCURO =
            booleanPreferencesKey("tema_oscuro")

        val MOSTRAR_COMPLETADOS =
            booleanPreferencesKey("mostrar_completados")

        val ORDEN_LISTADO =
            stringPreferencesKey("orden_listado")
    }


    // ======================================
    // LEER LAS PREFERENCIAS
    // ======================================

    val preferencias: Flow<PreferenciasUsuario> =
        dataStore.data

            .catch { error ->

                // Si falla la lectura del archivo,
                // utilizamos los valores predeterminados
                if (error is IOException) {

                    emit(emptyPreferences())

                } else {

                    throw error
                }
            }

            .map { datos ->

                // Recuperamos el orden guardado
                val ordenGuardado =
                    datos[Claves.ORDEN_LISTADO]

                // Si el valor es desconocido,
                // utilizamos el orden predeterminado
                val orden = when (ordenGuardado) {

                    OrdenListado.TITULO.name ->
                        OrdenListado.TITULO

                    OrdenListado.RECIENTES.name ->
                        OrdenListado.RECIENTES

                    else ->
                        OrdenListado.RECIENTES
                }

                PreferenciasUsuario(

                    temaOscuro =
                        datos[Claves.TEMA_OSCURO]
                            ?: false,

                    mostrarCompletados =
                        datos[Claves.MOSTRAR_COMPLETADOS]
                            ?: true,

                    ordenListado = orden
                )
            }


    // ======================================
    // GUARDAR EL TEMA VISUAL
    // ======================================

    suspend fun cambiarTemaOscuro(
        activado: Boolean
    ) {

        dataStore.edit { datos ->

            datos[Claves.TEMA_OSCURO] = activado
        }
    }


    // ======================================
    // MOSTRAR U OCULTAR COMPLETADOS
    // ======================================

    suspend fun cambiarMostrarCompletados(
        mostrar: Boolean
    ) {

        dataStore.edit { datos ->

            datos[Claves.MOSTRAR_COMPLETADOS] = mostrar
        }
    }


    // ======================================
    // CAMBIAR EL ORDEN DEL LISTADO
    // ======================================

    suspend fun cambiarOrdenListado(
        orden: OrdenListado
    ) {

        dataStore.edit { datos ->

            datos[Claves.ORDEN_LISTADO] = orden.name
        }
    }
}
