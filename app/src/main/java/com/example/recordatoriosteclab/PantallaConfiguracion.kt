
package com.example.recordatoriosteclab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme

// ==========================================
// PANTALLA DE CONFIGURACIÓN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    preferencias: PreferenciasUsuario,
    onTemaOscuroChange: (Boolean) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onOrdenChange: (OrdenListado) -> Unit,
    onVolver: () -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier,

        topBar = {
            TopAppBar(
                title = {
                    Text("Configuración")
                },

                navigationIcon = {
                    TextButton(
                        onClick = onVolver
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
                Arrangement.spacedBy(16.dp)
        ) {

            // ==================================
            // APARIENCIA
            // ==================================

            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleLarge
            )

            OpcionInterruptor(
                titulo = "Tema oscuro",
                descripcion = "Cambiar la apariencia de la aplicación",
                activado = preferencias.temaOscuro,
                onCambio = onTemaOscuroChange
            )

            HorizontalDivider()


            // ==================================
            // RECORDATORIOS
            // ==================================

            Text(
                text = "Recordatorios",
                style = MaterialTheme.typography.titleLarge
            )

            OpcionInterruptor(
                titulo = "Mostrar completados",
                descripcion = "Incluir las tareas finalizadas",
                activado = preferencias.mostrarCompletados,
                onCambio = onMostrarCompletadosChange
            )

            HorizontalDivider()


            // ==================================
            // ORDENAMIENTO
            // ==================================

            Text(
                text = "Orden del listado",
                style = MaterialTheme.typography.titleMedium
            )

            OpcionOrden(
                titulo = "Más recientes primero",
                seleccionado = preferencias.ordenListado ==
                        OrdenListado.RECIENTES,

                onClick = {
                    onOrdenChange(OrdenListado.RECIENTES)
                }
            )

            OpcionOrden(
                titulo = "Orden alfabético",
                seleccionado = preferencias.ordenListado ==
                        OrdenListado.TITULO,

                onClick = {
                    onOrdenChange(OrdenListado.TITULO)
                }
            )

            HorizontalDivider()


            // ==================================
            // MENSAJE DE ERROR
            // ==================================

            error?.let { mensaje ->

                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Las preferencias se guardan automáticamente.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// ==========================================
// COMPONENTE REUTILIZABLE: INTERRUPTOR
// ==========================================

@Composable
fun OpcionInterruptor(
    titulo: String,
    descripcion: String,
    activado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Switch(
            checked = activado,
            onCheckedChange = onCambio
        )
    }
}


// ==========================================
// COMPONENTE REUTILIZABLE: ORDENAMIENTO
// ==========================================

@Composable
fun OpcionOrden(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = seleccionado,
            onClick = onClick
        )

        Text(text = titulo)
    }
}


// ==========================================
// VISTA PREVIA
// ==========================================

@Preview(showBackground = true)
@Composable
fun VistaPreviaConfiguracion() {

    RecordatoriosTeclabTheme {

        PantallaConfiguracion(
            preferencias = PreferenciasUsuario(),
            onTemaOscuroChange = {},
            onMostrarCompletadosChange = {},
            onOrdenChange = {},
            onVolver = {}
        )
    }
}
