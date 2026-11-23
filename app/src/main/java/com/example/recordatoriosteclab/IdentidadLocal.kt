package com.example.recordatoriosteclab

import java.security.MessageDigest

/** Nombre estable y seguro para archivos/claves; no guarda tokens ni contraseñas. */
fun claveCuenta(uid: String): String {
    require(uid.isNotBlank())
    return MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
