# Check-in Tren & Bus — Proyecto Android (prototipo funcional)

App Android nativa (Kotlin + Jetpack Compose) para que la tripulación SAB registre qué
pasajeros abordaron realmente cada unidad — tanto del servicio **bimodal/bus** como del
**tren** — según el plan original (`plan-app-registro-abordaje-bimodal`) y, de forma
**incremental**, algunas piezas del brief oficial *"Aplicativo Abordaje Tren&Bus"* (v4.0,
Transformación Digital) que se solapaban con lo ya construido: escaneo de QR, registro de
tripulación/proveedores y una ficha de pasajero con incidencia/condición médica/requerimiento
especial. Ver la sección **"Alcance de este incremento"** más abajo para lo que sí y lo que
no cubre esta versión frente al brief completo.

## Estado actual

Este proyecto **usa datos de ejemplo** (`MockPassengerRepository`) salvo que configures la
integración real (ver más abajo). El flujo completo — login → frecuencias (Tren/Bus) →
lista de pasajeros (con escaneo QR) → tripulación/proveedores → ficha de pasajero → cierre
de manifiesto — ya es funcional con esos datos de ejemplo, y fue **compilado e instalado
exitosamente** en un teléfono real vía GitHub Actions antes de este incremento.

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
  MainActivity.kt              → entrada de la app y navegación (Login, Frecuencias, Pasajeros,
                                  Tripulación, Ficha, Resumen)
  data/
    Models.kt                  → modelos de dominio (Frequency, Passenger, CrewMember, enums
                                  ServiceMode/PersonRole, BoardingSummary)
    PassengerRepository.kt     → interfaz + implementación de ejemplo (MockPassengerRepository),
                                  incluye BoardingRegistrationResult / CrewRegistrationResult
    SalesApiPassengerRepository.kt → implementación real (Sheets/BigQuery)
  viewmodel/
    CheckinViewModel.kt        → estado y lógica de la UI, habla solo con PassengerRepository
  ui/
    scan/QrScanner.kt          → pantalla reutilizable de escaneo QR (CameraX + ML Kit)
    screens/Screens.kt         → pantallas en Compose: Login, Frecuencias (Tren/Bus), Pasajeros
                                  (+ QR), Tripulación/Proveedores, Ficha de pasajero, Resumen
    theme/Theme.kt             → colores de marca (mismos que el prototipo web presentado)
```

## Alcance de este incremento (Tren & Bus)

A Inca Rail le llegó un brief oficial mucho más amplio (*"Aplicativo Abordaje Tren&Bus"*,
prioridad ALTA) que incluye, además de lo de aquí, modo de emergencia con OTP, impresión de
manifiestos en PDF, un módulo médico/incidencias con flujo iOS/Web, consola de administración
y escritura en tiempo real hacia los sistemas de Inca Rail (JESAB/GOP). Se decidió, junto con
el equipo, tomar **solo la parte que es una extensión natural de lo ya construido** y dejar
el resto para cuando el equipo de TI/backend de Inca Rail participe:

**Sí incluido en este incremento:**
- Selector Tren / Bus en la lista de frecuencias (antes solo bus/bimodal).
- Escaneo de QR real (cámara, sin costo ni backend propio) para registrar el abordaje de un
  pasajero por su boleto/documento, con las validaciones del brief: vagón/frecuencia
  incorrecta, registro duplicado y código no encontrado — cada caso muestra una alerta clara
  al colaborador, con opción de entrada manual si el QR falla o no hay cámara.
- Registro de tripulación/proveedores por QR o formulario manual, por frecuencia.
- Ficha de pasajero ampliada: teléfono, correo, asiento, condición médica, requerimiento
  especial e incidencia (tipo + detalle) — **queda en el teléfono durante el turno**, no se
  envía a ningún sistema de salud/incidencias central.
- Login con nombre del colaborador (para mostrar "Hola, `<nombre>`" y quedar registrado
  como quien hizo cada check-in).

**Explícitamente fuera de este incremento** (requieren backend/IT de Inca Rail, no son
responsables de construir solo con IA):
- Modo de emergencia con OTP y flujo de doble validación.
- Impresión de manifiestos en PDF con valor legal/auditable.
- Envío en tiempo real de incidencias médicas a un sistema central (hoy solo queda en el
  teléfono, ver nota en la Ficha de pasajero).
- Consola de administración web.
- Escritura real hacia JESAB/GOP (sigue pendiente, ver más abajo).
- Versión iOS o Web del módulo de emergencia médica.

## Integración de datos reales (ya implementada)

La app ya puede leer, en vez de datos de ejemplo, la lista real de pasajeros con servicio
bimodal — vía una hoja de Google Sheets conectada a BigQuery (donde vive hoy el modelo del
reporte de Power BI). Ver **`INTEGRACION_DATOS.md`** para el paso a paso completo (crear la
vista en BigQuery, conectarla a Sheets, publicar el Web App de Apps Script, y configurar
`local.properties` en este proyecto).

`SalesApiPassengerRepository` (en `data/`) es la implementación real de `PassengerRepository`
que hace esto — `MainActivity.kt` elige automáticamente entre ella y `MockPassengerRepository`
según si `local.properties` tiene configurada la integración, así que una compilación sin ese
secreto (como la de GitHub Actions) nunca se rompe.

Lo que **todavía no** está conectado: el envío del resumen de abordaje (quién abordó
realmente) a un backend — eso sigue siendo la fase pendiente sobre JESAB/GOP, marcada con un
`TODO` en `closeManifest()` de `SalesApiPassengerRepository.kt`.

Para la persistencia local (caché offline si se pierde señal a mitad de ruta) ya se agregó
la dependencia de Room en `build.gradle.kts`; falta el plugin de `ksp`/`kapt` y las
entidades/DAO, que se pueden añadir cuando se priorice ese caso de uso.

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
