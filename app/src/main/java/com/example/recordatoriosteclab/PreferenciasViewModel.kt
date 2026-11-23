package com.example.recordatoriosteclab




import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ==========================================
// VIEWMODEL DE PREFERENCIAS
// ==========================================

class PreferenciasViewModel(
    private val repositorio: PreferenciasRepository
) : ViewModel() {

    // ======================================
    // ESTADO DE LAS PREFERENCIAS
    // ======================================

    val preferencias: StateFlow<PreferenciasUsuario> =
        repositorio.preferencias.stateIn(

            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(5000),

            initialValue = PreferenciasUsuario()
        )


    // ======================================
    // ESTADO DE ERROR
    // ======================================

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    // ======================================
    // CAMBIAR TEMA VISUAL
    // ======================================

    fun cambiarTemaOscuro(activado: Boolean) {

        guardarPreferencia {

            repositorio.cambiarTemaOscuro(activado)
        }
    }


    // ======================================
    // MOSTRAR U OCULTAR COMPLETADOS
    // ======================================

    fun cambiarMostrarCompletados(mostrar: Boolean) {

        guardarPreferencia {

            repositorio.cambiarMostrarCompletados(mostrar)
        }
    }


    // ======================================
    // CAMBIAR ORDEN DEL LISTADO
    // ======================================

    fun cambiarOrdenListado(orden: OrdenListado) {

        guardarPreferencia {

            repositorio.cambiarOrdenListado(orden)
        }
    }


    // ======================================
    // GUARDADO Y MANEJO DE ERRORES
    // ======================================

    private fun guardarPreferencia(
        operacion: suspend () -> Unit
    ) {

        viewModelScope.launch {

            try {

                _error.value = null

                operacion()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _error.value =
                    "No se pudo guardar la preferencia"
            }
        }
    }


    // ======================================
    // LIMPIAR MENSAJE DE ERROR
    // ======================================

    fun limpiarError() {

        _error.value = null
    }
}
