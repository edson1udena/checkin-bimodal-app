/**
 * Web App de solo lectura que expone, como JSON, los datos de la hoja de Google Sheets
 * conectada a BigQuery (Conectores de datos → BigQuery), para que la app Android de
 * check-in bimodal los consuma.
 *
 * CÓMO DESPLEGAR (una sola vez):
 * 1. Abre la hoja de Google Sheets que ya conectaste a la vista de BigQuery
 *    (vw_checkin_bimodal_pasajeros) — ver INTEGRACION_DATOS.md si aún no la creaste.
 * 2. Extensiones → Apps Script. Borra el contenido de Code.gs y pega este script completo.
 * 3. Reemplaza SHEET_NAME (abajo) por el nombre exacto de la pestaña donde aparecen los
 *    datos conectados (Google la nombra automáticamente algo como "BigQuery de vw_...").
 * 4. Reemplaza API_TOKEN por un texto secreto largo e inventado por ti — cualquier cadena
 *    larga y aleatoria sirve (por ejemplo, generada en https://www.uuidgenerator.net/).
 *    Este token es lo único que evita que cualquiera con la URL pueda leer los datos.
 * 5. Guarda. Luego: Implementar → Nueva implementación → tipo "Aplicación web".
 *      - Ejecutar como: Yo (tu cuenta)
 *      - Quién tiene acceso: Cualquier usuario
 * 6. Autoriza los permisos que Google te pida la primera vez (acceso a tus propias hojas).
 * 7. Copia la "URL de la aplicación web" que te entrega — esa, junto con el token del
 *    paso 4, es lo que se configura en local.properties del proyecto Android
 *    (ver INTEGRACION_DATOS.md).
 *
 * Si más adelante cambias el contenido de este script, tienes que crear una NUEVA versión
 * de la implementación (Implementar → Gestionar implementaciones → editar → Nueva versión)
 * para que los cambios se reflejen en la URL ya publicada.
 */

const SHEET_NAME = 'NOMBRE_DE_LA_PESTAÑA_CONECTADA'; // <-- AJUSTAR
const API_TOKEN = 'REEMPLAZA_ESTO_POR_UN_TOKEN_SECRETO_LARGO'; // <-- AJUSTAR

function doGet(e) {
  const providedToken = e && e.parameter ? e.parameter.token : null;
  if (providedToken !== API_TOKEN) {
    return jsonResponse({ error: 'No autorizado' });
  }

  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAME);
  if (!sheet) {
    return jsonResponse({ error: 'No se encontró la pestaña "' + SHEET_NAME + '". Revisa SHEET_NAME en este script.' });
  }

  const values = sheet.getDataRange().getValues();
  if (values.length < 2) {
    return jsonResponse({ generatedAt: new Date().toISOString(), frequencies: [] });
  }

  const headers = values[0].map(function (h) { return String(h).trim(); });
  const rows = values.slice(1).filter(function (r) {
    return r.some(function (cell) { return cell !== '' && cell !== null; });
  });

  function col(name) { return headers.indexOf(name); }
  const idx = {
    fecha: col('fecha_servicio'),
    hora: col('hora_salida_tren'),
    direccion: col('direccion'),
    codigoReserva: col('codigo_reserva'),
    nombre: col('nombre_pasajero'),
    documento: col('documento'),
    tipo: col('tipo'),
    unidadBus: col('unidad_bus'),
  };

  if (idx.fecha < 0 || idx.hora < 0 || idx.direccion < 0 || idx.codigoReserva < 0 || idx.nombre < 0) {
    return jsonResponse({
      error: 'Faltan columnas esperadas en la pestaña. Encontradas: ' + headers.join(', '),
    });
  }

  const frequencyMap = {};
  const order = [];

  rows.forEach(function (row) {
    const fecha = formatDate(row[idx.fecha]);
    const hora = String(row[idx.hora]);
    const direccion = String(row[idx.direccion]);
    const freqId = fecha + '-' + hora + '-' + direccion;

    if (!frequencyMap[freqId]) {
      frequencyMap[freqId] = {
        id: freqId,
        time: hora,
        route: direccion,
        unit: idx.unidadBus >= 0 ? String(row[idx.unidadBus] || '') : '',
        passengers: [],
      };
      order.push(freqId);
    }

    frequencyMap[freqId].passengers.push({
      id: String(row[idx.codigoReserva]),
      name: String(row[idx.nombre]),
      document: idx.documento >= 0 ? String(row[idx.documento] || '') : '',
      type: idx.tipo >= 0 ? String(row[idx.tipo] || 'Directo') : 'Directo',
    });
  });

  return jsonResponse({
    generatedAt: new Date().toISOString(),
    frequencies: order.map(function (id) { return frequencyMap[id]; }),
  });
}

function formatDate(value) {
  if (Object.prototype.toString.call(value) === '[object Date]') {
    return Utilities.formatDate(value, 'America/Lima', 'yyyy-MM-dd');
  }
  return String(value);
}

function jsonResponse(obj) {
  // Nota: los Web Apps de Apps Script siempre responden HTTP 200; los errores se
  // comunican dentro del propio JSON con la clave "error", y la app Android los revisa ahí.
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
