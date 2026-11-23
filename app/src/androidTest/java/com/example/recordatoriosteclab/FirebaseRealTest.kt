package com.example.recordatoriosteclab

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Prueba REAL, excluida por defecto: requiere -e autorizarFirebaseReal true.
 * Ejecutar las tres fases por separado con am instrument y force-stop entre ellas.
 * No desinstalar entre fases: el archivo privado temporal permite limpiar las cuentas.
 * Nunca usar credenciales personales. Ver ACTIVIDAD_4_FIREBASE.md.
 */
class FirebaseRealTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val autorizacion = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                assumeTrue("Requiere autorización explícita para Firebase real",
                    InstrumentationRegistry.getArguments().getString("autorizarFirebaseReal") == "true")
                base.evaluate()
            }
        }
    }
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(autorizacion).around(compose)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth get() = FirebaseAuth.getInstance()
    private val archivo get() = File(context.filesDir, "firebase_verificacion_temporal.json")
    private val tituloA = "Recordatorio privado de prueba A"
    private val tituloB = "Recordatorio privado de prueba B"

    private fun esperar(texto: String) {
        compose.waitUntil(60_000) {
            compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun tocar(texto: String) = compose.onNodeWithText(texto).performClick()
    private fun campo(etiqueta: String, valor: String) {
        compose.onNodeWithText(etiqueta).performTextReplacement(valor)
    }
    private fun salir() {
        esperar("Cerrar sesión")
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Cerrar sesión") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        tocar("Cerrar sesión")
        esperar("Iniciar sesión")
        assertNull(auth.currentUser)
        compose.onNodeWithText(tituloA).assertDoesNotExist()
        compose.onNodeWithText(tituloB).assertDoesNotExist()
    }
    private fun ingresar(cuenta: JSONObject, password: String = cuenta.getString("password")) {
        campo("Email", cuenta.getString("email"))
        campo("Contraseña", password)
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
    }
    private fun registrar(cuenta: JSONObject, cuentas: JSONArray) {
        campo("Email", cuenta.getString("email"))
        campo("Contraseña", cuenta.getString("password"))
        campo("Confirmar contraseña", cuenta.getString("password"))
        compose.onNodeWithText("Registrarme").performScrollTo().performClick()
        esperar("Cerrar sesión")
        val usuario = requireNotNull(auth.currentUser)
        assertEquals(cuenta.getString("email"), usuario.email)
        cuenta.put("uid", usuario.uid)
        archivo.writeText(cuentas.toString())
    }

    @Test fun fase1RegistroIngresoYAislamiento() {
        assertNull("No modificar una sesión existente", auth.currentUser)
        check(!archivo.exists()) { "Hay una verificación pendiente: ejecutar fase3Limpiar primero" }
        val cuentas = JSONArray()
        repeat(2) {
            cuentas.put(JSONObject().put("email", "teclab-${UUID.randomUUID()}@example.com")
                .put("password", "Aa1!${UUID.randomUUID()}"))
        }
        archivo.writeText(cuentas.toString()) // Solo sandbox privado del emulador; borrado en fase 3.
        val a = cuentas.getJSONObject(0)
        val b = cuentas.getJSONObject(1)
        esperar("Iniciar sesión")
        tocar("Ingresar")
        esperar("Ingresá un email válido.")
        tocar("Crear una cuenta")
        esperar("Crear cuenta")
        campo("Email", a.getString("email"))
        campo("Contraseña", "123")
        campo("Confirmar contraseña", "123")
        compose.onNodeWithText("Registrarme").performScrollTo().performClick()
        esperar("La contraseña debe tener al menos 6 caracteres.")
        campo("Contraseña", a.getString("password"))
        compose.onNodeWithText("Registrarme").performScrollTo().performClick()
        esperar("Las contraseñas no coinciden.")
        registrar(a, cuentas)
        val uidA = a.getString("uid")
        runBlocking {
            RecordatoriosRepositoryRoom(RecordatoriosDatabase.obtenerBaseDeDatos(context, uidA))
                .crear(tituloA, null, DatosSimulados.categorias.first(), Prioridad.Alta)
            PreferenciasRepository(context, uidA).cambiarTemaOscuro(true)
        }
        esperar(tituloA)
        salir()
        ingresar(a, "Incorrecta1!${UUID.randomUUID()}")
        esperar("No se pudo autenticar la cuenta. Revisá los datos o intentá iniciar sesión.")
        assertNull(auth.currentUser)
        ingresar(a)
        esperar(tituloA)
        salir()
        tocar("Crear una cuenta")
        registrar(b, cuentas)
        esperar("Sin recordatorios")
        compose.onNodeWithText(tituloA).assertDoesNotExist()
        val uidB = b.getString("uid")
        assertNotEquals(uidA, uidB)
        runBlocking {
            val repo = RecordatoriosRepositoryRoom(RecordatoriosDatabase.obtenerBaseDeDatos(context, uidB))
            assertTrue(repo.obtenerTodos().isEmpty())
            assertFalse(PreferenciasRepository(context, uidB).preferencias.first().temaOscuro)
            repo.crear(tituloB, null, DatosSimulados.categorias.first(), Prioridad.Media)
        }
        esperar(tituloB)
        compose.onNodeWithText(tituloA).assertDoesNotExist()
        salir()
        ingresar(a)
        esperar(tituloA)
        compose.onNodeWithText(tituloB).assertDoesNotExist()
        runBlocking { assertTrue(PreferenciasRepository(context, uidA).preferencias.first().temaOscuro) }
        // No cerrar sesión: la siguiente fase debe recuperarla desde OTRO proceso.
    }

    @Test fun fase2RecuperarSesionYCerrar() {
        val a = JSONArray(archivo.readText()).getJSONObject(0)
        esperar(tituloA)
        assertEquals(a.getString("uid"), auth.currentUser?.uid)
        compose.onNodeWithText(tituloB).assertDoesNotExist()
        tocar("Configuración")
        esperar("Apariencia")
        salir()
        // Logout debe descartar también el destino protegido de configuración.
        compose.activityRule.scenario.recreate()
        esperar("Iniciar sesión")
        compose.onNodeWithText("Apariencia").assertDoesNotExist()
        assertNull(auth.currentUser)
    }

    @Test fun fase3Limpiar() {
        // También sirve para recuperar/limpiar una ejecución parcial.
        val cuentas = JSONArray(archivo.readText())
        val sesionCerrada = auth.currentUser == null
        for (i in 0 until cuentas.length()) {
            val cuenta = cuentas.getJSONObject(i)
            if (cuenta.optBoolean("eliminada")) continue
            try {
                val resultado = Tasks.await(auth.signInWithEmailAndPassword(
                    cuenta.getString("email"), cuenta.getString("password")), 60, TimeUnit.SECONDS)
                val usuario = requireNotNull(resultado.user)
                val uid = usuario.uid
                Tasks.await(usuario.delete(), 60, TimeUnit.SECONDS)
                runBlocking {
                    val db = RecordatoriosDatabase.obtenerBaseDeDatos(context, uid)
                    db.clearAllTables()
                    // Restablecer solo preferencias de las cuentas temporales.
                    PreferenciasRepository(context, uid).apply {
                        cambiarTemaOscuro(false)
                        cambiarMostrarCompletados(true)
                        cambiarOrdenListado(OrdenListado.RECIENTES)
                    }
                }
                cuenta.put("eliminada", true)
                archivo.writeText(cuentas.toString())
            } catch (e: Exception) {
                // No ocultar errores: conservar el archivo para reintentar la limpieza.
                throw AssertionError("No se pudo limpiar la cuenta temporal ${i + 1}", e)
            }
        }
        auth.signOut()
        check(archivo.delete())
        esperar("Iniciar sesión")
        assertTrue("El cierre de sesión debe persistir entre procesos", sesionCerrada)
    }
}
