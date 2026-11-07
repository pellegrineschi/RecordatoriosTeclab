package com.example.recordatoriosteclab



// Representa los datos y el estado del formulario
data class FormularioUiState(

    // Texto ingresado por el usuario
    val titulo: String = "",
    val descripcion: String = "",

    // Opciones seleccionadas
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,

    // Estado del guardado
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val errorGeneral: String? = null

) {
    // El título necesita al menos cuatro caracteres
    val tituloValido: Boolean
        get() = titulo.trim().length >= 4

    // La descripción no puede superar 200 caracteres
    val descripcionValida: Boolean
        get() = descripcion.length <= 200

    // La categoría debe ser una opción permitida
    val categoriaValida: Boolean
        get() = categoria != null &&
                categoria in DatosSimulados.categorias

    // Es obligatorio seleccionar una prioridad
    val prioridadValida: Boolean
        get() = prioridad != null

    // Solo permitimos guardar formularios válidos
    val puedeGuardar: Boolean
        get() = tituloValido &&
                descripcionValida &&
                categoriaValida &&
                prioridadValida &&
                !guardando
}
