package com.example.recordatoriosteclab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*

@Composable
fun NavegacionAutenticacion(estado: AuthUiState, vm: AuthViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "ingreso") {
        for (ruta in listOf("ingreso", "registro")) {
            composable(ruta) {
                PantallaAutenticacion(estado, ruta == "registro", vm::autenticar) {
                    vm.limpiarError()
                    if (ruta == "registro") nav.popBackStack()
                    else nav.navigate("registro") { launchSingleTop = true }
                }
            }
        }
    }
}

@Composable
fun PantallaAutenticacion(
    estado: AuthUiState,
    registro: Boolean,
    onEnviar: (String, String, Boolean, String) -> Unit,
    onCambiar: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Las contraseñas no se guardan en SavedState ni en disco.
    var password by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Recordatorios Teclab", style = MaterialTheme.typography.headlineMedium)
        Text(if (registro) "Crear cuenta" else "Iniciar sesión", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(email, { email = it }, label = { Text("Email") },
            singleLine = true, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") },
            singleLine = true, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        if (registro) OutlinedTextField(confirmacion, { confirmacion = it },
            label = { Text("Confirmar contraseña") }, singleLine = true,
            enabled = !estado.cargando, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (estado.cargando) CircularProgressIndicator()
        Button(onClick = { onEnviar(email, password, registro, confirmacion) },
            enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
            Text(if (registro) "Registrarme" else "Ingresar")
        }
        TextButton(onClick = onCambiar, enabled = !estado.cargando) {
            Text(if (registro) "Ya tengo cuenta" else "Crear una cuenta")
        }
    }
}
