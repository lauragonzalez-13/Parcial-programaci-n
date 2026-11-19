import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProductoInventario {
  id: number; codigo: string; nombre: string; descripcion?: string; presentacion?: string;
  tipo: 'MEDICAMENTO' | 'INSUMO'; registroInvima?: string; stockMinimo: number; ivaPorcentaje?: number;
  fechaProximoPedido?: string; cantidadDisponible: number; precioCompraMinimo: number;
  precioVenta: number; proximoVencimiento?: string; estado: string; activo: boolean;
}
export interface ProductoRequest {
  codigo: string; nombre: string; descripcion: string; presentacion: string;
  tipo: 'MEDICAMENTO' | 'INSUMO'; registroInvima: string; stockMinimo: number; fechaProximoPedido: string | null; ivaPorcentaje?: number;
}
export interface LoteInventario {
  id: number; codigoLote: string; cantidadDisponible: number; precioCompra: number; precioVenta: number;
  fechaIngreso: string; fechaVencimiento?: string; medicamento: { id: number; nombre: string; codigo: string };
}
export interface MovimientoInventario {
  id: number; tipo: string; cantidad: number; fecha: string; usuarioResponsable: string;
  observacion?: string; medicamento: { id: number; nombre: string; codigo: string };
  lote: { codigoLote: string }; formulaMedica?: { id: number };
}
export interface FilaImportacion {
  fila: number; codigo: string; nombre: string; tipo: string; codigoLote: string;
  cantidad: number; precioCompra: number; precioVenta: number; fechaIngreso?: string;
  fechaVencimiento?: string; errores: string[];
}
export interface PreviewImportacion { token: string; registrosValidos: number; registrosConError: number; filas: FilaImportacion[]; }
export interface Importacion { id: number; nombreArchivo: string; usuarioResponsable: string; fecha: string; registrosLeidos: number; registrosImportados: number; registrosRechazados: number; detalleErrores?: string; }

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly api = 'http://localhost:8080/clinica/v1/inventario';
  constructor(private http: HttpClient) {}
  productos(disponibles = false): Observable<ProductoInventario[]> { return this.http.get<ProductoInventario[]>(`${this.api}/medicamentos`, { params: { disponibles } }); }
  crearProducto(data: ProductoRequest): Observable<ProductoInventario> { return this.http.post<ProductoInventario>(`${this.api}/medicamentos`, data); }
  actualizarProducto(id: number, data: ProductoRequest): Observable<ProductoInventario> { return this.http.put<ProductoInventario>(`${this.api}/medicamentos/${id}`, data); }
  actualizarIva(id:number,ivaPorcentaje:number):Observable<ProductoInventario>{return this.http.patch<ProductoInventario>(`${this.api}/medicamentos/${id}/iva`,{ivaPorcentaje});}
  lotes(medicamentoId?: number): Observable<LoteInventario[]> { return this.http.get<LoteInventario[]>(`${this.api}/lotes`, { params: medicamentoId ? { medicamentoId } : {} }); }
  ingresarLote(data: object): Observable<LoteInventario> { return this.http.post<LoteInventario>(`${this.api}/lotes`, data); }
  movimientos(medicamentoId?: number): Observable<MovimientoInventario[]> { return this.http.get<MovimientoInventario[]>(`${this.api}/movimientos`, { params: medicamentoId ? { medicamentoId } : {} }); }
  importaciones(): Observable<Importacion[]> { return this.http.get<Importacion[]>(`${this.api}/importaciones`); }
  previsualizar(archivo: File, usuario: string): Observable<PreviewImportacion> { const body = new FormData(); body.append('archivo', archivo); body.append('usuarioResponsable', usuario); return this.http.post<PreviewImportacion>(`${this.api}/importaciones/preview`, body); }
  confirmar(token: string, usuario: string): Observable<Importacion> { return this.http.post<Importacion>(`${this.api}/importaciones/${token}/confirmar`, null, { params: { usuarioResponsable: usuario } }); }
}
