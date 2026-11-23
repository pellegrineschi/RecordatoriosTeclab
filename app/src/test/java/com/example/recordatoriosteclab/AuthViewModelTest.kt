package com.example.recordatoriosteclab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun finalizar() { Dispatchers.resetMain() }

    @Test fun recuperaSesionYCierraSinConservarUsuario() = runTest(dispatcher) {
        val repo = FakeAuth(UsuarioSesion("A", "a@example.com"))
        val vm = AuthViewModel(repo)
        runCurrent()
        assertEquals("A", vm.estado.value.usuario?.uid)
        assertFalse(vm.estado.value.iniciando)
        vm.salir()
        runCurrent()
        assertNull(vm.estado.value.usuario)
    }

    @Test fun validaRegistroSinEnviarDatosInvalidos() = runTest(dispatcher) {
        val repo = FakeAuth()
        val vm = AuthViewModel(repo)
        runCurrent()
        vm.autenticar("a@example.com", "123456", true, "distinta")
        runCurrent()
        assertNotNull(vm.estado.value.error)
        assertEquals(0, repo.llamadas)
    }

    @Test fun evitaEnviosDuplicadosYMuestraCarga() = runTest(dispatcher) {
        val repo = FakeAuth()
        repo.espera = CompletableDeferred()
        val vm = AuthViewModel(repo)
        runCurrent()
        vm.autenticar(" a@example.com ", "123456", false, "")
        vm.autenticar("a@example.com", "123456", false, "")
        runCurrent()
        assertTrue(vm.estado.value.cargando)
        assertEquals(1, repo.llamadas)
        assertEquals("a@example.com", repo.emailRecibido)
        repo.espera!!.complete(Unit)
        runCurrent()
        assertFalse(vm.estado.value.cargando)
        assertEquals("nueva", vm.estado.value.usuario?.uid)
    }

    @Test fun errorPermiteReintentar() = runTest(dispatcher) {
        val repo = FakeAuth()
        repo.fallar = true
        val vm = AuthViewModel(repo)
        runCurrent()
        vm.autenticar("a@example.com", "123456", false, "")
        runCurrent()
        assertNotNull(vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        assertNull(vm.estado.value.usuario)
        repo.fallar = false
        vm.autenticar("a@example.com", "123456", true, "123456")
        runCurrent()
        assertNull(vm.estado.value.error)
        assertEquals("nueva", vm.estado.value.usuario?.uid)
        assertTrue(repo.registro)
    }

    @Test fun clavesLocalesSonEstablesYAisladas() {
        assertEquals(claveCuenta("A"), claveCuenta("A"))
        assertNotEquals(claveCuenta("A"), claveCuenta("B"))
        assertTrue(claveCuenta("../usuario").matches(Regex("[a-f0-9]{64}")))
    }

    @Test fun noRecortaPasswordNiAplicaPoliticaDeRegistroAlIngreso() {
        assertNull(validarCredenciales("a@example.com", " x ", false, ""))
        assertNotNull(validarCredenciales("a@example.com", " x ", true, " x "))
        assertNotNull(validarCredenciales("incorrecto", "123456", false, ""))
    }

    private class FakeAuth(inicial: UsuarioSesion? = null) : AuthRepository {
        override val usuario = MutableStateFlow(inicial)
        var llamadas = 0
        var registro = false
        var fallar = false
        var emailRecibido = ""
        var espera: CompletableDeferred<Unit>? = null
        override suspend fun ingresar(email: String, password: String) {
            llamadas++
            emailRecibido = email
            espera?.await()
            if (fallar) error("error de prueba")
            usuario.value = UsuarioSesion("nueva", email)
        }
        override suspend fun registrar(email: String, password: String) {
            registro = true
            ingresar(email, password)
        }
        override fun salir() { usuario.value = null }
    }
}
