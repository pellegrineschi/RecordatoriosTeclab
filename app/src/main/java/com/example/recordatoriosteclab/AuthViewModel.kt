package com.example.recordatoriosteclab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val iniciando: Boolean = true,
    val usuario: UsuarioSesion? = null,
    val cargando: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AuthUiState())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repository.usuario.collect { usuario ->
                _estado.update { it.copy(iniciando = false, usuario = usuario, error = null) }
            }
        }
    }

    fun autenticar(email: String, password: String, registro: Boolean, confirmacion: String) {
        if (_estado.value.cargando || _estado.value.iniciando) return
        val error = validarCredenciales(email.trim(), password, registro, confirmacion)
        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                if (registro) repository.registrar(email.trim(), password)
                else repository.ingresar(email.trim(), password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(error = mensajeAuth(e)) }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    fun limpiarError() { _estado.update { it.copy(error = null) } }

    fun salir() {
        if (_estado.value.cargando) return
        try {
            repository.salir()
            _estado.value = AuthUiState(iniciando = false)
        } catch (e: Exception) {
            _estado.update { it.copy(error = mensajeAuth(e)) }
        }
    }
}

internal fun validarCredenciales(email: String, password: String, registro: Boolean, confirmacion: String): String? = when {
    !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "Ingresá un email válido."
    password.isEmpty() -> "Ingresá tu contraseña."
    registro && password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
    registro && password != confirmacion -> "Las contraseñas no coinciden."
    else -> null
}

internal fun mensajeAuth(error: Exception): String = when (error) {
    is FirebaseNetworkException -> "No se pudo conectar. Revisá tu conexión e intentá nuevamente."
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Esperá unos minutos."
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_WEAK_PASSWORD" -> "La contraseña no cumple los requisitos del proyecto."
        "ERROR_OPERATION_NOT_ALLOWED" -> "El acceso por email y contraseña no está habilitado en Firebase."
        "ERROR_INVALID_EMAIL" -> "Ingresá un email válido."
        else -> "No se pudo autenticar la cuenta. Revisá los datos o intentá iniciar sesión."
    }
    else -> "No se pudo completar la autenticación. Intentá nuevamente."
}
