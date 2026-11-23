package com.example.recordatoriosteclab

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriosteclab.ui.theme.RecordatoriosTeclabTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AplicacionAutenticada(context: Context) {
    val firebase = remember { FirebaseApp.initializeApp(context) }
    if (firebase == null) {
        RecordatoriosTeclabTheme {
            Surface(Modifier.fillMaxSize()) {
                MensajeEstado(
                    titulo = "Configuración Firebase pendiente",
                    descripcion = "Agregá el archivo real app/google-services.json, habilitá Email/Password en Firebase y recompilá la aplicación."
                )
            }
        }
        return
    }
    val authVm: AuthViewModel = viewModel {
        AuthViewModel(FirebaseAuthRepository(FirebaseAuth.getInstance(firebase)))
    }
    val estado by authVm.estado.collectAsStateWithLifecycle()
    val usuario = estado.usuario
    if (usuario != null) {
        // Al cambiar de cuenta se descartan navegación, formularios y ViewModels anteriores.
        key(usuario.uid) { SesionAutenticada(context, usuario, estado, authVm::salir) }
    } else {
        RecordatoriosTeclabTheme {
            Surface(Modifier.fillMaxSize()) {
                if (estado.iniciando) MensajeEstado("Recuperando sesión", "", cargando = true)
                else NavegacionAutenticacion(estado, authVm)
            }
        }
    }
}

@Composable
private fun SesionAutenticada(
    context: Context,
    usuario: UsuarioSesion,
    auth: AuthUiState,
    onSalir: () -> Unit
) {
    val owner = remember {
        object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        val vm: RecordatoriosViewModel = viewModel {
            RecordatoriosViewModel(RecordatoriosRepositoryRoom(
                RecordatoriosDatabase.obtenerBaseDeDatos(context, usuario.uid)
            ))
        }
        val preferenciasVm: PreferenciasViewModel = viewModel {
            PreferenciasViewModel(PreferenciasRepository(context, usuario.uid))
        }
        val preferencias by preferenciasVm.preferencias.collectAsStateWithLifecycle()
        RecordatoriosTeclabTheme(darkTheme = preferencias.temaOscuro) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(usuario.email.orEmpty(), modifier = Modifier.weight(1f))
                        TextButton(onClick = onSalir, enabled = !auth.cargando) { Text("Cerrar sesión") }
                    }
                    auth.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Box(Modifier.weight(1f)) { NavegacionRecordatorios(vm, preferenciasVm) }
                }
            }
        }
    }
}
