# Actividad 4 — primera etapa: Firebase Authentication

## Alcance y versiones

Trabajo sobre `actividad-4`. Se conserva Gradle 9.6.0, AGP 9.4.1,
plugin Compose/Kotlin 2.2.10, KSP 2.3.10, compileSdk/targetSdk 37 y minSdk 29.
El daemon utiliza JDK 25. Para ejecutar las pruebas en API 37 se actualizaron
AndroidX Test JUnit a 1.3.0 y Espresso a 3.7.0 (la versión 3.5.1 fallaba al buscar
InputManager.getInstance). Room, DataStore, Navigation y Retrofit conservan sus versiones.

Se agrega Google Services 4.6.0 y Firebase BoM 34.7.0 (`firebase-auth`, sin módulo KTX).
El BoM 35.2.0 fue descartado: Auth 25 contiene metadatos Kotlin 2.4 incompatibles
con el compilador actual. No se cambió el toolchain para resolverlo.

## Configuración manual obligatoria

1. Crear o seleccionar el proyecto correcto en https://console.firebase.google.com/.
2. Registrar una aplicación Android con paquete **com.example.recordatoriosteclab**.
3. Descargar su archivo auténtico `google-services.json` y colocarlo exactamente en:
   `/home/matias/AndroidStudioProjects/RecordatoriosTeclab/app/google-services.json`.
4. En Authentication → Sign-in method (Método de acceso), habilitar **Email/Password**.
   No hace falta habilitar el acceso por vínculo de correo ni configurar SHA para este proveedor.
5. Sincronizar Gradle y recompilar. Google Services se aplica cuando existe el archivo
   en la raíz del módulo `app`; esta etapa no configura variantes de Firebase.
6. Probar registro, cierre e ingreso con una cuenta de prueba propia.

No se creó configuración ficticia, cuenta Firebase ni clave privada. El JSON está
excluido de Git. No agregar archivos de cuentas de servicio ni credenciales Admin SDK.
Sin el JSON se puede compilar y abrir la aplicación, pero aparece una explicación de
configuración pendiente y no se permite acceder a datos ni simular autenticación.
La configuración y autenticación contra Firebase real quedan pendientes del usuario.

## Arquitectura y datos

- `AuthRepository` define el contrato; `FirebaseAuthRepository` encapsula Firebase y
  publica el usuario mediante AuthStateListener. Firebase conserva la sesión local.
- `AuthViewModel` expone StateFlow, valida formularios y gestiona carga, errores y logout.
- `PantallaAutenticacion` presenta ingreso y registro con Navigation Compose.
- `AplicacionAutenticada` protege el flujo existente y elimina los ViewModels y la
  navegación de una sesión al salir. Las contraseñas no se guardan en SavedState,
  Room, DataStore ni logs.
- Room usa `recordatorios_<SHA-256 del UID>.db`; DataStore utiliza claves con el
  mismo identificador. Cada cuenta conserva su CRUD, tema, filtros y orden.
- La base antigua `recordatorios.db` y las preferencias antiguas se conservan intactas.
  No se muestran ni asignan al primer usuario: no existe evidencia de quién es su dueño.
  Una migración/importación futura requiere definir explícitamente esa propiedad.
- Los datos nuevos son locales: Firebase Authentication no sincroniza recordatorios.
- El servidor Python anterior expone datos globales sin UID. Su código se conserva,
  pero no se conecta al repositorio de las sesiones autenticadas. El botón Sincronizar
  explica el bloqueo y los cambios siguen guardándose localmente. Habilitar ese servidor
  requeriría validación de tokens y autorización por UID en el backend, fuera de esta etapa.
- El arranque y el CRUD no dependen del servidor Python. Registro/ingreso requieren
  conexión con Firebase; una sesión ya persistida permite trabajar localmente sin Python.

## Verificación reproducible

Con el JDK configurado (en este equipo, `JAVA_HOME=/home/matias/Aplicaciones/android-studio/jbr`):

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

La segunda orden requiere un emulador o dispositivo. Las pruebas locales usan un
repositorio de autenticación falso; las instrumentadas verifican Room/DataStore reales
y el formulario Compose. No sustituyen la prueba de integración con Firebase real.

Después de configurar Firebase:

1. Registrar A, crear y editar un recordatorio, cambiar tema/filtros y reiniciar la app.
2. Confirmar recuperación de A y sus datos; cerrar sesión y pulsar Atrás.
3. Registrar B: no debe ver datos ni preferencias de A, incluso si coincide un ID local.
4. Volver a A y comprobar que conserva datos y preferencias.
5. Probar contraseña incorrecta, correo duplicado, falta de red y política de contraseña.
6. Reiniciar con sesión activa y servidor Python apagado; comprobar CRUD local.

Referencias oficiales:
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/support/release-notes/android

## Resultado de verificación (23/11/2026)

- `assembleDebug`: correcto. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- `testDebugUnitTest`: 10 pruebas aprobadas, sin errores ni omisiones.
- `connectedDebugAndroidTest`: 4 pruebas aprobadas en Medium_Phone_API_37.0,
  sin errores ni omisiones: CRUD/aislamiento, contexto y dos pruebas Compose.
- `lintDebug`: 0 errores, 38 advertencias (versiones disponibles, catálogo y estilo/recursos).
  Se corrigió el atributo `includeSubdomains` faltante en la configuración de red debug.
- Arranque del APK en API 37: correcto; muestra «Configuración Firebase pendiente» sin cierre inesperado.
- `git diff --check`: correcto.
- Firebase real: pendiente de configuración; no se afirma validación de registro/ingreso remoto.
- No se hizo push.

## Inventario de archivos

Creados:

- `ACTIVIDAD_4_FIREBASE.md`
- `app/src/main/java/com/example/recordatoriosteclab/AuthRepository.kt`
- `app/src/main/java/com/example/recordatoriosteclab/AuthViewModel.kt`
- `app/src/main/java/com/example/recordatoriosteclab/PantallaAutenticacion.kt`
- `app/src/main/java/com/example/recordatoriosteclab/AplicacionAutenticada.kt`
- `app/src/main/java/com/example/recordatoriosteclab/IdentidadLocal.kt`
- `app/src/test/java/com/example/recordatoriosteclab/AuthViewModelTest.kt`
- `app/src/androidTest/java/com/example/recordatoriosteclab/AislamientoCuentaTest.kt`
- `app/src/androidTest/java/com/example/recordatoriosteclab/PantallaAutenticacionTest.kt`

Modificados:

- `.gitignore`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/com/example/recordatoriosteclab/MainActivity.kt`
- `app/src/main/java/com/example/recordatoriosteclab/RecordatoriosDatabase.kt`
- `app/src/main/java/com/example/recordatoriosteclab/RecordatoriosRepositoryRoom.kt`
- `app/src/main/java/com/example/recordatoriosteclab/PreferenciasRepository.kt`
- `app/src/debug/res/xml/network_security_config.xml`
