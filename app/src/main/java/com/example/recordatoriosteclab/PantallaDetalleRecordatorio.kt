package com.example.recordatoriosteclab



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleRecordatorio(
    recordatorio: Recordatorio?,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle del recordatorio")
                },
                navigationIcon = {
                    TextButton(onClick = onVolver) {
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

            // Si el recordatorio no existe
            if (recordatorio == null) {

                Text("No se encontró el recordatorio")

                Button(onClick = onVolver) {
                    Text("Regresar al listado")
                }

            } else {

                // Mostramos toda la información
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = recordatorio.descripcion
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

                val estado = when (recordatorio.estado) {
                    EstadoRecordatorio.Pendiente ->
                        "Pendiente"

                    EstadoRecordatorio.Completado ->
                        "Completado"
                }

                Text("Estado: $estado")

                Text(
                    "Ubicación: ${
                        recordatorio.ubicacion ?: "No asignada"
                    }"
                )

                Text(
                    "Aviso: ${
                        recordatorio.aviso ?: "No configurado"
                    }"
                )
            }
        }
    }
}

// Vista previa con un recordatorio simulado
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
