package com.example.recordatoriosteclab



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearRecordatorio(
    estado: FormularioUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (Categoria) -> Unit,
    onPrioridadChange: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Controla si el menú de categorías está abierto
    var menuAbierto by remember { mutableStateOf(false) }

    val tituloInvalido =
        estado.titulo.isNotEmpty() && !estado.tituloValido

    val categorias =
        DatosSimulados.categorias.sortedBy { it.nombre }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo recordatorio") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // CAMPO 1: Título
            OutlinedTextField(
                value = estado.titulo,
                onValueChange = onTituloChange,
                label = { Text("Título") },
                singleLine = true,
                enabled = !estado.guardando,
                isError = tituloInvalido,
                supportingText = {
                    Text(
                        if (tituloInvalido)
                            "El título debe tener al menos 4 caracteres"
                        else
                            "Mínimo 4 caracteres"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (tituloInvalido) {
                            error("Título demasiado corto")
                        }
                    }
            )

            // CAMPO 2: Descripción
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = onDescripcionChange,
                label = { Text("Descripción") },
                minLines = 3,
                maxLines = 5,
                enabled = !estado.guardando,
                isError = !estado.descripcionValida,
                supportingText = {
                    Text(
                        if (!estado.descripcionValida)
                            "La descripción supera los 200 caracteres"
                        else
                            "${estado.descripcion.length}/200 caracteres"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // CAMPO 3: Categoría
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium
            )

            Box {
                OutlinedButton(
                    onClick = { menuAbierto = true },
                    enabled = !estado.guardando
                ) {
                    Text(
                        estado.categoria?.nombre
                            ?: "Seleccionar categoría"
                    )
                }

                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nombre) },
                            onClick = {
                                onCategoriaChange(categoria)
                                menuAbierto = false
                            }
                        )
                    }
                }
            }

            if (!estado.categoriaValida) {
                Text(
                    "Seleccioná una categoría válida",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // CAMPO 4: Prioridad
            Text(
                text = "Prioridad",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opciones = listOf(
                    Prioridad.Alta to "Alta",
                    Prioridad.Media to "Media",
                    Prioridad.Baja to "Baja"
                )

                opciones.forEach { (prioridad, nombre) ->
                    FilterChip(
                        selected = estado.prioridad == prioridad,
                        onClick = {
                            onPrioridadChange(prioridad)
                        },
                        enabled = !estado.guardando,
                        label = { Text(nombre) }
                    )
                }
            }

            if (!estado.prioridadValida) {
                Text(
                    "Seleccioná una prioridad",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Mensaje de error general
            estado.errorGeneral?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Confirmación
            if (estado.guardadoExitoso) {
                Text(
                    text = "Recordatorio guardado correctamente",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Botones de acción
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = onCancelar,
                    enabled = !estado.guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = onGuardar,
                    enabled = estado.puedeGuardar &&
                            !estado.guardadoExitoso,
                    modifier = Modifier.weight(1f)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

// Vista previa para Android Studio
@Preview(showBackground = true)
@Composable
fun VistaPreviaCrearRecordatorio() {
    RecordatoriosTeclabTheme {
        PantallaCrearRecordatorio(
            estado = FormularioUiState(),
            onTituloChange = {},
            onDescripcionChange = {},
            onCategoriaChange = {},
            onPrioridadChange = {},
            onGuardar = {},
            onCancelar = {}
        )
    }
}
