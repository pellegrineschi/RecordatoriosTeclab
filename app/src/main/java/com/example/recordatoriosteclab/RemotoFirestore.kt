package com.example.recordatoriosteclab

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** El UID se fija al crear la sesión; nunca se obtiene de un formulario. */
interface RemotoRecordatorios {
    suspend fun enviar(entidad: RecordatorioEntity)
    suspend fun descargar(): List<RecordatorioEntity>
}

class ConflictoRemoto : Exception("Existe una versión remota diferente o más nueva. El cambio local sigue pendiente.")

internal fun validarEnvio(local: RecordatorioEntity, remoto: RecordatorioEntity?) {
    if (remoto != null && remoto.toDto() != local.toDto() &&
        remoto.versionLocal >= local.versionLocal) throw ConflictoRemoto()
}

class RemotoFirestore(
    private val uid: String,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : RemotoRecordatorios {
    init { require(uid.isNotBlank() && '/' !in uid) }
    private val coleccion = firestore.collection("users").document(uid).collection("reminders")
    private fun verificarSesion() { check(auth.currentUser?.uid == uid) { "La sesión cambió" } }

    override suspend fun enviar(entidad: RecordatorioEntity) {
        verificarSesion()
        val referencia = coleccion.document(entidad.claveSincronizacion)
        // Transacción de servidor: no confirma escrituras pendientes en caché sin conexión.
        firestore.runTransaction { transaction ->
            verificarSesion()
            val snapshot = transaction.get(referencia)
            val remoto = snapshot.data?.let { desdeMapa(snapshot.id, it) }
            validarEnvio(entidad, remoto)
            if (remoto?.toDto() != entidad.toDto()) transaction.set(referencia, entidad.aMapa())
            Unit
        }.esperarFirestore()
        verificarSesion()
    }

    override suspend fun descargar(): List<RecordatorioEntity> {
        verificarSesion()
        val resultado = coleccion.get(Source.SERVER).esperarFirestore()
        verificarSesion()
        return resultado.documents.map { desdeMapa(it.id, requireNotNull(it.data)) }
    }
}

internal fun RecordatorioEntity.aMapa(): Map<String, Any?> = mapOf(
    "claveSincronizacion" to claveSincronizacion, "titulo" to titulo,
    "descripcion" to descripcion, "categoria" to categoria, "prioridad" to prioridad,
    "estado" to estado, "fechaCreacion" to fechaCreacion, "fechaModificacion" to fechaModificacion,
    "ubicacion" to ubicacion, "aviso" to aviso, "versionLocal" to versionLocal, "eliminado" to eliminado
)

internal fun desdeMapa(id: String, datos: Map<String, Any?>): RecordatorioEntity {
    require(id == datos["claveSincronizacion"])
    for (campo in listOf("descripcion", "ubicacion", "aviso")) {
        require(datos.containsKey(campo) && (datos[campo] == null || datos[campo] is String))
    }
    return RecordatorioDto(
        id, datos["titulo"] as? String, datos["descripcion"] as? String,
        datos["categoria"] as? String, datos["prioridad"] as? String, datos["estado"] as? String,
        datos["fechaCreacion"] as? Long, datos["fechaModificacion"] as? Long,
        datos["ubicacion"] as? String, datos["aviso"] as? String,
        datos["versionLocal"] as? Long, datos["eliminado"] as? Boolean
    ).aEntidadRemota()
}

private suspend fun <T> Task<T>.esperarFirestore(): T = suspendCancellableCoroutine { c ->
    addOnCompleteListener { task ->
        if (c.isActive) {
            if (task.isSuccessful) c.resume(task.result)
            else c.resumeWithException(task.exception ?: IllegalStateException("Firestore no confirmó la operación"))
        }
    }
}
