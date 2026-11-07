
package com.example.recordatoriosteclab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

// ACTIVIDAD PRINCIPAL
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            // Creamos el ViewModel con el repositorio
            val vm: RecordatoriosViewModel = viewModel {
                RecordatoriosViewModel(
                    RecordatoriosRepositoryEnMemoria()
                )
            }

            RecordatoriosTeclabTheme {

                // Conectamos las pantallas mediante Navigation Compose
                NavegacionRecordatorios(vm)

            }
        }
    }
}

// PANTALLA PRINCIPAL DE RECORDATORIOS
@Composable
fun PantallaRecordatorios(
    estado: ListadoUiState,
    onReintentar: () -> Unit,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit
) {

    Scaffold(

        // Botón flotante para crear recordatorios
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCrearRecordatorio
            ) {
                Text("+")
            }
        }

    ) { innerPadding ->

        // Mostramos la pantalla según su estado
        when (estado) {

            // ESTADO: CARGANDO
            ListadoUiState.Cargando -> {

                MensajeEstado(
                    titulo = "Cargando recordatorios...",
                    descripcion = "Un momento, por favor.",
                    cargando = true,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // ESTADO: VACÍO
            ListadoUiState.Vacio -> {

                MensajeEstado(
                    titulo = "Sin recordatorios",
                    descripcion = "Todavía no registraste tareas.",
                    textoAccion = "Crear recordatorio",
                    onAccion = onCrearRecordatorio,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // ESTADO: ERROR
            is ListadoUiState.Error -> {

                MensajeEstado(
                    titulo = "Ocurrió un error",
                    descripcion = estado.mensaje,
                    textoAccion = "Reintentar",
                    onAccion = onReintentar,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // ESTADO: CON DATOS
            is ListadoUiState.ConDatos -> {

                val lista = estado.recordatorios

                // Calculamos los contadores
                val pendientes = lista.count {
                    it.estado == EstadoRecordatorio.Pendiente
                }

                val altaPrioridad = lista.count {
                    it.prioridad == Prioridad.Alta
                }

                val categorias = lista
                    .map { it.categoria }
                    .toSet()
                    .size

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),

                    contentPadding = PaddingValues(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    // Encabezado y contadores
                    item {

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = "Mis recordatorios",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Text("Total: ${lista.size} tareas")

                            Text("Pendientes: $pendientes")

                            Text("Prioridad alta: $altaPrioridad")

                            Text("Categorías diferentes: $categorias")
                        }
                    }

                    // Mostramos cada recordatorio
                    items(
                        items = lista,
                        key = { it.id }
                    ) { recordatorio ->

                        TarjetaRecordatorio(
                            recordatorio = recordatorio,

                            // Al tocar, abrimos el detalle
                            onClick = {
                                onRecordatorioClick(
                                    recordatorio.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// COMPONENTE REUTILIZABLE PARA ESTADOS
@Composable
fun MensajeEstado(
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    textoAccion: String = "",
    onAccion: (() -> Unit)? = null
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        // Indicador de carga
        if (cargando) {

            CircularProgressIndicator()

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(text = descripcion)

        // Botón opcional para reintentar o crear
        if (onAccion != null) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onAccion
            ) {
                Text(textoAccion)
            }
        }
    }
}

// TARJETA REUTILIZABLE DE RECORDATORIO
@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

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

    // Tarjeta que permite recibir clics
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = recordatorio.titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Categoría: ${recordatorio.categoria.nombre}"
            )

            Text(
                text = "Prioridad: $prioridad"
            )

            Text(
                text = "Estado: $estado"
            )
        }
    }
}

// VISTA PREVIA PARA ANDROID STUDIO
@Preview(showBackground = true)
@Composable
fun PantallaPreview() {

    RecordatoriosTeclabTheme {

        PantallaRecordatorios(

            estado = ListadoUiState.ConDatos(
                DatosSimulados.recordatorios
            ),

            onReintentar = {},

            // Parámetros que faltaban
            onCrearRecordatorio = {},

            onRecordatorioClick = {}
        )
    }
}
