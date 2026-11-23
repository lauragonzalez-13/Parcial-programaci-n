# Entregable 3 — Guía de evidencias funcionales

Las capturas deben tomarse ejecutando la aplicación con datos reales de la base `prueba`; este documento no incluye capturas simuladas. Para cada prueba guarda captura de pantalla completa donde se vean módulo, resultado y, cuando aplique, fecha/filtro. Complementa con la respuesta de API o consulta SQL indicada.

## Preparación

1. Ejecuta `docker compose up --build -d` y abre http://localhost:4200.
2. Ingresa con la cuenta local de demostración indicada en el README. Crea cuentas de prueba para secretaria, veterinario y despacho desde Usuarios y perfiles.
3. Registra especialidad y tarifa; crea veterinario activo con disponibilidad, cliente y mascota.

## Evidencias requeridas

| Evidencia | Pasos verificables | Captura / comprobación adicional |
|---|---|---|
| Importación masiva (>50) | En Inventario, importa `backend2026-80869/inventario_demo_60.csv`; revisa la previsualización y confirma. El archivo trae 60 filas con códigos/lotes únicos de demostración. | Captura de preview y confirmación mostrando 60 importados. Verifica que el historial de importaciones registre leídos/importados/rechazados. Si repites la importación, los códigos/lotes ya existen y las filas repetidas deben rechazarse. |
| Descuento por dispensación | Asocia medicamento con stock a una fórmula, finaliza la atención, ingresa como perfil Despacho, prepara y confirma la entrega. | Captura del antes/después y recibo. Consulta `GET /inventario/movimientos` y confirma movimiento de salida; el lote debe bajar por la cantidad entregada. |
| Citas en calendario | Entra con perfil veterinario vinculado a un veterinario activo, registra/consulta una cita y cambia las vistas día, semana y mes. Prueba un cruce de horario y uno fuera de disponibilidad. | Captura de cada vista y del mensaje de validación. La cita debe persistir tras recargar. |
| Fórmula y factura | Finaliza una consulta con diagnóstico; registra fórmula con cantidad/dosis y genera prefactura. Genera factura y registra un pago parcial y luego el saldo. | Guarda PDF de fórmula, prefactura/factura, captura de estado `PAGO_PARCIAL` y luego `PAGADA`; verifica líneas, impuestos y eventos. |
| Reportes PDF y Excel | En Reportes selecciona fechas con datos y al menos un filtro disponible; previsualiza y descarga ambos formatos. | Captura previa al exportar y archivos descargados abiertos mostrando encabezados, periodo, generación y totales. |

## Pruebas cruzadas sugeridas

- Intenta guardar una cita con mascota de otro propietario, veterinario inactivo, fecha pasada y horario ocupado. El backend debe rechazar cada caso aunque se omita el formulario y se invoque la API directamente.
- Intenta facturar dos veces la misma consulta. La segunda operación debe rechazarse y la primera factura permanecer.
- Intenta dispensar cantidad superior a la existencia utilizable. No debe haber movimiento ni saldo de lote negativo.
- Registra pago parcial y comprueba `pagado`, saldo y estado; anula una factura con motivo y verifica que permanezca visible en el historial.
- Comprueba perfiles: despacho puede consultar inventario y operar despachos; no debe poder administrar agenda o facturación.

## Consulta SQL para soporte de capturas

En MySQL Workbench conectado a `prueba`, ejecuta consultas de solo lectura como:

```sql
SELECT COUNT(*) AS lotes_importados FROM lote_inventario;
SELECT tipo, COUNT(*) AS movimientos, SUM(cantidad) AS unidades FROM movimiento_inventario GROUP BY tipo;
SELECT estado, COUNT(*) AS citas FROM consulta GROUP BY estado;
SELECT estado, COUNT(*) AS facturas, SUM(total) AS total FROM factura GROUP BY estado;
```

Conserva los CSV, PDFs y XLSX resultantes junto con las capturas en la carpeta de entrega solicitada por el docente.
