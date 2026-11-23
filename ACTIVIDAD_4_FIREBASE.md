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

## Configuración inicial (completada en la verificación real)

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
El archivo auténtico ya fue proporcionado y se verificó Firebase real; ver la sección de verificación real al final.

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

## Resultado histórico de la primera implementación (sin JSON, 23/11/2026)

- `assembleDebug`: correcto. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- `testDebugUnitTest`: 10 pruebas aprobadas, sin errores ni omisiones.
- `connectedDebugAndroidTest`: 4 pruebas aprobadas en Medium_Phone_API_37.0,
  sin errores ni omisiones: CRUD/aislamiento, contexto y dos pruebas Compose.
- `lintDebug`: 0 errores, 38 advertencias (versiones disponibles, catálogo y estilo/recursos).
  Se corrigió el atributo `includeSubdomains` faltante en la configuración de red debug.
- Arranque del APK en API 37: correcto; muestra «Configuración Firebase pendiente» sin cierre inesperado.
- `git diff --check`: correcto.
- Firebase real: pendiente de configuración; no se afirma validación de registro/ingreso remoto.
- En esa primera implementación no se hizo push automático; posteriormente el usuario autorizó commit y push. La verificación real siguiente no incluye commit ni push.

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

## Verificación con Firebase real — 23/11/2026

### Configuración encontrada

- Rama: `actividad-4`.
- Existe `app/google-services.json`, proporcionado por el usuario; proyecto
  `recordatoriosteclab`, cliente Android `com.example.recordatoriosteclab`.
- El JSON sigue excluido de Git. No se copiaron sus claves al informe.
- Google Services 4.6.0 y Firebase BoM 34.7.0 conservados, sin cambios de versiones.
- Google Services procesó la configuración real durante la compilación.
- Email/Password fue informado como habilitado por el usuario y su funcionamiento
  quedó corroborado por registros e ingresos exitosos contra Firebase real.
- AVD comprobado: `Medium_Phone_API_37.0`, dispositivo `emulator-5554`, API 37.
- El servidor Python configurado en `http://10.0.2.2:8000/` no estaba escuchando
  durante las pruebas. La app abrió y permitió el trabajo local sin él.

### Compilación y pruebas automatizadas locales finales

- `:app:assembleDebug`: correcto, APK debug generado con configuración Firebase real.
- `:app:testDebugUnitTest`: correcto; 10 pruebas, 0 fallos.
- `:app:lintDebug`: correcto; 0 errores y 38 advertencias.
- `:app:connectedDebugAndroidTest`: correcto; 4 pruebas locales, 0 fallos,
  con las tres fases reales excluidas explícitamente de esta suite.
- Comando combinado final: `BUILD SUCCESSFUL` (12 s).
- Un intento intermedio se interrumpió y otro falló por no haber dispositivos
  conectados; se reinició el AVD y se completó la verificación satisfactoriamente.
- `git diff --check`: correcto.

### Pruebas contra Firebase real efectivamente ejecutadas

El usuario autorizó expresamente crear y eliminar dos cuentas temporales con correos
únicos `@example.com` y contraseñas aleatorias. No se usaron credenciales personales.
Las tres fases de `FirebaseRealTest` se ejecutaron con el SDK real en el emulador,
mediante instrumentación y la interfaz Compose de `MainActivity`:

1. `fase1RegistroIngresoYAislamiento`: **OK**, 13,865 s.
   - Ingreso vacío: error de email local.
   - Registro: contraseña corta y confirmación diferente rechazadas localmente.
   - Registro real de A y B desde la pantalla Compose: exitoso, con UID distintos.
   - Contraseña incorrecta de A: Firebase rechaza el ingreso y la UI muestra error.
   - Ingreso posterior correcto y cierre de sesión desde la UI: exitosos.
   - Recordatorios creados mediante el repositorio Room de cada UID y observados
     en la UI: A no muestra B, B no muestra A; A recupera sus registros al regresar.
   - Preferencia de tema de A conservada, B comienza con preferencias predeterminadas.
2. `fase2RecuperarSesionYCerrar`: **OK**, 2,517 s, después de `am force-stop`.
   - Recuperación real de la sesión persistida A desde otro proceso.
   - Recuperación de su recordatorio sin mostrar el de B.
   - Cierre de sesión desde Configuración; la recreación de la Activity vuelve al
     ingreso y no recupera la pantalla protegida ni una sesión activa.
3. `fase3Limpiar`: **OK**, 4,096 s, después de otro `am force-stop`.
   - Comprobación de que el cierre de sesión persistió entre procesos.
   - Ingreso por SDK únicamente para eliminar ambas cuentas temporales mediante
     `FirebaseUser.delete()`, con respuesta exitosa en los dos casos.
   - Vaciado de sus tablas Room y restablecimiento de sus preferencias de prueba.
   - Eliminación comprobada por la prueba del archivo privado temporal con credenciales.
   - Estado final: sin usuario autenticado y pantalla de ingreso.

Estas son pruebas automatizadas de integración **contra Firebase real**, no mocks.
La creación de recordatorios se hizo por repositorio: no equivale a probar manualmente
cada interacción del formulario CRUD. Tampoco se prueba sincronización remota de datos:
Room y DataStore siguen siendo locales; no se incorporó Firestore.

### Separación de las suites y reproducción

`connectedDebugAndroidTest` excluye `FirebaseRealTest` mediante `notClass` en Gradle.
Además, la clase exige el argumento `autorizarFirebaseReal=true`. Así, ejecutar las
pruebas normales no crea ni elimina cuentas Firebase. Para una futura ejecución real
se necesita nueva autorización explícita y ejecutar cada fase por separado, instalando
ambos APK y manteniendo sus datos entre fases:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e autorizarFirebaseReal true -e class com.example.recordatoriosteclab.FirebaseRealTest#fase1RegistroIngresoYAislamiento com.example.recordatoriosteclab.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop com.example.recordatoriosteclab
adb shell am instrument -w -e autorizarFirebaseReal true -e class com.example.recordatoriosteclab.FirebaseRealTest#fase2RecuperarSesionYCerrar com.example.recordatoriosteclab.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop com.example.recordatoriosteclab
adb shell am instrument -w -e autorizarFirebaseReal true -e class com.example.recordatoriosteclab.FirebaseRealTest#fase3Limpiar com.example.recordatoriosteclab.test/androidx.test.runner.AndroidJUnitRunner
```

No desinstalar ni ejecutar la suite Gradle conectada entre fases: podría eliminar el
archivo privado requerido para la limpieza. Ante un fallo, priorizar fase 3 y revisar
su resultado; si también falla, conservar los datos y limpiar las cuentas en Firebase
Console. No ejecutar fase 1 sobre una sesión personal o una verificación pendiente.

### Problemas encontrados y correcciones

- El informe anterior todavía describía la ausencia del JSON: se actualizó sin borrar
  la evidencia histórica de aquella primera implementación.
- El exportador XML de pruebas representó las tres exclusiones por `AssumptionViolatedException`
  como fallos, aunque Gradle terminó correctamente. Se agregó la exclusión explícita
  `notClass` para que la suite habitual ejecute solo las cuatro pruebas locales.
- La limpieza de la prueba real ahora omite cuentas ya marcadas como eliminadas, para
  poder reintentar una limpieza parcial sin intentar autenticar una cuenta borrada.
- No se detectaron errores funcionales en los flujos reales ejecutados. No se
  modificó código productivo de autenticación, Room, DataStore ni sincronización.

### Archivos de esta verificación

- Creado: `app/src/androidTest/java/com/example/recordatoriosteclab/FirebaseRealTest.kt`.
- Modificado: `app/build.gradle.kts` (exclusión explícita de pruebas reales en la suite normal).
- Modificado: `ACTIVIDAD_4_FIREBASE.md` (este informe).
- Configuración local existente: `app/google-services.json`, ignorada por Git y sin modificar.

### Pruebas manuales y casos adicionales pendientes

- Recorrido manual visual del usuario, teclado, accesibilidad y botón Atrás tras logout.
- Registro con correo duplicado y políticas de contraseña más exigentes del proyecto.
- Pérdida de conectividad durante una operación, límites de intentos y recuperación.
- Sesión revocada/usuario deshabilitado desde Console y expiración prolongada de tokens.
- CRUD completo desde las pantallas en ambas cuentas (el aislamiento se verificó
  con Room real y la visualización Compose, y el CRUD por repositorio en la suite normal).

### Estado final

Firebase Authentication está verificado para registro, ingreso válido/inválido,
cierre, persistencia entre procesos y aislamiento local entre dos UIDs reales en API 37.
Los casos adicionales anteriores siguen pendientes; no se afirma cobertura universal.
Las dos cuentas temporales se eliminaron. Sin Firestore, sin commit ni push en esta verificación.
