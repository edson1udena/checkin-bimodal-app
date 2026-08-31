-- Vista de solo lectura para alimentar el check-in de abordaje bimodal.
--
-- Qué hace: expone únicamente las columnas que la app necesita, y solo para los pasajeros
-- que sí tienen el tramo bimodal contratado — no toda tu tabla de ventas/reservas.
-- Esto es intencional: mientras menos columnas expongamos aquí, menos datos sensibles
-- viajan hacia la hoja de Google Sheets y hacia el teléfono.
--
-- Cómo usarla:
-- 1) Reemplaza cada <<marcador>> por el nombre real en tu proyecto de BigQuery. Si tú no
--    tienes permisos de escritura en ese dataset, pásale este archivo a quien mantiene hoy
--    el modelo semántico de Power BI — son los mismos nombres de tabla/columna que ya usan
--    ahí, solo hay que confirmarlos.
-- 2) Ejecuta esto una sola vez en BigQuery (Editor de consultas) para crear la vista.
-- 3) Esa vista es la que conectas después a Google Sheets (ver INTEGRACION_DATOS.md).

CREATE OR REPLACE VIEW `<<proyecto_gcp>>.<<dataset>>.vw_checkin_bimodal_pasajeros` AS
SELECT
  DATE(fecha_servicio)               AS fecha_servicio,      -- fecha del viaje
  hora_salida_tren                   AS hora_salida_tren,    -- hora de la FRECUENCIA DE TREN asociada, ej. '07:00'
  direccion_tramo_bimodal            AS direccion,           -- ej. 'Cusco → Ollantaytambo' / 'Ollantaytambo → Cusco'
  codigo_reserva                     AS codigo_reserva,      -- PNR o código único de venta (identifica al pasajero)
  nombre_pasajero                    AS nombre_pasajero,
  numero_documento                   AS documento,
  tipo_pasajero                      AS tipo,                -- ej. 'Directo' / 'Agencia' — ajusta a tus valores reales
  unidad_bus_asignada                AS unidad_bus           -- opcional; puede ser NULL si aún no se asigna
FROM `<<proyecto_gcp>>.<<dataset>>.<<tabla_ventas_o_reservas>>`
WHERE tiene_servicio_bimodal = TRUE   -- el mismo filtro que ya usa tu reporte de Power BI para marcar "tiene bimodal"
  AND DATE(fecha_servicio) = CURRENT_DATE('America/Lima');

-- Nota: si el bimodal puede vender el tramo por separado (sin ir siempre ligado 1 a 1 a la
-- hora exacta del tren), ajusta `direccion_tramo_bimodal` para que sea el dato que usa tu
-- equipo hoy para agrupar "quiénes van en este bus", aunque no sea literalmente la hora del tren.
