package com.example.recordatoriosteclab

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Opt-in; no Activity ni sincronización automática de sesiones personales.
 * Deja un inventario privado para limpiar SOLO sus cuentas después de borrar
 * sus dos documentos en Console. Nunca ejecutar pruebas reales en la suite normal.
 */
@PruebaFirebaseReal
class FirestoreRealTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val archivo get() = File(context.filesDir, "firestore_prueba_20261123.json")
    private val idA = "b52ebf65-d798-4c62-80a1-f01c35cb05ac"
    private val idB = "cd0bc24e-a65c-4604-9ec3-2d322bb84ffb"
    private fun <T> esperar(task: Task<T>): T = Tasks.await(task, 60, TimeUnit.SECONDS)
    private fun autorizar() = assumeTrue("Requiere autorización explícita de datos temporales",
        InstrumentationRegistry.getArguments().getString("autorizarFirestoreReal") == "true")
    private fun app(): FirebaseApp = FirebaseApp.initializeApp(context,
        FirebaseApp.getInstance().options, "prueba-firestore-${UUID.randomUUID()}")
    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
    private fun base() = Room.inMemoryDatabaseBuilder(context, RecordatoriosDatabase::class.java).build()
    private fun denegado(operacion: () -> Unit) {
        try { operacion(); fail("Operación indebidamente permitida") }
        catch (e: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                (e.cause as? FirebaseFirestoreException)?.code)
        }
    }

    @Test fun sincronizacionReal() = runBlocking {
        autorizar()
        val reanudar = InstrumentationRegistry.getArguments().getString("reanudarFirestoreReal") == "true"
        check(!archivo.exists() || reanudar) { "Existe un inventario pendiente: no crear más cuentas" }
        val app = app()
        val auth = FirebaseAuth.getInstance(app)
        val store = FirebaseFirestore.getInstance(app)
        val cuentas = if (archivo.exists()) JSONArray(archivo.readText()) else JSONArray()
        if (cuentas.length() == 0) listOf("a", "b").forEach { letra -> cuentas.put(JSONObject()
            .put("email", "teclab-fs-$letra-20261123-2126@example.com")
            .put("password", "Aa1!${UUID.randomUUID()}")
            .put("documento", if (letra == "a") idA else idB)) }
        archivo.writeText(cuentas.toString())
        val dbA = base()
        val dbB = base()
        val recuperada = base()
        try {
            for (i in 0..1) {
                val c = cuentas.getJSONObject(i)
                if (c.has("uid")) continue
                val u = esperar(auth.createUserWithEmailAndPassword(c.getString("email"), c.getString("password"))).user!!
                c.put("uid", u.uid)
                archivo.writeText(cuentas.toString())
                auth.signOut()
            }
            val a = cuentas.getJSONObject(0)
            val b = cuentas.getJSONObject(1)
            val uidA = a.getString("uid")
            val uidB = b.getString("uid")
            val docA = store.collection("users").document(uidA).collection("reminders").document(idA)
            val docB = store.collection("users").document(uidB).collection("reminders").document(idB)
            denegado { esperar(docA.get(Source.SERVER)) }
            esperar(auth.signInWithEmailAndPassword(a.getString("email"), a.getString("password")))
            val remotoA = RemotoFirestore(uidA, auth, store)
            val syncA = SincronizadorFirestore(dbA, remotoA)
            val entidad = RecordatorioEntity(titulo = "TEMP Teclab Firestore A 20261123",
                categoria = "Estudio", prioridad = "ALTA", estado = "PENDIENTE", claveSincronizacion = idA)
            // Reanudar una prueba interrumpida sobre los mismos documentos, sin duplicar.
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            val localId = dbA.recordatorioDao().obtenerPorClave(idA)?.id
                ?: dbA.recordatorioDao().insertar(entidad)
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals(dbA.recordatorioDao().obtenerPorId(localId)!!.titulo, esperar(docA.get(Source.SERVER)).getString("titulo"))
            val repoA = RecordatoriosRepositoryRoom(dbA, remoto = remotoA)
            repoA.actualizar(repoA.obtenerPorId(localId)!!.copy(titulo = "TEMP Teclab A editado"))
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals("TEMP Teclab A editado", esperar(docA.get(Source.SERVER)).getString("titulo"))
            val wifi = shell("settings get global wifi_on").trim()
            val datos = shell("settings get global mobile_data").trim()
            try {
            shell("svc wifi disable")
            shell("svc data disable")
            val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
            val limite = android.os.SystemClock.elapsedRealtime() + 15_000
            while (connectivity.activeNetwork != null && android.os.SystemClock.elapsedRealtime() < limite)
                android.os.SystemClock.sleep(200)
            check(connectivity.activeNetwork == null) { "El emulador todavía tiene red activa" }
            repoA.actualizar(repoA.obtenerPorId(localId)!!.copy(titulo = "TEMP Teclab A sin conexion"))
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Error)
            assertEquals(1, dbA.recordatorioDao().obtenerPendientes().size)
            } finally {
                if (wifi == "1") shell("svc wifi enable")
                if (datos == "1") shell("svc data enable")
            }
            val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
            val limite = android.os.SystemClock.elapsedRealtime() + 20_000
            while (connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) != true &&
                android.os.SystemClock.elapsedRealtime() < limite) android.os.SystemClock.sleep(200)
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals(1, esperar(docA.parent.get(Source.SERVER)).size())
            // Base Room nueva + descarga SERVER: recuperación sin datos locales previos.
            val syncNueva = SincronizadorFirestore(recuperada, remotoA)
            assertTrue(syncNueva.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals("TEMP Teclab A sin conexion", recuperada.recordatorioDao().obtenerTodos().single().titulo)
            denegado { esperar(docA.update("categoria", "Invalida", "versionLocal", 4L)) }
            denegado { esperar(docA.delete()) }
            repoA.eliminar(localId)
            assertTrue(syncA.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals(true, esperar(docA.get(Source.SERVER)).getBoolean("eliminado"))
            assertTrue(syncNueva.sincronizar() is ResultadoSincronizacion.Exito)
            assertTrue(recuperada.recordatorioDao().obtenerTodos().isEmpty())
            auth.signOut()
            esperar(auth.signInWithEmailAndPassword(b.getString("email"), b.getString("password")))
            denegado { esperar(docA.get(Source.SERVER)) }
            denegado { esperar(docA.set(entidad.aMapa())) }
            val syncB = SincronizadorFirestore(dbB, RemotoFirestore(uidB, auth, store))
            assertTrue(syncB.sincronizar() is ResultadoSincronizacion.Exito)
            assertTrue(dbB.recordatorioDao().obtenerTodos().isEmpty())
            dbB.recordatorioDao().insertar(entidad.copy(claveSincronizacion = idB,
                titulo = "TEMP Teclab Firestore B 20261123"))
            assertTrue(syncB.sincronizar() is ResultadoSincronizacion.Exito)
            assertEquals(1, esperar(docB.parent.get(Source.SERVER)).size())
            assertEquals(1, dbB.recordatorioDao().obtenerTodos().size)
            cuentas.getJSONObject(0).put("pruebaAprobada", true)
            archivo.writeText(cuentas.toString())
        } finally {
            esperar(store.enableNetwork())
            auth.signOut()
            dbA.close(); dbB.close(); recuperada.close()
            esperar(store.terminate()); app.delete()
        }
    }

    @Test fun recuperarTrasReinstalacion() = runBlocking {
        autorizar()
        val b = JSONArray(archivo.readText()).getJSONObject(1)
        val app = app()
        val auth = FirebaseAuth.getInstance(app)
        val store = FirebaseFirestore.getInstance(app)
        val db = RecordatoriosDatabase.obtenerBaseDeDatos(context, b.getString("uid"))
        try {
            assertTrue(db.recordatorioDao().obtenerTodos().isEmpty())
            esperar(auth.signInWithEmailAndPassword(b.getString("email"), b.getString("password")))
            assertTrue(SincronizadorFirestore(db, RemotoFirestore(b.getString("uid"), auth, store))
                .sincronizar() is ResultadoSincronizacion.Exito)
            val restaurado = db.recordatorioDao().obtenerTodos().single()
            assertEquals(idB, restaurado.claveSincronizacion)
            assertEquals("TEMP Teclab Firestore B 20261123", restaurado.titulo)
            assertFalse(restaurado.eliminado)
        } finally { auth.signOut(); db.close(); esperar(store.terminate()); app.delete() }
    }

    @Test fun limpiarCuentasDespuesDeConsole() {
        autorizar()
        val cuentas = JSONArray(archivo.readText())
        val app = app()
        val auth = FirebaseAuth.getInstance(app)
        val store = FirebaseFirestore.getInstance(app)
        try {
            for (i in 0 until cuentas.length()) {
                val c = cuentas.getJSONObject(i)
                if (c.optBoolean("eliminada")) continue
                val u = esperar(auth.signInWithEmailAndPassword(c.getString("email"), c.getString("password"))).user!!
                check(u.uid == c.getString("uid"))
                val col = store.collection("users").document(u.uid).collection("reminders")
                check(esperar(col.get(Source.SERVER)).isEmpty) { "Primero eliminar SOLO los documentos temporales en Console" }
                esperar(u.delete())
                c.put("eliminada", true)
                archivo.writeText(cuentas.toString())
            }
            check(archivo.delete())
        } finally { auth.signOut(); esperar(store.terminate()); app.delete() }
    }
}
