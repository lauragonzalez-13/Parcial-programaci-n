import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import { ConsultasService, ConsultaCita } from 'src/app/services/consultas.service';
import { AtencionService } from 'src/app/services/atencion.service';
import { AnotacionClinica } from 'src/app/services/historias-clinicas.service';
import { FormulasMedicasService, FormulaMedica, NuevaFormulaMedica } from 'src/app/services/formulas-medicas.service';
import { InventarioService, ProductoInventario } from 'src/app/services/inventario.service';
import { FacturacionService, Factura, PagoFactura } from 'src/app/services/facturacion.service';
import { TarifasService, Tarifa } from 'src/app/services/tarifas.service';

@Component({selector:'app-atencion-medica',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./atencion-medica.html',styleUrl:'./atencion-medica.scss'})
export class AtencionMedicaComponent implements OnInit {
  consultaId=0;cita?:ConsultaCita;historial:AnotacionClinica[]=[];formulas:FormulaMedica[]=[];productos:ProductoInventario[]=[];factura?:Factura;tarifasAdicionales:Tarifa[]=[];tarifasSeleccionadas:number[]=[];
  error='';mensaje='';cargando=true;guardando=false;creandoFormula=false;pagando=false;
  clinica={motivo:'',sintomas:'',diagnostico:'',procedimiento:'',observaciones:'',recomendaciones:''};
  receta={productoId:0,dosis:'',viaAdministracion:'Oral',frecuencia:'Cada 8 horas',duracionDias:5,unidadesPorToma:1,tomasPorDia:3,indicaciones:'',observaciones:''};
  pago={valor:0,medio:'EFECTIVO',referencia:'',registradoPor:'',montoRecibido:0};
  descuento=0;descuentoAutorizadoPor='';
  constructor(private route:ActivatedRoute,private consultas:ConsultasService,private atencion:AtencionService,private formulasApi:FormulasMedicasService,private inventario:InventarioService,private facturasApi:FacturacionService,private tarifasApi:TarifasService,private cdr:ChangeDetectorRef){}
  ngOnInit():void{this.consultaId=Number(this.route.snapshot.paramMap.get('id'));this.cargarCita();this.inventario.productos().subscribe({next:p=>{this.productos=p.filter(x=>x.tipo==='MEDICAMENTO');this.cdr.detectChanges();},error:()=>{this.error='No se pudo consultar el inventario.';this.cdr.detectChanges();}});this.tarifasApi.listar().subscribe({next:t=>{this.tarifasAdicionales=t.filter(x=>x.activa&&x.tipoServicio!=='CONSULTA');this.cdr.detectChanges();},error:()=>{}});}
  recargarCita():void{this.cargarCita();}
  private cargarCita():void{
    if(!Number.isInteger(this.consultaId)||this.consultaId<1){this.error='El identificador de la cita no es válido. Vuelve al calendario y ábrela nuevamente.';this.cargando=false;return;}
    this.error='';this.cargando=true;
    this.consultas.buscar(this.consultaId).pipe(timeout(15000),finalize(()=>{this.cargando=false;this.cdr.detectChanges();})).subscribe({
      next:c=>{this.cita=c;this.clinica.motivo=c.motivo;this.historialPrevio();if(c.estado==='ATENDIDA')this.cargarPostAtencion();else if(c.estado==='EN_ATENCION')this.cargarFormulas();else if(c.estado!=='CANCELADA'&&localStorage.getItem('clinica.rol')==='VETERINARIO')this.atencion.iniciar(this.consultaId).subscribe({next:()=>{if(this.cita)this.cita.estado='EN_ATENCION';this.cargarFormulas();this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo iniciar la atención.';this.cdr.detectChanges();}});},
      error:e=>{this.error=e?.name==='TimeoutError'?'El servidor tardó más de 15 segundos en responder. Verifica que el backend esté ejecutándose y vuelve a intentar.':e?.error?.mensaje||e?.message||'No se pudo cargar la cita. Verifica la conexión con el backend.';}
    });
  }
  get atendida():boolean{return this.cita?.estado==='ATENDIDA';}
  get enAtencion():boolean{return this.cita?.estado==='EN_ATENCION';}
  get productoSeleccionado():ProductoInventario|undefined{return this.productos.find(p=>p.id===Number(this.receta.productoId));}
  get cantidadRecetada():number{return Number(this.receta.unidadesPorToma)*Number(this.receta.tomasPorDia)*Number(this.receta.duracionDias);}
  get cambioEfectivo():number{return this.pago.medio==='EFECTIVO'?Math.max(0,Number(this.pago.montoRecibido)-Number(this.pago.valor)):0;}
  private cargarFormulas():void{this.formulasApi.listarPorConsulta(this.consultaId).subscribe({next:f=>{this.formulas=f;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron consultar las fórmulas.';this.cdr.detectChanges();}});}
  get hayRecetaPendiente():boolean{return this.formulas.some(f=>f.medicamentoInventario&&f.estadoDispensacion!=='DISPENSADA');}
  toggleTarifa(id:number,checked:boolean):void{this.tarifasSeleccionadas=checked?[...this.tarifasSeleccionadas,id]:this.tarifasSeleccionadas.filter(x=>x!==id);}
  historialPrevio():void{if(!this.cita)return;this.atencion.historialMascota(this.cita.mascota.id).subscribe({next:h=>{this.historial=h.filter(x=>x.consulta?.id!==this.consultaId);this.cdr.detectChanges();},error:()=>{this.historial=[];this.cdr.detectChanges();}});}
  finalizar():void{if(!this.cita||this.guardando)return;this.error='';this.mensaje='';if(!this.clinica.diagnostico.trim()){this.error='Registra el diagnóstico para finalizar.';this.cdr.detectChanges();return;}this.guardando=true;this.cdr.detectChanges();this.atencion.finalizar(this.consultaId,this.clinica).subscribe({next:()=>{this.guardando=false;this.cita!.estado='ATENDIDA';this.mensaje='Atención clínica guardada y asociada al historial de la mascota.';this.cdr.detectChanges();this.cargarPostAtencion();},error:e=>{this.guardando=false;this.error=e?.error?.mensaje||'No se pudo finalizar la atención.';this.cdr.detectChanges();}});}
  cargarPostAtencion():void{if(this.cita)this.atencion.historialMascota(this.cita.mascota.id).subscribe({next:rows=>{this.historial=rows.filter(x=>x.consulta?.id!==this.consultaId);const actual=rows.find(x=>x.consulta?.id===this.consultaId);if(actual)this.clinica={motivo:actual.descripcion,sintomas:actual.sintomas||'',diagnostico:actual.diagnostico||'',procedimiento:actual.procedimiento||'',observaciones:actual.observacion||'',recomendaciones:actual.recomendaciones||''};this.cdr.detectChanges();},error:()=>this.cdr.detectChanges()});this.formulasApi.listarPorConsulta(this.consultaId).subscribe({next:f=>{this.formulas=f;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron consultar las fórmulas.';this.cdr.detectChanges();}});this.facturasApi.porConsulta(this.consultaId).subscribe({next:f=>{this.factura=f;this.cdr.detectChanges();},error:()=>{this.factura=undefined;this.cdr.detectChanges();}});}
  guardarFormula():void{if(!this.cita||!this.receta.productoId||!this.receta.dosis.trim()){this.error='Selecciona un medicamento e indica la dosis.';return;}const product=this.productoSeleccionado!;const r:NuevaFormulaMedica={fechaEmision:new Date().toISOString().slice(0,10),medicamento:product.nombre,dosis:this.receta.dosis.trim(),viaAdministracion:this.receta.viaAdministracion,frecuencia:this.receta.frecuencia,duracionDias:Number(this.receta.duracionDias),indicaciones:this.receta.indicaciones.trim()||'Seguir la dosis y duración indicadas.',observaciones:this.receta.observaciones.trim()||undefined,mascota:{id:this.cita!.mascota.id},consulta:{id:this.consultaId},medicamentoInventario:{id:product.id},cantidadRequerida:this.cantidadRecetada,unidadesPorToma:Number(this.receta.unidadesPorToma),tomasPorDia:Number(this.receta.tomasPorDia)};this.creandoFormula=true;this.formulasApi.guardar(r).subscribe({next:f=>{this.creandoFormula=false;this.mensaje='Fórmula registrada; queda pendiente de despacho.';this.formulas=[f,...this.formulas];this.inventario.productos().subscribe(p=>this.productos=p.filter(x=>x.tipo==='MEDICAMENTO'));},error:e=>{this.creandoFormula=false;this.error=e?.error?.mensaje||'No se pudo guardar la fórmula.';}});}
  dispensar(f:FormulaMedica):void{this.error='La entrega de medicamentos ahora se realiza desde el módulo Despacho médico.';}
  pdfFormula(id:number):void{this.formulasApi.descargarPdf(id).subscribe({next:b=>this.descargar(b,`formula-${id}.pdf`),error:()=>this.error='No se pudo generar la fórmula en PDF.'});}
  pdfPrefactura():void{this.atencion.prefactura(this.consultaId).subscribe({next:b=>this.descargar(b,`prefactura-${this.consultaId}.pdf`),error:e=>this.error=e?.error?.mensaje||'No se pudo generar la prefactura.'});}
  generarFactura():void{this.error='';if(this.hayRecetaPendiente){this.error='Dispensa todos los medicamentos formulados desde inventario antes de facturar.';return;}this.facturasApi.generar(this.consultaId,Number(this.descuento),this.descuentoAutorizadoPor,this.tarifasSeleccionadas).subscribe({next:f=>{this.factura=f;this.mensaje='Factura generada. Las tarifas e impuestos quedaron guardados como valores históricos.';},error:e=>this.error=e?.error?.mensaje||'No se pudo generar la factura.'});}
  registrarPago():void{if(!this.factura)return;this.pagando=true;const data={...this.pago,valor:Number(this.pago.valor),montoRecibido:Number(this.pago.montoRecibido)};this.facturasApi.pagar(this.factura.id,data).subscribe({next:p=>{this.pagando=false;this.facturasApi.porConsulta(this.consultaId).subscribe(f=>this.factura=f);this.mensaje=`Pago registrado. Ticket ${p.numeroTicket||p.id} generado.`;this.descargarTicket(p.id);this.cdr.detectChanges();},error:e=>{this.pagando=false;this.error=e?.error?.mensaje||'No se pudo registrar el pago.';this.cdr.detectChanges();}});}
  descargarTicket(pagoId:number):void{if(!this.factura)return;this.facturasApi.ticket(this.factura.id,pagoId).subscribe({next:b=>this.descargar(b,`ticket-pago-${pagoId}.pdf`),error:e=>{this.error=e?.error?.mensaje||'No se pudo generar el ticket de pago.';this.cdr.detectChanges();}});}
  pdfFactura():void{if(this.factura)this.facturasApi.pdf(this.factura.id).subscribe({next:b=>this.descargar(b,`${this.factura!.numero}.pdf`),error:()=>this.error='No se pudo descargar la factura.'});}
  private descargar(blob:Blob,name:string):void{const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);}
}
