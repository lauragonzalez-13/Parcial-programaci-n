import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoteDespacho { id:number; codigo:string; cantidadDisponible:number; fechaVencimiento?:string; vencido:boolean; proximoVencer:boolean; vigente:boolean; }
export interface EntregaDespacho { loteId:number; cantidad:number; }
export interface EventoDespacho { estado:string; usuarioResponsable:string; fecha:string; observacion?:string; }
export interface DetalleEntregado { fecha:string; lote:string; cantidad:number; movimientoId:number; }
export interface FormulaDespacho {
  formulaId:number; fechaGeneracion:string; veterinario:string; propietario:string; mascota:string; medicamento:string; medicamentoId:number;
  cantidadPrescrita:number; cantidadEntregada:number; cantidadPendiente:number; dosis:string; via?:string; frecuencia:string; duracionDias:number;
  indicaciones:string; estado:string; despachoId?:number; usuarioResponsable?:string; entregadoEn?:string; existenciaActual:number;
  lotes:LoteDespacho[]; detalleEntregas?:DetalleEntregado[]; eventos?:EventoDespacho[];
}

@Injectable({providedIn:'root'})
export class DespachosService {
  private readonly api='http://localhost:8080/clinica/v1/despachos';
  constructor(private http:HttpClient){}
  pendientes():Observable<FormulaDespacho[]>{return this.http.get<FormulaDespacho[]>(this.api);}
  historial():Observable<FormulaDespacho[]>{return this.http.get<FormulaDespacho[]>(`${this.api}/historial`);}
  preparar(id:number,usuarioResponsable:string):Observable<FormulaDespacho>{return this.http.post<FormulaDespacho>(`${this.api}/${id}/preparar`,{usuarioResponsable});}
  confirmar(id:number,data:{usuarioResponsable:string;observacion:string;detalles:EntregaDespacho[]}):Observable<FormulaDespacho>{return this.http.post<FormulaDespacho>(`${this.api}/${id}/confirmar`,data);}
  pendienteDisponibilidad(id:number,usuarioResponsable:string,observacion:string):Observable<FormulaDespacho>{return this.http.post<FormulaDespacho>(`${this.api}/${id}/pendiente-disponibilidad`,{usuarioResponsable,observacion});}
  cancelar(id:number,usuarioResponsable:string,observacion:string):Observable<FormulaDespacho>{return this.http.post<FormulaDespacho>(`${this.api}/${id}/cancelar`,{usuarioResponsable,observacion});}
  eventos(id:number):Observable<EventoDespacho[]>{return this.http.get<EventoDespacho[]>(`${this.api}/formula/${id}/eventos`);}
  comprobante(id:number):Observable<Blob>{return this.http.get(`${this.api}/${id}/comprobante.pdf`,{responseType:'blob'});}
}
