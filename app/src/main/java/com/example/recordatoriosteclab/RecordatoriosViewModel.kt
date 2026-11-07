
package com.example.recordatoriosteclab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==========================================
// ESTADOS DE LA PANTALLA DE LISTADO
// ==========================================

sealed interface ListadoUiState {

    // La aplicación está cargando los datos
    data object Cargando : ListadoUiState

    // No existen recordatorios
    data object Vacio : ListadoUiState

    // Tenemos recordatorios para mostrar
    data class ConDatos(
        val recordatorios: List<Recordatorio>
    ) : ListadoUiState

    // Ocurrió un error
    data class Error(
        val mensaje: String
    ) : ListadoUiState
}


// ==========================================
// VIEWMODEL PRINCIPAL
// ==========================================

class RecordatoriosViewModel(
    private val repositorio: RecordatoriosRepository
) : ViewModel() {

    // --------------------------------------
    // ESTADO DEL LISTADO
    // --------------------------------------

    // Estado interno modificable
    private val _uiState =
        MutableStateFlow<ListadoUiState>(
            ListadoUiState.Cargando
        )

    // Estado público de solo lectura
    val uiState: StateFlow<ListadoUiState> =
        _uiState.asStateFlow()


    // --------------------------------------
    // ESTADO DEL FORMULARIO
    // --------------------------------------

    // Estado interno del formulario
    private val _formularioState =
        MutableStateFlow(FormularioUiState())

    // Estado público del formulario
    val formularioState: StateFlow<FormularioUiState> =
        _formularioState.asStateFlow()


    // --------------------------------------
    // CONTROL DE OPERACIONES
    // --------------------------------------

    // Guarda la referencia a la carga actual
    private var trabajoDeCarga: Job? = null


    // --------------------------------------
    // INICIALIZACIÓN
    // --------------------------------------

    init {
        // Cargamos los recordatorios al iniciar
        cargarRecordatorios()
    }


    // ======================================
    // CARGAR RECORDATORIOS
    // ======================================

    fun cargarRecordatorios() {

        // Cancelamos una carga anterior
        trabajoDeCarga?.cancel()

        // Iniciamos una nueva operación
        trabajoDeCarga = viewModelScope.launch {

            // Mostramos el estado de carga
            _uiState.value = ListadoUiState.Cargando

            try {

                // Simulamos una espera de 500 ms
                delay(500)

                // Consultamos el repositorio
                val lista = repositorio.obtenerTodos()

                // Evaluamos si hay recordatorios
                _uiState.value = if (lista.isEmpty()) {

                    ListadoUiState.Vacio

                } else {

                    ListadoUiState.ConDatos(lista)

                }

            } catch (e: CancellationException) {

                // Respetamos la cancelación
                throw e

            } catch (e: Exception) {

                // Informamos el error
                _uiState.value = ListadoUiState.Error(
                    "No se pudieron cargar los recordatorios"
                )
            }
        }
    }


    // ======================================
    // SIMULACIONES PARA LA ACTIVIDAD 2
    // ======================================

    // Simula que no existen recordatorios
    fun simularEstadoVacio() {

        // Evitamos que una carga anterior
        // cambie el estado simulado
        trabajoDeCarga?.cancel()

        _uiState.value = ListadoUiState.Vacio
    }


    // Simula un error de carga
    fun simularEstadoError() {

        // Cancelamos cualquier carga pendiente
        trabajoDeCarga?.cancel()

        _uiState.value = ListadoUiState.Error(
            "Error de conexión simulado. Intentá nuevamente."
        )
    }


    // ======================================
    // FUNCIONES DEL FORMULARIO
    // ======================================

    // Actualizar el título
    fun cambiarTitulo(nuevoTitulo: String) {

        _formularioState.update {
            it.copy(
                titulo = nuevoTitulo,
                errorGeneral = null
            )
        }
    }


    // Actualizar la descripción
    fun cambiarDescripcion(nuevaDescripcion: String) {

        _formularioState.update {
            it.copy(
                descripcion = nuevaDescripcion,
                errorGeneral = null
            )
        }
    }


    // Seleccionar una categoría
    fun seleccionarCategoria(categoria: Categoria) {

        _formularioState.update {
            it.copy(
                categoria = categoria,
                errorGeneral = null
            )
        }
    }


    // Seleccionar una prioridad
    fun seleccionarPrioridad(prioridad: Prioridad) {

        _formularioState.update {
            it.copy(
                prioridad = prioridad,
                errorGeneral = null
            )
        }
    }


    // ======================================
    // GUARDAR UN NUEVO RECORDATORIO
    // ======================================

    fun guardarRecordatorio() {

        // Obtenemos los datos del formulario
        val datos = _formularioState.value

        // Si el formulario no es válido,
        // no permitimos guardar
        if (!datos.puedeGuardar) return

        // Informamos que empezó el guardado
        _formularioState.update {
            it.copy(
                guardando = true,
                guardadoExitoso = false,
                errorGeneral = null
            )
        }

        // Ejecutamos la operación asincrónica
        viewModelScope.launch {

            try {

                // Simulamos el tiempo de guardado
                delay(500)

                // Consultamos las tareas existentes
                val existentes = repositorio.obtenerTodos()

                // Generamos un ID nuevo
                val nuevoId =
                    (existentes.maxOfOrNull { it.id } ?: 0L) + 1L

                // Creamos el nuevo recordatorio
                val nuevo = Recordatorio(
                    id = nuevoId,

                    titulo = datos.titulo.trim(),

                    descripcion = datos.descripcion
                        .trim()
                        .ifBlank { null },

                    categoria = requireNotNull(
                        datos.categoria
                    ),

                    prioridad = requireNotNull(
                        datos.prioridad
                    ),

                    estado = EstadoRecordatorio.Pendiente
                )

                // Guardamos en el repositorio
                repositorio.guardar(nuevo)

                // Informamos que el guardado fue exitoso
                _formularioState.update {
                    it.copy(
                        guardando = false,
                        guardadoExitoso = true,
                        errorGeneral = null
                    )
                }

                // Actualizamos la lista
                cargarRecordatorios()

            } catch (e: CancellationException) {

                // No ocultamos una cancelación
                throw e

            } catch (e: Exception) {

                // Informamos que no se pudo guardar
                _formularioState.update {
                    it.copy(
                        guardando = false,
                        guardadoExitoso = false,
                        errorGeneral =
                            "No se pudo guardar el recordatorio"
                    )
                }
            }
        }
    }


    // ======================================
    // LIMPIAR FORMULARIO
    // ======================================

    fun limpiarFormulario() {

        // Restauramos el estado inicial
        _formularioState.value = FormularioUiState()
    }
}
