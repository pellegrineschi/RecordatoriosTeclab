
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Activamos KSP en el módulo app
    id("com.google.devtools.ksp")
}

// Sin configuración real se permite compilar, pero el acceso queda bloqueado.
if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("Firebase pendiente: colocar la configuración real en app/google-services.json")
}

android {
    namespace = "com.example.recordatoriosteclab"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.recordatoriosteclab"
        minSdk = 29
        targetSdk = 37
        buildConfigField("boolean", "FIRESTORE_SYNC_ENABLED",
            providers.gradleProperty("firestoreSyncEnabled").orElse("false").get())
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
        // Las pruebas reales crean cuentas: solo se ejecutan aparte y con autorización.
        testInstrumentationRunnerArguments["notAnnotation"] =
            "com.example.recordatoriosteclab.PruebaFirebaseReal"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compatible con Kotlin 2.2 del proyecto (Auth 25 requiere metadatos 2.4).
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // ======================================
// ACTIVIDAD 3 - RETROFIT Y OKHTTP
// ======================================

    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ======================================
    // ACTIVIDAD 3 - DATASTORE
    // ======================================

    implementation("androidx.datastore:datastore-preferences:1.2.1")


    // ======================================
    // ACTIVIDAD 3 - ROOM
    // ======================================

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")

    ksp("androidx.room:room-compiler:2.8.5")


    // ======================================
    // ACTIVIDAD 2 - NAVEGACIÓN
    // ======================================

    implementation("androidx.navigation:navigation-compose:2.10.2")


    // ======================================
    // VIEWMODEL Y CORRUTINAS
    // ======================================

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")


    // ======================================
    // DEPENDENCIAS ORIGINALES
    // ======================================

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    // ======================================
    // PRUEBAS
    // ======================================

    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(libs.androidx.junit)

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}