package com.example.recordatoriosteclab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleRecordatorio(
    recordatorio: Recordatorio?,
    onVolver: () -> Unit,
    onEditar: () -> Unit = {},
    onCambiarEstado: () -> Unit = {},
    onEliminar: () -> Unit = {},
    operacion: DetalleOperacionUiState =
        DetalleOperacionUiState(),
    modifier: Modifier = Modifier
) {

    var confirmarEliminacion by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle del recordatorio")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onVolver,
                        enabled = !operacion.ocupado
                    ) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            if (recordatorio == null) {

                if (operacion.ocupado ||
                    operacion.eliminadoId != null
                ) {

                    CircularProgressIndicator()

                } else {

                    Text("No se encontró el recordatorio")

                    Button(onClick = onVolver) {
                        Text("Regresar al listado")
                    }
                }

            } else {

                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    recordatorio.descripcion
                        ?: "Sin descripción"
                )

                HorizontalDivider()

                Text("ID: ${recordatorio.id}")

                Text(
                    "Categoría: ${recordatorio.categoria.nombre}"
                )

                val prioridad = when (recordatorio.prioridad) {
                    Prioridad.Alta -> "Alta"
                    Prioridad.Media -> "Media"
                    Prioridad.Baja -> "Baja"
                }

                Text("Prioridad: $prioridad")

                val completado =
                    recordatorio.estado ==
                        EstadoRecordatorio.Completado

                Text(
                    if (completado)
                        "Estado: Completado"
                    else
                        "Estado: Pendiente"
                )

                Text(
                    "Ubicación: ${recordatorio.ubicacion ?: "No asignada"}"
                )

                Text(
                    "Aviso: ${recordatorio.aviso ?: "No configurado"}"
                )

                val sincronizacion = when (
                    recordatorio.estadoSincronizacion
                ) {
                    EstadoSincronizacion.PENDIENTE ->
                        "Pendiente"

                    EstadoSincronizacion.SINCRONIZADO ->
                        "Sincronizado"

                    EstadoSincronizacion.ERROR ->
                        "Error de sincronización"
                }

                Text("Sincronización: $sincronizacion")

                HorizontalDivider()

                // EDITAR
                Button(
                    onClick = onEditar,
                    enabled = !operacion.ocupado,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar recordatorio")
                }

                // COMPLETAR O VOLVER A PENDIENTE
                OutlinedButton(
                    onClick = onCambiarEstado,
                    enabled = !operacion.ocupado,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (completado)
                            "Marcar como pendiente"
                        else
                            "Marcar como completado"
                    )
                }

                // ELIMINAR
                OutlinedButton(
                    onClick = {
                        confirmarEliminacion = true
                    },
                    enabled = !operacion.ocupado,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar recordatorio")
                }

                if (operacion.ocupado) {
                    CircularProgressIndicator()
                }

                operacion.error?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // CONFIRMAR ELIMINACIÓN
    if (confirmarEliminacion) {

        AlertDialog(
            onDismissRequest = {
                confirmarEliminacion = false
            },

            title = {
                Text("Eliminar recordatorio")
            },

            text = {
                Text(
                    "¿Estás seguro de que querés eliminar esta tarea?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                        onEliminar()
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VistaPreviaDetalle() {
    RecordatoriosTeclabTheme {
        PantallaDetalleRecordatorio(
            recordatorio =
                DatosSimulados.recordatorios.firstOrNull(),
            onVolver = {}
        )
    }
}
