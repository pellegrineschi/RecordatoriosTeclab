
package com.example.recordatoriosteclab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavegacionRecordatorios(

    vm: RecordatoriosViewModel,

    preferenciasVm: PreferenciasViewModel
) {

    val navController = rememberNavController()

    val listado by vm.uiState
        .collectAsStateWithLifecycle()

    val formulario by vm.formularioState
        .collectAsStateWithLifecycle()

    val preferencias by preferenciasVm.preferencias
        .collectAsStateWithLifecycle()

    val errorPreferencias by preferenciasVm.error
        .collectAsStateWithLifecycle()

    val detalleOperacion by vm.detalleOperacion
        .collectAsStateWithLifecycle()

    val sincronizacion by vm.sincronizacion
        .collectAsStateWithLifecycle()


    // ======================================
    // FILTROS Y ORDENAMIENTO
    // ======================================

    val listadoVisible = when (val estado = listado) {

        is ListadoUiState.ConDatos -> {

            val filtrados =
                if (preferencias.mostrarCompletados) {

                    estado.recordatorios

                } else {

                    estado.recordatorios.filter {

                        it.estado !=
                                EstadoRecordatorio.Completado
                    }
                }

            val ordenados = when (
                preferencias.ordenListado
            ) {

                OrdenListado.RECIENTES -> {

                    filtrados.sortedWith(

                        compareByDescending<Recordatorio> {

                            it.fechaCreacion

                        }.thenByDescending {

                            it.id
                        }
                    )
                }

                OrdenListado.TITULO -> {

                    filtrados.sortedBy {

                        it.titulo.lowercase()
                    }
                }
            }

            ListadoUiState.ConDatos(ordenados)
        }

        else -> estado
    }


    // ======================================
    // NAVEGACIÓN
    // ======================================

    NavHost(

        navController = navController,

        startDestination = "listado"

    ) {

        // ==================================
        // LISTADO
        // ==================================

        composable("listado") {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),

                    horizontalArrangement =
                        Arrangement.End
                ) {

                    OutlinedButton(

                        onClick = {

                            navController.navigate(
                                "configuracion"
                            )
                        }
                    ) {

                        Text("Configuración")
                    }
                }

// ======================================
// SINCRONIZACIÓN CON EL SERVIDOR
// ======================================

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = vm::sincronizarRecordatorios,

                        enabled = sincronizacion !=
                                ResultadoSincronizacion.EnCurso,

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        if (sincronizacion ==
                            ResultadoSincronizacion.EnCurso
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text("Sincronizando...")

                        } else {

                            Text("Sincronizar")
                        }
                    }

                    when (val resultado = sincronizacion) {

                        is ResultadoSincronizacion.Exito -> {

                            Text(
                                text = "Sincronización correcta. " +
                                        "Enviados: ${resultado.enviados}",

                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        is ResultadoSincronizacion.Error -> {

                            Text(
                                text = resultado.mensaje,

                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> Unit
                    }
                }

                Text(

                    text = "Pruebas de estados",

                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 8.dp
                    ),

                    style = MaterialTheme
                        .typography.labelMedium
                )

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)

                ) {

                    OutlinedButton(

                        onClick = vm::cargarRecordatorios,

                        modifier = Modifier.weight(1f)

                    ) {

                        Text("Datos")
                    }

                    OutlinedButton(

                        onClick = vm::simularEstadoVacio,

                        modifier = Modifier.weight(1f)

                    ) {

                        Text("Vacío")
                    }

                    OutlinedButton(

                        onClick = vm::simularEstadoError,

                        modifier = Modifier.weight(1f)

                    ) {

                        Text("Error")
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    PantallaRecordatorios(

                        estado = listadoVisible,

                        onReintentar =
                            vm::cargarRecordatorios,

                        onCrearRecordatorio = {

                            vm.limpiarFormulario()

                            navController.navigate("crear")
                        },

                        onRecordatorioClick = { id ->

                            vm.limpiarOperacionDetalle()

                            navController.navigate(
                                "detalle/$id"
                            )
                        }
                    )
                }
            }
        }


        // ==================================
        // FORMULARIO: CREAR O EDITAR
        // ==================================

        composable("crear") {

            // Evitar salir durante el guardado
            BackHandler(
                enabled = formulario.guardando
            ) {}

            LaunchedEffect(
                formulario.guardadoExitoso
            ) {

                if (formulario.guardadoExitoso) {

                    navController.popBackStack()

                    vm.limpiarFormulario()
                }
            }

            PantallaCrearRecordatorio(

                estado = formulario,

                onTituloChange =
                    vm::cambiarTitulo,

                onDescripcionChange =
                    vm::cambiarDescripcion,

                onCategoriaChange =
                    vm::seleccionarCategoria,

                onPrioridadChange =
                    vm::seleccionarPrioridad,

                onGuardar =
                    vm::guardarRecordatorio,

                onCancelar = {

                    vm.limpiarFormulario()

                    navController.popBackStack()
                }
            )
        }


        // ==================================
        // DETALLE
        // ==================================

        composable("detalle/{id}") { entrada ->

            val id = entrada.arguments
                ?.getString("id")
                ?.toLongOrNull()

            val recordatorio =
                (listado as? ListadoUiState.ConDatos)
                    ?.recordatorios
                    ?.firstOrNull {

                        it.id == id
                    }

            BackHandler(
                enabled = detalleOperacion.ocupado
            ) {}

            // Regresar solo si se eliminó correctamente
            LaunchedEffect(
                detalleOperacion.eliminadoId
            ) {

                if (id != null &&
                    detalleOperacion.eliminadoId == id
                ) {

                    navController.popBackStack()

                    vm.limpiarOperacionDetalle()
                }
            }

            PantallaDetalleRecordatorio(

                recordatorio = recordatorio,

                operacion = detalleOperacion,

                onVolver = {

                    vm.limpiarOperacionDetalle()

                    navController.popBackStack()
                },

                onEditar = {

                    recordatorio?.let {

                        vm.prepararEdicion(it)

                        vm.limpiarOperacionDetalle()

                        navController.navigate("crear")
                    }
                },

                onCambiarEstado = {

                    id?.let {

                        vm.cambiarEstadoRecordatorio(it)
                    }
                },

                onEliminar = {

                    id?.let {

                        vm.eliminarRecordatorio(it)
                    }
                }
            )
        }


        // ==================================
        // CONFIGURACIÓN
        // ==================================

        composable("configuracion") {

            PantallaConfiguracion(

                preferencias = preferencias,

                onTemaOscuroChange =
                    preferenciasVm::cambiarTemaOscuro,

                onMostrarCompletadosChange =
                    preferenciasVm::cambiarMostrarCompletados,

                onOrdenChange =
                    preferenciasVm::cambiarOrdenListado,

                error = errorPreferencias,

                onVolver = {

                    preferenciasVm.limpiarError()

                    navController.popBackStack()
                }
            )
        }
    }
}
