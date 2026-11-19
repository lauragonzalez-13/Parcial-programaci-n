import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin, timeout } from 'rxjs';
import { ClienteCita, ConsultaCita, ConsultaRequest, ConsultasService, EstadoConsulta, MascotaCita, VeterinarioCita } from 'src/app/services/consultas.service';
import { PerfilDemoService } from 'src/app/services/perfil-demo.service';

@Component({ selector: 'app-calendario-medico', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './calendario-medico.html', styleUrl: './calendario-medico.scss' })
export class CalendarioMedicoComponent implements OnInit {
  citas: ConsultaCita[]=[]; clientes: ClienteCita[]=[]; mascotas: MascotaCita[]=[]; veterinarios: VeterinarioCita[]=[];
  fecha=this.fechaInicial(); vista:'mes'|'semana'|'dia'=this.vistaInicial(); loading=true; error=''; mensaje=''; seleccionado?:ConsultaCita;
  formulario=false; editId:number|null=null; historial:ConsultaCita[]=[]; mostrarHistorial=false; guardando=false;
  desde=this.localDate(new Date()); hasta=this.localDate(new Date());
  form={ clienteId:0, mascotaId:0, fecha:this.localDate(new Date()), hora:'09:00', duracionMinutos:30, motivo:'', estado:'PENDIENTE' as EstadoConsulta };
  constructor(private api:ConsultasService, private profile:PerfilDemoService, private router:Router, private cdr:ChangeDetectorRef) {}
  ngOnInit():void {
    if (this.profile.perfil!=='VETERINARIO') { this.router.navigate(['/citas']); return; }
    const id=this.profile.veterinarioId; if(!id){this.error='Selecciona un veterinario en el menú lateral.';this.loading=false;return;}
    forkJoin({c:this.api.clientes(),v:this.api.veterinarios()}).pipe(timeout({first:15000})).subscribe({next:d=>{this.clientes=d.c.filter(x=>x.activo);this.veterinarios=d.v.filter(x=>x.id===id);this.cargar();},error:e=>{this.loading=false;this.error=e?.name==='TimeoutError'?'El servidor tardó demasiado en cargar los datos iniciales. Comprueba el backend.':'No se pudieron cargar los datos del calendario.';this.cdr.detectChanges();}});
  }
  get vetId():number{return this.profile.veterinarioId;}
  get title():string{return this.fecha.toLocaleDateString('es-CO',{month:'long',year:'numeric'});}
  get rango():Date[]{
    if(this.vista==='dia') return [new Date(this.fecha)];
    const inicio=new Date(this.fecha); if(this.vista==='semana'){const d=(inicio.getDay()+6)%7;inicio.setDate(inicio.getDate()-d);} else inicio.setDate(1);
    const n=this.vista==='semana'?7:new Date(inicio.getFullYear(),inicio.getMonth()+1,0).getDate();
    return Array.from({length:n},(_,i)=>{const x=new Date(inicio);x.setDate(x.getDate()+i);return x;});
  }
  get resumen(){return {pendientes:this.citas.filter(x=>x.estado==='PENDIENTE').length,confirmadas:this.citas.filter(x=>x.estado==='CONFIRMADA').length,canceladas:this.citas.filter(x=>x.estado==='CANCELADA').length,finalizadas:this.citas.filter(x=>x.estado==='ATENDIDA').length};}
  cambiarVista(v:'mes'|'semana'|'dia'):void{this.vista=v;this.persistirVista();this.cargar();}
  mover(n:number):void{const d=new Date(this.fecha);if(this.vista==='mes')d.setMonth(d.getMonth()+n);else d.setDate(d.getDate()+n*(this.vista==='semana'?7:1));this.fecha=d;this.persistirVista();this.cargar();}
  hoy():void{this.fecha=new Date();this.persistirVista();this.cargar();}
  citasDia(d:Date):ConsultaCita[]{return this.citas.filter(c=>c.fechaHora.slice(0,10)===this.localDate(d)).sort((a,b)=>a.fechaHora.localeCompare(b.fechaHora));}
  cargar():void{const days=this.rango;const from=this.localDate(days[0]);const to=this.localDate(days[days.length-1]);this.loading=true;this.error='';this.api.porVeterinario(this.vetId,from,to).pipe(timeout({first:15000}),finalize(()=>{this.loading=false;this.cdr.detectChanges();})).subscribe({next:x=>{this.citas=x;this.cdr.detectChanges();},error:e=>{this.citas=[];this.error=e?.name==='TimeoutError'?'La agenda no respondió en 15 segundos. Revisa que el backend y MySQL estén activos.':e?.error?.mensaje||'No se pudieron consultar las citas.';this.cdr.detectChanges();}});}
  abrir(c:ConsultaCita):void{this.seleccionado=c;this.formulario=false;this.mostrarHistorial=false;}
  nueva():void{this.seleccionado=undefined;this.editId=null;this.form={clienteId:0,mascotaId:0,fecha:this.localDate(this.fecha),hora:'09:00',duracionMinutos:30,motivo:'',estado:'PENDIENTE'};this.mascotas=[];this.formulario=true;}
  editar(c:ConsultaCita):void{if(c.estado==='CANCELADA'||c.estado==='ATENDIDA')return;this.seleccionado=undefined;this.editId=c.id;this.form={clienteId:c.mascota.cliente.id,mascotaId:c.mascota.id,fecha:c.fechaHora.slice(0,10),hora:c.fechaHora.slice(11,16),duracionMinutos:c.duracionMinutos||30,motivo:c.motivo,estado:c.estado};this.cambiarCliente();this.formulario=true;}
  cambiarCliente():void{this.form.mascotaId=0;this.mascotas=[];this.cdr.detectChanges();if(this.form.clienteId)this.api.mascotas(Number(this.form.clienteId)).subscribe({next:m=>{this.mascotas=m.filter(x=>x.activa);this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar las mascotas del propietario.';this.cdr.detectChanges();}});}
  guardar():void{this.error='';if(this.guardando)return;if(!this.form.clienteId||!this.form.mascotaId||!this.form.fecha||!this.form.hora||!this.form.motivo.trim()){this.error='Completa propietario, mascota, fecha, hora y motivo.';return;}const r:ConsultaRequest={clienteId:Number(this.form.clienteId),mascotaId:Number(this.form.mascotaId),veterinarioId:this.vetId,fechaHora:`${this.form.fecha}T${this.form.hora}:00`,duracionMinutos:Number(this.form.duracionMinutos),motivo:this.form.motivo.trim(),estado:this.form.estado};this.guardando=true;this.cdr.detectChanges();(this.editId?this.api.actualizar(this.editId,r):this.api.crear(r)).pipe(timeout({first:20000}),finalize(()=>{this.guardando=false;this.cdr.detectChanges();})).subscribe({next:()=>{this.formulario=false;this.seleccionado=undefined;this.mensaje='Cita guardada correctamente.';this.fecha=new Date(`${this.form.fecha}T12:00:00`);this.persistirVista();this.citas=[];this.cdr.detectChanges();this.cargar();},error:e=>{this.error=e?.name==='TimeoutError'?'El servidor no respondió en 20 segundos. Recarga la agenda antes de volver a guardar para verificar si la cita alcanzó a registrarse.':e?.status===0?'No hay conexión con el backend. Comprueba que Spring Boot y MySQL estén activos.':e?.error?.mensaje||'No se pudo guardar la cita.';this.cdr.detectChanges();}});}
  atender(c:ConsultaCita):void{this.router.navigate(['/atencion-medica',c.id]);}
  cancelar(c:ConsultaCita):void{if(!confirm('¿Cancelar esta cita? El registro se conservará en el historial.'))return;this.api.cancelar(c.id).subscribe({next:()=>{this.seleccionado=undefined;this.cargar();},error:e=>this.error=e?.error?.mensaje||'No se pudo cancelar.'});}
  verHistorial(c:ConsultaCita):void{this.api.historialMascota(c.mascota.id).subscribe({next:x=>{this.historial=x;this.mostrarHistorial=true;},error:()=>this.error='No se pudo cargar el historial de la mascota.'});}
  generarReporte():void{if(!this.desde||!this.hasta||this.hasta<this.desde){this.error='Indica un rango de fechas válido.';return;}this.api.reporteVeterinario(this.vetId,this.desde,this.hasta).subscribe({next:b=>{const a=document.createElement('a');a.href=URL.createObjectURL(b);a.download=`citas-veterinario-${this.vetId}.xlsx`;a.click();URL.revokeObjectURL(a.href);},error:()=>this.error='No se pudo generar el Excel.'});}
  nombreCliente(c:ConsultaCita):string{return `${c.mascota?.cliente?.nombre||''} ${c.mascota?.cliente?.apellido||''}`.trim();}
  fin(c:ConsultaCita):string{const x=new Date(c.fechaHora);x.setMinutes(x.getMinutes()+(c.duracionMinutos||30));return x.toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'});}
  localDate(d:Date):string{const x=new Date(d.getTime()-d.getTimezoneOffset()*60000);return x.toISOString().slice(0,10);}
  private fechaInicial():Date{const value=localStorage.getItem('clinica.agenda.fecha');return value&&/^\d{4}-\d{2}-\d{2}$/.test(value)?new Date(`${value}T12:00:00`):new Date();}
  private vistaInicial():'mes'|'semana'|'dia'{const value=localStorage.getItem('clinica.agenda.vista');return value==='mes'||value==='dia'?value:'semana';}
  private persistirVista():void{localStorage.setItem('clinica.agenda.fecha',this.localDate(this.fecha));localStorage.setItem('clinica.agenda.vista',this.vista);}
  estadoLabel(s:string):string{return s==='ATENDIDA'?'Finalizada':s.charAt(0)+s.slice(1).toLowerCase();}
}
