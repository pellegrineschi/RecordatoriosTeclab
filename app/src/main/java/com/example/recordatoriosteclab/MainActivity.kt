
package com.example.recordatoriosteclab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

// Actividad principal de nuestra aplicación
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostramos nuestra pantalla de recordatorios
        setContent {
            RecordatoriosTeclabTheme {
                PantallaRecordatorios()
            }
        }
    }
}

// Pantalla principal creada con Jetpack Compose
@Composable
fun PantallaRecordatorios() {

    Scaffold { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            contentPadding = PaddingValues(16.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Título de la pantalla
            item {
                Text(
                    text = "Mis recordatorios",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Total: ${DatosSimulados.recordatorios.size} tareas",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Pendientes: ${DatosSimulados.pendientes.size}"
                )

                Text(
                    text = "Prioridad alta: ${DatosSimulados.altaPrioridad.size}"
                )

                Text(
                    text = "Categorías diferentes: ${DatosSimulados.categorias.size}"
                )

            }

            // Recorremos la lista de recordatorios
            items(
                items = DatosSimulados.recordatorios,
                key = { it.id }
            ) { recordatorio ->

                TarjetaRecordatorio(recordatorio)
            }
        }
    }
}

// Tarjeta que muestra cada recordatorio
@Composable
fun TarjetaRecordatorio(recordatorio: Recordatorio) {

    // Convertimos la prioridad en texto
    val prioridad = when (recordatorio.prioridad) {
        Prioridad.Alta -> "Alta"
        Prioridad.Media -> "Media"
        Prioridad.Baja -> "Baja"
    }

    // Convertimos el estado en texto
    val estado = when (recordatorio.estado) {
        EstadoRecordatorio.Pendiente -> "Pendiente"
        EstadoRecordatorio.Completado -> "Completado"
    }

    // Obtenemos la etiqueta desde nuestro Map
    val categoria =
        DatosSimulados.etiquetasPorCategoria[recordatorio.categoria]
            ?: recordatorio.categoria.nombre

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Text(text = "Categoría: $categoria")

            Text(text = "Prioridad: $prioridad")

            Text(text = "Estado: $estado")
        }
    }
}

// Vista previa para Android Studio
@Preview(showBackground = true)
@Composable
fun PantallaPreview() {
    RecordatoriosTeclabTheme {
        PantallaRecordatorios()
    }
}
