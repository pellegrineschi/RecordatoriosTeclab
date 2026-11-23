
package com.example.recordatoriosteclab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==========================================
// ESTADOS DEL LISTADO
// ==========================================

sealed interface ListadoUiState {

    data object Cargando : ListadoUiState

    data object Vacio : ListadoUiState

    data class ConDatos(
        val recordatorios: List<Recordatorio>
    ) : ListadoUiState

    data class Error(
        val mensaje: String
    ) : ListadoUiState
}


// ==========================================
// ESTADO DE LAS OPERACIONES DE DETALLE
// ==========================================

data class DetalleOperacionUiState(

    val ocupado: Boolean = false,

    val error: String? = null,

    val eliminadoId: Long? = null
)


// ==========================================
// VIEWMODEL PRINCIPAL
// ==========================================

class RecordatoriosViewModel(
    private val repositorio: RecordatoriosRepositoryRoom
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ListadoUiState>(
            ListadoUiState.Cargando
        )

    val uiState: StateFlow<ListadoUiState> =
        _uiState.asStateFlow()


    private val _formularioState =
        MutableStateFlow(FormularioUiState())

    val formularioState: StateFlow<FormularioUiState> =
        _formularioState.asStateFlow()


    private val _detalleOperacion =
        MutableStateFlow(DetalleOperacionUiState())

    val detalleOperacion: StateFlow<DetalleOperacionUiState> =
        _detalleOperacion.asStateFlow()


    private var trabajoDeCarga: Job? = null


// ==========================================
// ESTADO DE SINCRONIZACIÓN
// ==========================================

    private val _sincronizacion =
        MutableStateFlow<ResultadoSincronizacion?>(null)

    val sincronizacion: StateFlow<ResultadoSincronizacion?> =
        _sincronizacion.asStateFlow()


// ==========================================
// SINCRONIZAR CON EL SERVIDOR
// ==========================================

    fun sincronizarRecordatorios() {

        // Evitar sincronizaciones simultáneas
        if (_sincronizacion.value ==
            ResultadoSincronizacion.EnCurso
        ) return

        _sincronizacion.value =
            ResultadoSincronizacion.EnCurso

        viewModelScope.launch {

            try {

                _sincronizacion.value =
                    repositorio.sincronizar()

            } catch (e: CancellationException) {

                _sincronizacion.value = null

                throw e

            } catch (e: Exception) {

                _sincronizacion.value =
                    ResultadoSincronizacion.Error(
                        "No se pudo sincronizar"
                    )
            }
        }
    }



    init {

        cargarRecordatorios()
        if (repositorio.sincronizacionAutomatica) viewModelScope.launch {
            // Reintentos mientras existe la sesión. Al salir, el scope se cancela.
            // Tras muerte del proceso, Room conserva pendientes y se retoman al abrir.
            while (true) {
                sincronizarRecordatorios()
                kotlinx.coroutines.delay(30_000)
            }
        }
    }


    // ======================================
    // OBSERVAR ROOM
    // ======================================

    fun cargarRecordatorios() {

        trabajoDeCarga?.cancel()

        _uiState.value = ListadoUiState.Cargando

        trabajoDeCarga = viewModelScope.launch {

            try {

                repositorio.observarTodos()
                    .collect { lista ->

                        _uiState.value =
                            if (lista.isEmpty()) {

                                ListadoUiState.Vacio

                            } else {

                                ListadoUiState.ConDatos(lista)
                            }
                    }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _uiState.value = ListadoUiState.Error(
                    "No se pudieron cargar los recordatorios"
                )
            }
        }
    }


    // ======================================
    // SIMULACIONES
    // ======================================

    fun simularEstadoVacio() {

        trabajoDeCarga?.cancel()

        _uiState.value = ListadoUiState.Vacio
    }


    fun simularEstadoError() {

        trabajoDeCarga?.cancel()

        _uiState.value = ListadoUiState.Error(
            "Error simulado. Intentá nuevamente."
        )
    }


    // ======================================
    // FUNCIONES DEL FORMULARIO
    // ======================================

    fun cambiarTitulo(nuevoTitulo: String) {

        _formularioState.update {

            it.copy(
                titulo = nuevoTitulo,
                errorGeneral = null
            )
        }
    }


    fun cambiarDescripcion(
        nuevaDescripcion: String
    ) {

        _formularioState.update {

            it.copy(
                descripcion = nuevaDescripcion,
                errorGeneral = null
            )
        }
    }


    fun seleccionarCategoria(
        categoria: Categoria
    ) {

        _formularioState.update {

            it.copy(
                categoria = categoria,
                errorGeneral = null
            )
        }
    }


    fun seleccionarPrioridad(
        prioridad: Prioridad
    ) {

        _formularioState.update {

            it.copy(
                prioridad = prioridad,
                errorGeneral = null
            )
        }
    }


    // ======================================
    // PREPARAR EDICIÓN
    // ======================================

    fun prepararEdicion(
        recordatorio: Recordatorio
    ) {

        _formularioState.value = FormularioUiState(

            titulo = recordatorio.titulo,

            descripcion =
                recordatorio.descripcion.orEmpty(),

            categoria = recordatorio.categoria,

            prioridad = recordatorio.prioridad,

            idEdicion = recordatorio.id
        )
    }


    // ======================================
    // CREAR O ACTUALIZAR
    // ======================================

    fun guardarRecordatorio() {

        val datos = _formularioState.value

        if (!datos.puedeGuardar ||
            datos.guardadoExitoso
        ) return

        _formularioState.update {

            it.copy(
                guardando = true,
                guardadoExitoso = false,
                errorGeneral = null
            )
        }

        viewModelScope.launch {

            try {

                val id = datos.idEdicion

                if (id == null) {

                    // Crear un recordatorio nuevo
                    repositorio.crear(

                        titulo = datos.titulo,

                        descripcion = datos.descripcion
                            .ifBlank { null },

                        categoria = requireNotNull(
                            datos.categoria
                        ),

                        prioridad = requireNotNull(
                            datos.prioridad
                        )
                    )

                } else {

                    // Recuperar el recordatorio existente
                    val existente = checkNotNull(
                        repositorio.obtenerPorId(id)
                    )

                    // Modificar conservando el ID
                    val actualizado = existente.copy(

                        titulo = datos.titulo.trim(),

                        descripcion = datos.descripcion
                            .trim()
                            .ifBlank { null },

                        categoria = requireNotNull(
                            datos.categoria
                        ),

                        prioridad = requireNotNull(
                            datos.prioridad
                        )
                    )

                    repositorio.actualizar(actualizado)
                }

                _formularioState.update {

                    it.copy(
                        guardando = false,
                        guardadoExitoso = true,
                        errorGeneral = null
                    )
                }

                if (trabajoDeCarga?.isActive != true) {

                    cargarRecordatorios()
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

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
    // CAMBIAR ESTADO DEL RECORDATORIO
    // ======================================

    fun cambiarEstadoRecordatorio(id: Long) {

        if (_detalleOperacion.value.ocupado) return

        _detalleOperacion.value =
            DetalleOperacionUiState(ocupado = true)

        viewModelScope.launch {

            try {

                val existente = checkNotNull(
                    repositorio.obtenerPorId(id)
                )

                val nuevoEstado =
                    if (existente.estado ==
                        EstadoRecordatorio.Pendiente
                    ) {

                        EstadoRecordatorio.Completado

                    } else {

                        EstadoRecordatorio.Pendiente
                    }

                repositorio.actualizar(

                    existente.copy(
                        estado = nuevoEstado
                    )
                )

                _detalleOperacion.value =
                    DetalleOperacionUiState()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _detalleOperacion.value =
                    DetalleOperacionUiState(

                        error =
                            "No se pudo cambiar el estado"
                    )
            }
        }
    }


    // ======================================
    // ELIMINAR RECORDATORIO
    // ======================================

    fun eliminarRecordatorio(id: Long) {

        if (_detalleOperacion.value.ocupado) return

        _detalleOperacion.value =
            DetalleOperacionUiState(ocupado = true)

        viewModelScope.launch {

            try {

                repositorio.eliminar(id)

                // Confirmamos que terminó correctamente
                _detalleOperacion.value =
                    DetalleOperacionUiState(

                        eliminadoId = id
                    )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _detalleOperacion.value =
                    DetalleOperacionUiState(

                        error =
                            "No se pudo eliminar el recordatorio"
                    )
            }
        }
    }


    // ======================================
    // LIMPIAR ESTADOS
    // ======================================

    fun limpiarOperacionDetalle() {

        if (!_detalleOperacion.value.ocupado) {

            _detalleOperacion.value =
                DetalleOperacionUiState()
        }
    }


    fun limpiarFormulario() {

        _formularioState.value = FormularioUiState()
    }
}
