package com.example.recordatoriosteclab

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PantallaAutenticacionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun registroEnviaEmailPasswordYConfirmacion() {
        var recibido: List<Any>? = null
        compose.setContent {
            MaterialTheme {
                PantallaAutenticacion(AuthUiState(iniciando = false), true,
                    { email, password, registro, confirmacion ->
                        recibido = listOf(email, password, registro, confirmacion)
                    }, {})
            }
        }
        compose.onNodeWithText("Email").performTextInput("prueba@example.com")
        compose.onNodeWithText("Contraseña").performTextInput("secreto123")
        compose.onNodeWithText("Confirmar contraseña").performTextInput("secreto123")
        compose.onNodeWithText("Registrarme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("prueba@example.com", "secreto123", true, "secreto123"), recibido)
        }
    }

    @Test fun cargaBloqueaIngresoYCambioDePantalla() {
        compose.setContent {
            MaterialTheme {
                PantallaAutenticacion(AuthUiState(iniciando = false, cargando = true), false,
                    { _, _, _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Ingresar").assertIsNotEnabled()
        compose.onNodeWithText("Crear una cuenta").assertIsNotEnabled()
    }
}
