
package com.example.recordatoriosteclab

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

// ==========================================
// NAVEGACIÓN PRINCIPAL DE LA APLICACIÓN
// ==========================================

@Composable
fun NavegacionRecordatorios(
    vm: RecordatoriosViewModel
) {

    // Controla los cambios entre pantallas
    val navController = rememberNavController()

    // Observamos el estado del listado
    val listado by vm.uiState.collectAsStateWithLifecycle()

    // Observamos el estado del formulario
    val formulario by vm.formularioState.collectAsStateWithLifecycle()

    // ======================================
    // GRAFO DE NAVEGACIÓN
    // ======================================

    NavHost(
        navController = navController,
        startDestination = "listado"
    ) {

        // ==================================
        // PANTALLA 1: LISTADO
        // ==================================

        composable("listado") {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // --------------------------
                // BOTONES DE SIMULACIÓN
                // --------------------------

                Text(
                    text = "Pruebas de estados",
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 8.dp
                    ),
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    // BOTÓN DATOS
                    OutlinedButton(
                        onClick = {
                            vm.cargarRecordatorios()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Datos")
                    }

                    // BOTÓN VACÍO
                    OutlinedButton(
                        onClick = {
                            vm.simularEstadoVacio()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vacío")
                    }

                    // BOTÓN ERROR
                    OutlinedButton(
                        onClick = {
                            vm.simularEstadoError()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Error")
                    }
                }

                // --------------------------
                // LISTADO DE RECORDATORIOS
                // --------------------------

                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    PantallaRecordatorios(

                        // Estado actual del listado
                        estado = listado,

                        // Reintentar una carga fallida
                        onReintentar = {
                            vm.cargarRecordatorios()
                        },

                        // Abrir formulario de creación
                        onCrearRecordatorio = {

                            vm.limpiarFormulario()

                            navController.navigate("crear")
                        },

                        // Abrir detalle de una tarea
                        onRecordatorioClick = { id ->

                            navController.navigate(
                                "detalle/$id"
                            )
                        }
                    )
                }
            }
        }


        // ==================================
        // PANTALLA 2: CREAR RECORDATORIO
        // ==================================

        composable("crear") {

            // Cuando el guardado termina correctamente,
            // regresamos al listado anterior.
            LaunchedEffect(formulario.guardadoExitoso) {

                if (formulario.guardadoExitoso) {

                    navController.popBackStack()

                    // Limpiamos los campos
                    vm.limpiarFormulario()
                }
            }

            PantallaCrearRecordatorio(

                // Estado del formulario
                estado = formulario,

                // Cambios del título
                onTituloChange = vm::cambiarTitulo,

                // Cambios de la descripción
                onDescripcionChange = vm::cambiarDescripcion,

                // Selección de categoría
                onCategoriaChange = vm::seleccionarCategoria,

                // Selección de prioridad
                onPrioridadChange = vm::seleccionarPrioridad,

                // Guardar el recordatorio
                onGuardar = {
                    vm.guardarRecordatorio()
                },

                // Cancelar y regresar
                onCancelar = {

                    vm.limpiarFormulario()

                    navController.popBackStack()
                }
            )
        }


        // ==================================
        // PANTALLA 3: DETALLE
        // ==================================

        composable("detalle/{id}") { entrada ->

            // Recibimos el identificador de la tarea
            val id = entrada.arguments
                ?.getString("id")
                ?.toLongOrNull()

            // Buscamos la tarea en los datos cargados
            val recordatorio =
                (listado as? ListadoUiState.ConDatos)
                    ?.recordatorios
                    ?.firstOrNull {
                        it.id == id
                    }

            PantallaDetalleRecordatorio(

                recordatorio = recordatorio,

                // Regresar al listado
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}
