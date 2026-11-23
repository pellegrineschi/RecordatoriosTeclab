package com.example.recordatoriosteclab

/** Excluye suites con efectos remotos de la ejecución normal de Gradle. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PruebaFirebaseReal
