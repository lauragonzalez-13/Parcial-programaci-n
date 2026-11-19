import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';
import { ToastService } from './toast.service';

const isApiRequest = (url: string): boolean => url.includes('/clinica/v1/');
const isMutation = (method: string): boolean => ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);
const isAuthenticationRequest = (url: string): boolean => /\/auth\/(login|logout|validate-token)(\?|$)/.test(url);

function successMessage(method: string, url: string): string {
  if (url.includes('/consultas') && url.endsWith('/cancelar')) return 'La cita fue cancelada y su historial se conservó.';
  if (url.includes('/consultas')) return method === 'POST' ? 'La cita se creó correctamente.' : 'La cita se actualizó correctamente.';
  if (url.includes('/mascotas')) return method === 'POST' ? 'La mascota se registró correctamente.' : 'Los datos de la mascota se guardaron.';
  if (url.includes('/clientes')) return method === 'POST' ? 'El cliente se registró correctamente.' : 'Los datos del cliente se guardaron.';
  if (url.includes('/disponibilidad')) return 'La disponibilidad del veterinario se actualizó.';
  if (url.includes('/especialidades')) return 'La especialidad se guardó correctamente.';
  if (url.includes('/tarifas')) return 'La tarifa se guardó correctamente.';
  if (url.includes('/facturas') && url.includes('/pagos')) return 'El pago quedó registrado en la factura.';
  if (url.includes('/facturas')) return 'La factura se procesó correctamente.';
  if (url.includes('/inventario')) return 'La operación de inventario se realizó correctamente.';
  if (url.includes('/formulas-medicas') && url.endsWith('/dispensar')) return 'La dispensación se registró y actualizó el inventario.';
  if (url.includes('/formulas-medicas')) return 'La fórmula médica se guardó correctamente.';
  if (url.includes('/historias-clinicas') || url.includes('/anotaciones')) return 'La historia clínica se guardó correctamente.';
  if (url.includes('/atencion-medica')) return 'La atención médica se guardó correctamente.';
  if (method === 'DELETE') return 'El registro se eliminó correctamente.';
  if (method === 'POST') return 'El registro se creó correctamente.';
  return 'Los cambios se guardaron correctamente.';
}

function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    const detail = typeof body === 'string' ? body : body?.mensaje ?? body?.message ?? body?.error;
    if (typeof detail === 'string' && detail.trim()) return detail;
    if (error.status === 0) return 'No fue posible conectar con el servidor. Verifica que el backend esté ejecutándose.';
    if (error.status === 404) return 'No se encontró el registro solicitado.';
    if (error.status === 409) return 'La operación entra en conflicto con otro registro o una regla del sistema.';
    if (error.status === 400) return 'Revisa los datos ingresados y las reglas de validación.';
    if (error.status >= 500) return 'Ocurrió un error en el servidor. Intenta nuevamente.';
  }
  return 'Ocurrió un error inesperado. Intenta nuevamente.';
}

export const responseToastInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(ToastService);
  const apiRequest = isApiRequest(request.url);
  return next(request).pipe(
    tap((event) => {
      // HttpClient emite varios eventos (envío, progreso, respuesta); notificar
      // únicamente en la respuesta final para no repetir el toast.
      if (event instanceof HttpResponse && apiRequest && isMutation(request.method) && !isAuthenticationRequest(request.url) && !request.url.includes('/importaciones/preview')) {
        notifications.success(successMessage(request.method, request.url));
      }
    }),
    catchError((error: unknown) => {
      // Este GET se usa para comprobar si una cita todavía no tiene factura.
      const expectedInvoiceLookup = request.method === 'GET' && request.url.includes('/facturas/consulta/') && error instanceof HttpErrorResponse && error.status === 404;
      if (apiRequest && !isAuthenticationRequest(request.url) && !expectedInvoiceLookup) notifications.error(errorMessage(error));
      return throwError(() => error);
    })
  );
};
