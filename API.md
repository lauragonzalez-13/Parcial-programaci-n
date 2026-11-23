# Entregable 4 — Servicios API

Base URL: `http://localhost:8080/clinica/v1`. Salvo login y validación de token, se requiere `Authorization: Bearer <token>`. Fechas se envían en ISO (`YYYY-MM-DD`, `YYYY-MM-DDTHH:mm:ss`). Los cuerpos JSON concretos se pueden explorar desde Swagger. Errores de validación usan códigos HTTP 4xx y un mensaje explicativo.

Swagger UI: `/swagger-ui.html`; documento OpenAPI: `/v3/api-docs`.

| Área | Método y ruta | Uso |
|---|---|---|
| Sesión | `POST /auth/login` | Autenticar `username` y `password`; devuelve identidad, rol y token. |
| Sesión | `POST /auth/validate-token`, `POST /auth/logout` | Validar o revocar token. |
| Usuarios | `GET/POST /auth/usuarios`, `DELETE /auth/usuarios/{id}` | Listar/crear/eliminar cuentas (administración). |
| Clientes | `GET /clientes`, `GET /clientes/{id}` | Consultar propietarios. |
| Mascotas | `GET /mascotas`, `GET /mascotas/{id}`, `POST /mascotas`, `PUT /mascotas/{id}`, `PATCH /mascotas/{id}/desactivar` | Gestionar pacientes. |
| Catálogo | `GET /clientes/{id}/mascotas`, `GET /veterinarios`, `POST /veterinarios`, `GET /veterinarios/especialidades` | Cargar opciones válidas para formularios. |
| Agenda | `GET/POST /consultas`, `GET/PUT /consultas/{id}`, `PATCH /consultas/{id}/cancelar`, `PATCH /consultas/{id}/atender` | Crear, editar, consultar y cambiar estado de citas. |
| Agenda | `GET /consultas/veterinario/{id}`, `GET /consultas/mascota/{id}/historial`, `GET /consultas/reportes/veterinario/{id}` | Calendario, historial del paciente y exportación de agenda. |
| Disponibilidad | `GET/POST /veterinarios/{id}/disponibilidad`, `DELETE /veterinarios/{id}/disponibilidad/{horarioId}` | Gestionar horarios profesionales. |
| Historia | `GET/POST /historias-clinicas`, `GET /historias-clinicas/{id}`, `GET /historias-clinicas/{id}/anotaciones`, `POST /historias-clinicas/{id}/anotaciones` | Historia longitudinal y notas clínicas. |
| Atención | `POST /atencion-medica/{consultaId}/finalizar`, `GET /atencion-medica/{consultaId}/prefactura.pdf`, `GET /atencion-medica/formulas/{formulaId}.pdf` | Finalizar consulta y generar PDF. |
| Inventario | `GET/POST /inventario/medicamentos`, `PUT /inventario/medicamentos/{id}`, `GET/POST /inventario/lotes`, `GET /inventario/movimientos`, `GET /inventario/alertas` | Catálogo y existencias por lote. |
| Carga masiva | `POST /inventario/importaciones/preview`, `POST /inventario/importaciones/{token}/confirmar`, `GET /inventario/importaciones` | Validar, confirmar y auditar CSV/XLSX. |
| Despacho | `GET /despachos`, `GET /despachos/historial`, `GET /despachos/formula/{id}/lotes`, `POST /despachos/{formulaId}/preparar`, `/confirmar`, `/pendiente-disponibilidad`, `/cancelar` | Dispensar y auditar medicamentos; confirmar genera movimientos de salida. |
| Tarifas | `GET/POST /administracion/tarifas`, `PUT /administracion/tarifas/{id}`, `PATCH /administracion/tarifas/{id}/estado`, `GET /administracion/tarifas/{id}/historial` | Administración de tarifas e IVA. |
| Facturación | `GET /facturas`, `GET /facturas/{id}`, `POST /facturas/consulta/{consultaId}`, `POST /facturas/{id}/pagos`, `PATCH /facturas/{id}/anular`, `GET /facturas/{id}/pdf` | Facturas, pagos y documento. Los filtros `clienteId` y `mascotaId` se aplican al GET. |
| Reportes | `GET /reportes/{tipo}`, `GET /reportes/{tipo}/pdf`, `GET /reportes/{tipo}/excel` | Vista previa, PDF y XLSX por rango `desde`/`hasta`; admite `veterinarioId`, `especialidadId`, `medicamentoId`. |

Los módulos y contratos están sujetos a las reglas descritas en [MODELO_DATOS.md](MODELO_DATOS.md). En Swagger, ejecuta primero login, copia `token` y usa **Authorize** con el token Bearer.
