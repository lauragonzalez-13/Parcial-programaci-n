import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoReporte = 'FINANCIERO' | 'INVENTARIO' | 'MEDICO';
export interface ReporteClinica { tipo: string; desde: string; hasta: string; generado: string; totales: Record<string, string | number>; filas: Record<string, unknown>[]; }
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly api = 'http://localhost:8080/clinica/v1/reportes';
  constructor(private http: HttpClient) {}
  consultar(tipo: TipoReporte, desde: string, hasta: string, filtros: { veterinarioId?: number; especialidadId?: number; medicamentoId?: number } = {}): Observable<ReporteClinica> {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);
    for (const [key, value] of Object.entries(filtros)) if (value) params = params.set(key, value);
    return this.http.get<ReporteClinica>(`${this.api}/${tipo}`, { params });
  }
  descargar(tipo: TipoReporte, formato: 'pdf' | 'excel', desde: string, hasta: string, filtros: { veterinarioId?: number; especialidadId?: number; medicamentoId?: number } = {}): Observable<Blob> {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);
    for (const [key, value] of Object.entries(filtros)) if (value) params = params.set(key, value);
    return this.http.get(`${this.api}/${tipo}/${formato}`, { params, responseType: 'blob' });
  }
}
