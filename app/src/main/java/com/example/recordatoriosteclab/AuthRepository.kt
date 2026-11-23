package com.example.recordatoriosteclab

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UsuarioSesion(val uid: String, val email: String?)

interface AuthRepository {
    val usuario: Flow<UsuarioSesion?>
    suspend fun ingresar(email: String, password: String)
    suspend fun registrar(email: String, password: String)
    fun salir()
}

/** Firebase conserva la sesión; la app no almacena contraseñas ni tokens. */
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override val usuario: Flow<UsuarioSesion?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser?.let { user -> UsuarioSesion(user.uid, user.email) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun ingresar(email: String, password: String) {
        esperar { auth.signInWithEmailAndPassword(email, password) }
    }

    override suspend fun registrar(email: String, password: String) {
        esperar { auth.createUserWithEmailAndPassword(email, password) }
    }

    override fun salir() = auth.signOut()

    private suspend fun esperar(operacion: () -> com.google.android.gms.tasks.Task<*>) {
        suspendCancellableCoroutine { continuation ->
            operacion().addOnCompleteListener { task ->
                if (continuation.isActive) {
                    if (task.isSuccessful) continuation.resume(Unit)
                    else continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Autenticación incompleta")
                    )
                }
            }
        }
    }
}
