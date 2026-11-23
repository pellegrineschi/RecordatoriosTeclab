
package com.example.recordatoriosteclab

data class FormularioUiState(

    val titulo: String = "",

    val descripcion: String = "",

    val categoria: Categoria? = null,

    val prioridad: Prioridad? = null,

    val guardando: Boolean = false,

    val guardadoExitoso: Boolean = false,

    val errorGeneral: String? = null,

    val idEdicion: Long? = null

) {

    val tituloValido: Boolean
        get() = titulo.trim().length >= 4

    val descripcionValida: Boolean
        get() = descripcion.length <= 200

    val categoriaValida: Boolean
        get() = categoria != null &&
                categoria in DatosSimulados.categorias

    val prioridadValida: Boolean
        get() = prioridad != null

    val puedeGuardar: Boolean
        get() = tituloValido &&
                descripcionValida &&
                categoriaValida &&
                prioridadValida &&
                !guardando
}
