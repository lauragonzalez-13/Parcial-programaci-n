import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LineaFactura { id:number; tipo:string; descripcion:string; cantidad:number; valorUnitario:number; ivaPorcentaje:number; subtotal:number; impuesto:number; total:number; codigoLote?:string; }
export interface PagoFactura { id:number; numeroTicket?:string; valor:number; medio:string; referencia?:string; registradoPor:string; fecha:string; montoRecibido:number; cambio:number; }
export interface EventoFactura { id:number; accion:string; detalle?:string; usuario:string; fecha:string; }
export type EstadoFactura='PENDIENTE_PAGO'|'PAGO_PARCIAL'|'PAGADA'|'ANULADA';
export interface Factura { id:number; numero:string; fechaGeneracion:string; estado:EstadoFactura; subtotal:number; descuento:number; impuestos:number; total:number; pagado:number; descuentoAutorizadoPor?:string; motivoAnulacion?:string; cliente:{id:number;nombre:string;apellido:string;documento:string}; mascota:{id:number;nombre:string;especie:string}; veterinario:{id:number;nombre:string;apellido:string;especialidad?:{nombre:string}}; consulta:{id:number}; lineas:LineaFactura[]; pagos:PagoFactura[]; eventos:EventoFactura[]; }
@Injectable({providedIn:'root'}) export class FacturacionService {
  private readonly api='http://localhost:8080/clinica/v1/facturas';constructor(private http:HttpClient){}
  listar():Observable<Factura[]>{return this.http.get<Factura[]>(this.api);}
  cliente(id:number):Observable<Factura[]>{return this.http.get<Factura[]>(this.api,{params:{clienteId:id}});}
  mascota(id:number):Observable<Factura[]>{return this.http.get<Factura[]>(this.api,{params:{mascotaId:id}});}
  porConsulta(id:number):Observable<Factura>{return this.http.get<Factura>(`${this.api}/consulta/${id}`);}
  generar(id:number,descuento=0,autorizadoPor='',tarifasAdicionales:number[]=[]):Observable<Factura>{return this.http.post<Factura>(`${this.api}/consulta/${id}`,{descuento,autorizadoPor,tarifasAdicionales});}
  pagar(id:number,data:{valor:number;medio:string;referencia:string;registradoPor:string;montoRecibido:number}):Observable<PagoFactura>{return this.http.post<PagoFactura>(`${this.api}/${id}/pagos`,data);}
  ticket(id:number,pagoId:number):Observable<Blob>{return this.http.get(`${this.api}/${id}/pagos/${pagoId}/ticket.pdf`,{responseType:'blob'});}
  anular(id:number,motivo:string,usuario:string):Observable<Factura>{return this.http.patch<Factura>(`${this.api}/${id}/anular`,{motivo,usuario});}
  pdf(id:number):Observable<Blob>{return this.http.get(`${this.api}/${id}/pdf`,{responseType:'blob'});}
}
