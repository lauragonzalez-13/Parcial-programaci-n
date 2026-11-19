import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { FacturacionService, Factura } from 'src/app/services/facturacion.service';
import { ClienteCita, ConsultasService, MascotaCita } from 'src/app/services/consultas.service';

@Component({selector:'app-facturacion',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./facturacion.html',styleUrl:'./facturacion.scss'})
export class FacturacionComponent implements OnInit {
  facturas:Factura[]=[];clientes:ClienteCita[]=[];mascotas:MascotaCita[]=[];selected?:Factura;error='';mensaje='';clienteId=0;mascotaId=0;
  pago={valor:0,medio:'EFECTIVO',referencia:'',registradoPor:'secretaria'};
  constructor(private api:FacturacionService,private catalogos:ConsultasService){}
  ngOnInit():void{forkJoin({f:this.api.listar(),c:this.catalogos.clientes()}).subscribe({next:r=>{this.facturas=r.f;this.clientes=r.c;},error:e=>this.error=e?.error?.mensaje||'No se pudieron cargar las facturas.'});}
  get saldo():number{return this.selected?this.selected.total-this.selected.pagado:0;}
  cargar():void{const req=this.mascotaId?this.api.mascota(this.mascotaId):this.clienteId?this.api.cliente(this.clienteId):this.api.listar();req.subscribe({next:f=>this.facturas=f,error:e=>this.error=e?.error?.mensaje||'No se pudo consultar el historial.'});this.selected=undefined;}
  cambiarCliente():void{this.mascotaId=0;this.mascotas=[];if(this.clienteId)this.catalogos.mascotas(this.clienteId).subscribe(m=>this.mascotas=m);this.cargar();}
  ver(f:Factura):void{this.api.porConsulta(f.consulta.id).subscribe({next:x=>{this.selected=x;this.pago.valor=x.total-x.pagado;},error:e=>this.error=e?.error?.mensaje||'No se pudo abrir la factura.'});}
  pagar():void{if(!this.selected)return;const consultaId=this.selected.consulta.id;this.api.pagar(this.selected.id,{...this.pago,valor:Number(this.pago.valor)}).subscribe({next:()=>{this.mensaje='Pago registrado en el historial.';this.cargar();this.api.porConsulta(consultaId).subscribe(f=>this.selected=f);},error:e=>this.error=e?.error?.mensaje||'No se pudo registrar el pago.'});}
  descargar(f:Factura):void{this.api.pdf(f.id).subscribe({next:b=>{const u=URL.createObjectURL(b);const a=document.createElement('a');a.href=u;a.download=`${f.numero}.pdf`;a.click();URL.revokeObjectURL(u);},error:()=>this.error='No se pudo descargar el PDF.'});}
  anular(f:Factura):void{const motivo=prompt('Motivo de anulación:');if(!motivo?.trim())return;const usuario=prompt('Responsable de la anulación:');if(!usuario?.trim())return;this.api.anular(f.id,motivo,usuario).subscribe({next:()=>{this.mensaje='Factura anulada con trazabilidad.';this.cargar();},error:e=>this.error=e?.error?.mensaje||'No se pudo anular la factura.'});}
}
