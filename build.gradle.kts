
plugins {
    id("com.google.gms.google-services") version "4.6.0" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // KSP - Actividad 3
    id("com.google.devtools.ksp") version "2.3.10" apply false
}
