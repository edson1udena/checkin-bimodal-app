# Integración de datos — pasajeros con servicio bimodal

Esta es la fase que conecta la app a datos reales: quiénes compraron el tramo bimodal para
cada frecuencia, en vez del `MockPassengerRepository` de ejemplo.

## Cómo queda armado, en una frase

BigQuery (donde ya vive el modelo del reporte de Power BI) → una vista de solo lectura →
una hoja de Google Sheets conectada a esa vista, que se actualiza sola cada 15-20 min →
un pequeño Web App de Apps Script que expone esa hoja como JSON → la app Android la lee.

Nadie tiene que mantener un servidor: Google Sheets y Apps Script son gratuitos y los aloja
Google. Lo único que hay que mantener es la vista de BigQuery si algún día cambian las
columnas de origen.

## Por qué no un `.xlsx` literal

Se evaluó la opción de exportar un Excel real (a SharePoint/OneDrive) y que la app lo lea
directamente. Se descartó porque la app tendría que autenticarse contra Microsoft y parsear
el archivo Excel completo cada vez que se abre — lento, pesado en el teléfono y frágil si el
archivo crece. Leer una hoja de Google Sheets como datos simples (filas y columnas) es
muchísimo más liviano y no requiere ninguna librería de Excel en el celular.

## Paso 1 — Crear la vista en BigQuery

Usa `integracion/bigquery_view_pasajeros_bimodal.sql` como plantilla. Reemplaza los
`<<marcadores>>` por los nombres reales de tu proyecto/dataset/tabla — son los mismos que ya
usa el modelo semántico de Power BI, así que quien lo mantiene hoy los tiene a la mano.

La vista expone **solo** las columnas que la app necesita (fecha, hora del tren, dirección
del tramo, código de reserva, nombre, documento, tipo, unidad de bus) y **solo** los
pasajeros que sí tienen el tramo bimodal — no toda la tabla de ventas.

## Paso 2 — Conectar esa vista a Google Sheets

1. Crea una hoja de cálculo nueva en Google Sheets.
2. **Datos → Conectores de datos → Conectar a BigQuery.**
3. Selecciona tu proyecto, el dataset, y la vista `vw_checkin_bimodal_pasajeros`.
4. Esto crea una pestaña conectada (Google la nombra automáticamente, algo como
   "BigQuery de vw_checkin_bimodal_pasajeros"). Anota ese nombre exacto — lo necesitas en el
   paso 3.
5. **Datos → Conectores de datos → Programar actualización** → configúralo cada 15 minutos
   (lo más cercano a los 20 min que buscabas; el mínimo disponible depende de tu plan de
   Google Workspace).

## Paso 3 — Publicar el Web App de Apps Script

1. En esa misma hoja: **Extensiones → Apps Script.**
2. Borra el contenido de `Code.gs` y pega el contenido de
   `integracion/AppsScript_WebApp.gs` (está en este mismo proyecto).
3. Ajusta las dos líneas marcadas `<-- AJUSTAR` al inicio del script:
   - `SHEET_NAME`: el nombre exacto de la pestaña conectada del paso 2.
   - `API_TOKEN`: un texto secreto largo inventado por ti (por ejemplo, generado en
     [uuidgenerator.net](https://www.uuidgenerator.net/)). Este token es lo único que evita
     que cualquiera con la URL pueda leer los datos de pasajeros — no lo compartas ni lo
     subas a GitHub.
4. Guarda. **Implementar → Nueva implementación** → tipo **"Aplicación web"**:
   - Ejecutar como: **Yo** (tu cuenta)
   - Quién tiene acceso: **Cualquier usuario**
5. Autoriza los permisos que Google pida la primera vez.
6. Copia la **URL de la aplicación web** que te entrega — la vas a necesitar en el paso 4.

Si más adelante editas este script, tienes que crear una **nueva versión** de la
implementación (Implementar → Gestionar implementaciones → ✏️ → Nueva versión) para que el
cambio se refleje en la URL ya publicada.

## Paso 4 — Configurar la app Android

En la raíz del proyecto (junto a `settings.gradle.kts`), crea o edita el archivo
`local.properties` (Android Studio ya crea uno con la ruta del SDK — solo agrégale estas dos
líneas al final):

```properties
salesApiBaseUrl=https://script.google.com/macros/s/AKfycb.../exec
salesApiToken=el-mismo-token-secreto-que-pusiste-en-el-Apps-Script
```

**Importante:** `local.properties` está en `.gitignore` — nunca se sube a GitHub. Es
justamente para que este secreto no quede expuesto en un repositorio público. Si alguna vez
lo compartes por error, genera un token nuevo y actualiza el Apps Script.

Con esas dos líneas presentes, la app detecta automáticamente la integración real y usa
`SalesApiPassengerRepository` en vez de los datos de ejemplo — no hay que cambiar ningún otro
archivo. Si `local.properties` no tiene esas líneas (como en la compilación de GitHub
Actions, que no tiene acceso a tus secretos), la app sigue funcionando con datos de ejemplo
sin errores.

## Qué pasa con el estado de abordaje (marcar quién abordó)

Este paso conecta el **lado de lectura**: quién compró y debería abordar. El registro de
quién abordó/no abordó realmente sigue viviendo solo en el teléfono durante el turno — todavía
no hay un backend que lo reciba (eso es la fase pendiente sobre JESAB/GOP mencionada en el
README). Cuando llegue esa fase, el punto de conexión es `closeManifest()` en
`SalesApiPassengerRepository.kt`, ya marcado con un `TODO`.

## Verificar que quedó bien conectado

1. Abre la URL del Web App directamente en un navegador, agregando `?token=tu-token` al
   final. Deberías ver un JSON con `"frequencies": [...]`. Si ves `{"error": "..."}`, el
   mensaje te dice qué revisar (nombre de pestaña, columnas faltantes, token).
2. Con `local.properties` configurado, recompila la app (Android Studio o un nuevo push que
   dispare GitHub Actions) e instala el `.apk` nuevo — la lista de frecuencias y pasajeros ya
   debería mostrar los datos reales del día en vez de los de ejemplo.
