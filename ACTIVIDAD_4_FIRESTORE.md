# Actividad 4 — Cloud Firestore y Room

**Actualización 23/11/2026:** base y reglas publicadas comprobadas en Console;
pruebas de seguridad locales y sincronización contra Firebase real ejecutadas.
Ver «Verificación de reglas y sincronización real» al final para resultados y límites.
Las secciones de preparación describen el estado anterior a esta verificación.

## Arquitectura implementada

Rama `actividad-4`. Se leyó el informe de Authentication y se conservaron sus funciones.
Compose → RecordatoriosViewModel → RecordatoriosRepositoryRoom → Room / RemotoFirestore.
La interfaz observa únicamente Room. Cada UID conserva su archivo Room y sus claves
DataStore existentes. No se cambió el esquema Room ni se borraron datos anteriores.

Se agregó `firebase-firestore` bajo el BoM compatible 34.7.0; no se actualizaron Kotlin,
Gradle, Google Services, Authentication, Room ni las demás dependencias.

El UUID `claveSincronizacion` es el ID estable del documento:
`users/{uid}/reminders/{claveSincronizacion}`. El UID proviene de Authentication,
queda fijado al crear el repositorio y se verifica antes/después de operaciones remotas.
No se utiliza `add()` ni IDs remotos autogenerados. El Long autoincremental sigue siendo local.

### Escritura, confirmación y descarga

- Crear/editar/eliminar escribe primero en Room con estado PENDIENTE y versión local.
- La eliminación lógica envía `eliminado=true`. Los tombstones no se purgan ni se
  elimina el documento: permiten que otros dispositivos reciban el borrado.
- Una transacción Firestore lee la versión antes de escribir. Repetir exactamente
  el mismo contenido es un éxito idempotente. Una versión remota mayor, o igual
  con contenido diferente, produce conflicto y conserva la operación local pendiente.
- La confirmación cambia a SINCRONIZADO solo si el contenido local sigue siendo el enviado.
  Una edición durante el envío conserva su estado pendiente.
- Tras un error de envío se continúa con otros registros y con la descarga.
- La descarga usa `Source.SERVER`. Se valida identidad, tipos y valores antes de
  aplicar datos. Un registro pendiente/ERROR nunca se sustituye por una descarga.
  Solo se actualizan registros confirmados con versiones remotas estrictamente superiores.
- Todo cambio de estado/merge local se hace dentro de transacciones Room.
- No se considera una escritura en caché como confirmación del servidor. Las transacciones
  requieren conexión; al fallar, Room conserva los cambios para reintentar.
- Un Mutex evita dos sincronizaciones simultáneas dentro del mismo repositorio.

### Recuperación y límites explícitos

Con Firestore habilitado, se intenta al iniciar la sesión, manualmente mediante
Sincronizar y cada 30 segundos mientras vive su ViewModel. Cada operación remota tiene
20 segundos de espera. Logout elimina el ViewModel y cancela sus coroutines. Si muere
el proceso, la cola persiste en Room y se retoma al volver a abrir la sesión.
No se implementó WorkManager: no se promete sincronizar con la aplicación terminada.
Una tarea SDK ya enviada puede terminar después de cancelarse la espera; su destino
sigue siendo el UID original y el reintento es idempotente.

Las versiones son contadores por documento. Ediciones simultáneas de dos dispositivos
con igual versión y distinto contenido quedan en conflicto, sin resolución silenciosa.
Se informa error; no hay todavía una pantalla para comparar/resolver esos conflictos.
Una futura resolución explícita debe conservar ambos contenidos y producir una versión
superior a las dos. No es un algoritmo de mezcla de campos ni de reloj global.

El código Retrofit/Python histórico se conserva, pero las sesiones autenticadas solo
pueden recibir el remoto Firestore. El backend Python no se instancia en ese flujo.

## Acceso real desactivado por defecto

`BuildConfig.FIRESTORE_SYNC_ENABLED` es false salvo compilación explícita con
`-PfirestoreSyncEnabled=true`. Así no se crean/modifican/eliminan datos reales durante
esta implementación ni durante las pruebas normales. El botón explica la configuración
pendiente; el CRUD local y Authentication siguen funcionando.

No habilitar este parámetro hasta revisar/publicar reglas y autorizar el uso de datos reales.
No se reutilizó la autorización anterior para cuentas temporales de Authentication.

## Reglas propuestas (NO publicadas ni confirmadas activas)

Archivo listo para revisión: `firestore.rules`.

- Exige autenticación y coincidencia exacta entre `request.auth.uid` y UID de la ruta.
- Solo permite lectura/creación/actualización bajo la colección propia `reminders`.
- Valida campos permitidos, tipos, UUID, categoría, estado, prioridad y versión.
- Mantiene fecha de creación y requiere incremento de versión o contenido idéntico.
- Prohíbe borrado físico; el cliente envía eliminación lógica.
- Ninguna otra ruta obtiene permisos. No existen reglas públicas globales.

No se ejecutó un emulador de reglas ni una prueba contra reglas desplegadas. La revisión
estática y los dobles NO demuestran que las reglas estén activas en Firebase.

## Configuración manual pendiente

1. Firebase Console → proyecto `recordatoriosteclab` → Compilación/Build → Firestore Database.
2. Si aún no existe, Crear base de datos **Cloud Firestore Standard**, ID `(default)`;
   elegir la región adecuada y comenzar en **modo producción**, nunca modo prueba público.
   La elección de ubicación requiere decisión del propietario.
3. Abrir pestaña Reglas, revisar el contenido íntegro de `firestore.rules`, reemplazar
   reglas anteriores que pudieran conceder acceso más amplio y pulsar **Publicar**.
4. Confirmar publicación/versionado en Console. Validar con Rules Playground o Emulator
   Suite: anónimo denegado, A en A permitido, B en A denegado, rutas ajenas denegadas.
5. Autorizar expresamente las pruebas con dos usuarios de prueba y los documentos que
   se crearán/modificarán. Solo entonces compilar:
   `./gradlew :app:assembleDebug -PfirestoreSyncEnabled=true`.
6. Verificar CRUD, dos dispositivos, desconexión/reconexión, reinicio, eliminación lógica,
   cuentas aisladas y reintentos contra Firestore real. Publicación y estas pruebas pendientes.

El JSON Firebase existente sigue ignorado por Git; no requiere generar otro archivo falso.

## Pruebas implementadas

`SincronizadorFirestoreTest`: pruebas instrumentadas con Room real en memoria y un
remoto determinista en memoria, sin llamadas a Firebase:

1. Offline → pendientes conservados → ACK perdido → reintento sin duplicación → borrado lógico.
2. Edición concurrente durante envío no queda marcada como confirmada; luego se envía versión nueva.
3. Conflicto remoto conserva pendientes y permite descargar otros documentos/tombstones.
4. Descarga actualiza sin cambiar ID Room ni retroceder a versiones antiguas.
5. Serialización conserva contenido y rechaza discordancia de ID.

Se conservan las pruebas de Authentication, formularios, CRUD y aislamiento de cuentas.
`FirebaseRealTest` sigue excluido: no se crean cuentas ni documentos reales.

## Archivos creados y modificados

Creados:
- `app/src/main/java/com/example/recordatoriosteclab/RemotoFirestore.kt`
- `app/src/main/java/com/example/recordatoriosteclab/SincronizadorFirestore.kt`
- `app/src/androidTest/java/com/example/recordatoriosteclab/SincronizadorFirestoreTest.kt`
- `firestore.rules`
- `ACTIVIDAD_4_FIRESTORE.md`

Modificados:
- `app/build.gradle.kts`: SDK y parámetro explícito de habilitación.
- `AplicacionAutenticada.kt`: inyección del remoto con UID autenticado.
- `RecordatoriosRepositoryRoom.kt`: delegación y señal de sincronización automática.
- `RecordatoriosViewModel.kt`: reintentos ligados al ciclo de vida de la sesión.
- `SincronizadorRecordatorios.kt`: reutilización interna del validador DTO; backend histórico conservado.

## Estado real

Implementación local-first preparada; acceso remoto desactivado. No se publicaron reglas,
no se configuró la base desde Console ni se probaron lecturas/escrituras reales de Firestore.
La integración real NO se considera verificada. Sin commit ni push.

Referencias: [transacciones](https://firebase.google.com/docs/firestore/manage-data/transactions)
y [reglas](https://firebase.google.com/docs/firestore/security/rules-conditions).

## Resultado de verificación y problemas encontrados (23/11/2026)

- `:app:assembleDebug`: correcto, dependencia Firestore compatible con el toolchain existente.
- `:app:testDebugUnitTest`: 10 pruebas aprobadas, 0 fallos.
- `:app:connectedDebugAndroidTest`: 9 pruebas aprobadas (5 nuevas), 0 fallos,
  0 omisiones, en Medium Phone API 37. Las pruebas reales están excluidas por configuración.
- `:app:lintDebug`: 0 errores, 39 advertencias (incluye recomendación de catálogo para
  la dependencia nueva; no se actualizaron versiones por esas advertencias).
- Verificación combinada final: BUILD SUCCESSFUL en 14 s.
- `git diff --check`: sin errores.
- Se detectó durante revisión que campos opcionales remotos de tipo incorrecto podían
  confundirse con null; el decodificador ahora los rechaza antes de aplicar el documento.
- No hubo errores de compilación en esta integración. Las pruebas no ejecutaron el SDK
  contra un servidor Firestore ni validaron reglas en Emulator Suite; esas comprobaciones
  siguen pendientes y no deben inferirse de los resultados con dobles.


## Verificación de reglas y sincronización real — 23/11/2026

### Reglas publicadas y contrato

Se abrió Firebase Console del proyecto `recordatoriosteclab`, base `(default)` en
ubicación `nam5`. Se consultó la versión activa de Reglas marcada con estrella,
«Hoy • 6:19 p.m.». Su contenido coincide con `firestore.rules` (sin considerar líneas
vacías finales). No se editaron ni publicaron reglas durante esta verificación.

Se comparó la estructura publicada con `RecordatorioEntity`, `aMapa()`, `desdeMapa()`
y las categorías disponibles. Los documentos generados contienen exactamente:

| Campo | Tipo enviado y permitido |
| --- | --- |
| claveSincronizacion | String UUID, idéntico al ID del documento |
| titulo | String, mínimo 4 caracteres; el repositorio recorta espacios |
| descripcion | String de hasta 200 caracteres o null |
| categoria | Estudio, Trabajo o Compras |
| prioridad | ALTA, MEDIA o BAJA |
| estado | PENDIENTE o COMPLETADO |
| fechaCreacion, fechaModificacion | Long de Kotlin → entero Firestore positivo, milisegundos |
| ubicacion, aviso | String o null |
| versionLocal | Long → entero positivo |
| eliminado | Boolean |

No se envían `id` local ni `estadoSincronizacion`. El UID se representa en la ruta,
no en un campo libre del documento. El UUID es estable y las fechas no son Timestamp:
se usan enteros compatibles con las reglas. La actualización conserva fechaCreacion.

### Seguridad: Emulator Suite (no producción)

Se instalaron herramientas oficiales en `/tmp/teclab-firestore-tools` y se ejecutó
Firestore Emulator contra **demo-teclab-rules**, exclusivamente en `127.0.0.1:8088`.
`tests/firestore/rules.test.cjs` carga el archivo real `firestore.rules`.

Resultado: **7 pruebas aprobadas, 0 fallos**:

1. Anónimo: lectura y escritura denegadas.
2. A: creación, lectura, edición, repetición idempotente y borrado lógico propios permitidos.
3. B: lectura, listado y escritura bajo A denegados.
4. Campos extra/faltantes, categoría/prioridad/UUID inválidos y tipos incorrectos denegados.
5. Borrado físico denegado incluso al dueño.
6. Regresión de versión, cambio sin incrementar versión y alteración de fechaCreacion denegados.
7. Ruta global `reminders` sin permisos.

Los mensajes PERMISSION_DENIED del emulador son resultados esperados de pruebas negativas.
Reproducción desde raíz, con Java 21+ y Node/npm:

```bash
npm install --prefix tests/firestore
npm --prefix tests/firestore test
```

No usar un ID de proyecto real ni desplegar desde esta configuración de emuladores.

### Compilación y suites Android

- `./gradlew :app:assembleDebug -PfirestoreSyncEnabled=true`: **BUILD SUCCESSFUL**.
- También se compiló el APK de instrumentación con ese parámetro.
- La suite normal conserva pruebas de Authentication, Compose y Room; excluye
  `FirebaseRealTest` y `FirestoreRealTest` mediante `notAnnotation` y la anotación `PruebaFirebaseReal`.
- El acceso real continúa siendo opt-in en Gradle; no se cambió el valor por defecto.

### Firebase real: operaciones efectivamente comprobadas

El usuario autorizó explícitamente las dos cuentas/documentos listados abajo. Se utilizó
el SDK Android real, `RemotoFirestore`, `SincronizadorFirestore` y Room. La prueba emplea
una instancia FirebaseApp nombrada para no utilizar la sesión personal/default de la UI.
No usa Admin SDK para las comprobaciones: las peticiones pasan por las reglas publicadas.

`FirestoreRealTest.sincronizacionReal`: **OK, 20,743 s** en la ejecución corregida.

- Creación y actualización confirmadas por lectura `Source.SERVER`.
- Desconexión real de Wi-Fi y datos del emulador, comprobando ausencia de red activa.
- Edición offline guardada en Room; sincronización devuelve error y conserva pendiente.
- Reconexión, confirmación remota y dos reintentos sin duplicados (un documento por UID).
- Descarga a otra base Room inicialmente vacía con el contenido actualizado.
- Eliminación lógica de A confirmada en servidor y propagada a la base receptora.
- A sin autenticar: lectura denegada por servidor.
- B: lectura/escritura de A denegadas; datos propios permitidos y Room de B sin datos de A.
- Categoría inválida y eliminación física de A: PERMISSION_DENIED del servidor.

`FirestoreRealTest.recuperarTrasReinstalacion`: **OK, 3,132 s**.

Se desinstaló realmente la aplicación de prueba del emulador y se reinstalaron ambos APK.
Solo se preservó en memoria del proceso de prueba el inventario temporal de credenciales;
no se restauraron Room, DataStore, sesión Firebase ni caché Firestore. Después de ingresar
con B, se recuperó desde SERVER su documento exacto en una nueva base Room persistente.
No se usaron ni guardaron credenciales personales fuera del emulador.

La prueba valida el repositorio y el SDK remoto: no sustituye un recorrido manual completo
de todos los formularios Compose ni mide el temporizador de reintento de 30 segundos.

### Problemas y correcciones

El primer intento real falló al esperar un registro pendiente tras `disableNetwork()`:
la transacción sí había sido confirmada, aunque una lectura posterior dio error. Por tanto,
ese método del SDK no era una simulación válida de desconexión total para esta prueba.
Se corrigió el **test**, desconectando Wi-Fi/datos del emulador, verificando la ausencia
de red y restaurando su estado previo en un bloque finally. Se reanudó con las mismas
cuentas y documentos, sin crear extras. El segundo intento aprobó todos los escenarios.
No se modificó código productivo ni reglas para ocultar el fallo.

Se agregaron exclusión explícita y autorización por argumento para evitar que la suite
normal cree cuentas o datos remotos. La reanudación requiere argumento adicional y usa
el inventario privado existente; nunca debe repetirse sin revisar la limpieza pendiente.

### Inventario temporal y limpieza

| Cuenta | UID | Documento bajo users/{uid}/reminders |
| --- | --- | --- |
| teclab-fs-a-20261123-2126@example.com | 7MuCTsHRAnN5XvnGTP9vu99vPzc2 | b52ebf65-d798-4c62-80a1-f01c35cb05ac |
| teclab-fs-b-20261123-2126@example.com | hy9F2kqhwfg5rmI9iGB3e1JDc3s1 | cd0bc24e-a65c-4604-9ec3-2d322bb84ffb |

Títulos iniciales: `TEMP Teclab Firestore A 20261123` y `TEMP Teclab Firestore B 20261123`.
A se editó a `TEMP Teclab A editado` y `TEMP Teclab A sin conexion`, luego quedó eliminado
lógicamente. Categoría Estudio, prioridad ALTA, estado PENDIENTE, opcionales null.
No se crearon documentos padre con campos, colecciones globales ni datos personales.

El usuario confirmó el borrado irreversible de ambas rutas. Se borró **cada documento
exacto** desde Firebase Console y se observó su colección vacía. No se borró la colección
users ni se tocaron otras rutas. Las reglas de clientes permanecieron intactas.

`FirestoreRealTest.limpiarCuentasDespuesDeConsole`: **OK, 3,967 s**.
El SDK volvió a comprobar ambas colecciones vacías mediante SERVER, verificó el UID de
cada cuenta, eliminó esas dos cuentas y borró el inventario privado de credenciales.
La desinstalación de pruebas y la suite local posterior retiran sus datos del emulador.
Las credenciales temporales no se incorporaron a Git ni a este informe.

### Archivos de esta verificación

Nuevos:
- `firebase.emulators.json`
- `tests/firestore/package.json`
- `tests/firestore/rules.test.cjs`
- `app/src/androidTest/java/com/example/recordatoriosteclab/FirestoreRealTest.kt`

Modificados:
- `.gitignore`: logs y dependencias locales de Emulator Suite.
- `app/build.gradle.kts`: exclusión de la nueva suite real en ejecuciones normales.
- `ACTIVIDAD_4_FIRESTORE.md`: evidencia, alcance, incidencias e inventario.

Los cambios de implementación previos siguen sin commit; no se revirtieron ni se publicaron.
`firestore.rules` no fue modificado en esta verificación.

### Escenarios pendientes y estado final

**Sincronización real verificada** para creación, edición, borrado lógico, desconexión /
reconexión, reintentos sin duplicados, recuperación tras reinstalación e aislamiento.
**Seguridad verificada** con Emulator Suite y con las peticiones reales descritas arriba.

Pendientes fuera de esa cobertura: edición concurrente desde dos dispositivos reales,
resolución de conflictos en UI, revocación de sesión durante un envío, límites/cuotas,
caídas de red prolongadas y recorrido manual completo Compose. Los conflictos siguen
protegidos por las pruebas locales, pero no se probó una pareja de dispositivos físicos.
No hay WorkManager: no se promete sincronización con el proceso terminado.
No se modificaron reglas publicadas. Sin commit ni push.


### Cierre de la verificación tras interrupción de la aplicación

El cierre interrumpió una repetición de la suite local y apagó el emulador, después
de que las pruebas reales y la limpieza ya habían terminado correctamente. Se reinició
Medium Phone API 37 y se completó el control final sin repetir operaciones reales:

- Comando combinado assembleDebug, testDebugUnitTest, lintDebug y connectedDebugAndroidTest:
  **BUILD SUCCESSFUL**.
- 10 pruebas unitarias aprobadas y 9 instrumentadas locales aprobadas, sin fallos.
- Lint: 0 errores, 39 advertencias.
- 7 pruebas de reglas previamente aprobadas en Emulator Suite.
- Pruebas reales y reinstalación aprobadas; dos documentos y dos cuentas temporales eliminados.
- `git diff --check`: correcto. No se hizo commit ni push.

Se detectó que el filtro notClass con dos clases no excluyó FirestoreRealTest del
reporte XML: tres supuestos de autorización aparecían como fallos, aunque no ejecutaron
operaciones remotas. Se reemplazó por notAnnotation y una anotación común
`PruebaFirebaseReal` en ambas suites reales. Archivos adicionales modificados:
`FirebaseRealTest.kt`, `FirestoreRealTest.kt`, `app/build.gradle.kts`; creado
`app/src/androidTest/java/com/example/recordatoriosteclab/PruebaFirebaseReal.kt`.

Resultado final con el filtro corregido: **BUILD SUCCESSFUL en 36 s**; XML de
instrumentación con **9 pruebas, 0 fallos, 0 errores y 0 omisiones**. Las suites
remotas no forman parte de esa ejecución. Lint permanece en 0 errores y 39 advertencias.
