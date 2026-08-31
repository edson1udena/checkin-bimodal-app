# Check-in Bimodal — Proyecto Android (prototipo funcional)

App Android nativa (Kotlin + Jetpack Compose) para que la tripulación SAB registre qué
pasajeros del servicio bimodal abordaron realmente cada unidad, según lo conversado en
el plan de proyecto (`plan-app-registro-abordaje-bimodal`).

## Estado actual

Este proyecto **usa datos de ejemplo** (`MockPassengerRepository`), no está conectado al
sistema de ventas de Inca Rail todavía — eso se deja para la fase de integración, tal
como acordamos. Las 4 pantallas y el flujo completo (login → frecuencias → lista de
pasajeros → cierre de manifiesto) ya son funcionales con esos datos de ejemplo.

Este código **no fue compilado en el entorno donde se generó** (el entorno de Claude no
tiene acceso al SDK de Android por política de red). Está escrito siguiendo las
convenciones estándar de un proyecto Android/Compose moderno, pero la primera vez que lo
abras en Android Studio puede pedirte sincronizar Gradle y quizás ajustar alguna versión
si tu Android Studio instalado es distinto al que se asumió aquí (ver "Versiones" abajo).

## Cómo abrirlo

1. Instala [Android Studio](https://developer.android.com/studio) (versión Koala/2024.1 o más reciente).
2. Abre esta carpeta (`checkinbimodal/`) como proyecto existente ("Open").
3. Deja que Gradle sincronice (puede tardar unos minutos la primera vez, descarga dependencias).
4. Ejecuta en un emulador Android o en un teléfono conectado por USB (▶ Run).

## Estructura del proyecto

```
app/src/main/java/com/incarail/checkinbimodal/
  MainActivity.kt              → entrada de la app y navegación entre las 4 pantallas
  data/
    Models.kt                  → modelos de dominio (Frequency, Passenger, BoardingSummary)
    PassengerRepository.kt     → interfaz + implementación de ejemplo (MockPassengerRepository)
  viewmodel/
    CheckinViewModel.kt        → estado y lógica de la UI, habla solo con PassengerRepository
  ui/
    screens/Screens.kt         → las 4 pantallas en Compose (Login, Frecuencias, Pasajeros, Resumen)
    theme/Theme.kt             → colores de marca (mismos que el prototipo web presentado)
```

## Cómo conectar la integración real (siguiente fase)

Todo el punto de esta estructura es que conectar el sistema de ventas real sea un cambio
acotado, sin tocar las pantallas:

1. Crear una clase `SalesApiPassengerRepository : PassengerRepository` (en `data/`) que
   implemente los 5 métodos de la interfaz llamando a la API real (autenticación, URL
   base, y el mapeo del modelo de reserva de Inca Rail hacia `Passenger` se hacen ahí).
2. En `MainActivity.kt`, cambiar `CheckinViewModel()` para que reciba
   `SalesApiPassengerRepository()` en vez de `MockPassengerRepository()`.
3. Nada más en el resto del proyecto necesita cambiar.

Para la persistencia local (caché de la lista + cola de sincronización cuando no hay
señal) ya se agregó la dependencia de Room en `build.gradle.kts`; falta el plugin de
`ksp`/`kapt` y las entidades/DAO, que se añaden en esa misma fase.

## Versiones asumidas

- Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00.
- `compileSdk`/`targetSdk` 34, `minSdk` 26 (cubre prácticamente todos los Android en uso hoy).

Si tu Android Studio sugiere una versión distinta del plugin de Android o de Kotlin al
sincronizar, acepta la que te sugiera — son ajustes menores de compatibilidad, no de
lógica.

## Compilación automática (GitHub Actions)

Este repositorio incluye `.github/workflows/build-apk.yml`, que compila el `.apk` de
depuración automáticamente en cada push a `main` (o manualmente desde la pestaña
**Actions → Build APK → Run workflow**). Al terminar, el `.apk` queda descargable en esa
misma ejecución, dentro de la sección **Artifacts** (`checkin-bimodal-debug-apk`), sin que
nadie tenga que instalar Android Studio.

## Distribución para el piloto

Mientras no se publique en Google Play, se recomienda **Firebase App Distribution** o
instalar el APK directamente en los teléfonos del personal SAB (usando el `.apk` que
genera Android Studio en *Build > Build Bundle(s)/APK(s) > Build APK(s)*).
