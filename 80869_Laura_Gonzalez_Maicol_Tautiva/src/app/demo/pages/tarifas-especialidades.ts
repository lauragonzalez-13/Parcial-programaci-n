import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TarifasService, Especialidad, Tarifa, LogTarifa, DatosClinica } from 'src/app/services/tarifas.service';
import { ConsultasService, VeterinarioCita } from 'src/app/services/consultas.service';
import { InventarioService, ProductoInventario } from 'src/app/services/inventario.service';

@Component({selector:'app-tarifas-especialidades',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./tarifas-especialidades.html',styleUrl:'./tarifas-especialidades.scss'})
export class TarifasEspecialidadesComponent implements OnInit {
  especialidades:Especialidad[]=[];tarifas:Tarifa[]=[];logs:LogTarifa[]=[];veterinarios:VeterinarioCita[]=[];error='';mensaje='';editTarifaId:number|null=null;editEspId:number|null=null;asignacion={veterinarioId:0,especialidadId:0};
  espForm={nombre:'',descripcion:''};tarifaForm={codigo:'',nombre:'',descripcion:'',tipoServicio:'CONSULTA',especialidadId:null as number|null,valor:50000,ivaPorcentaje:0,activa:true,usuario:'administrador'};
  clinica:DatosClinica={nombre:'Clínica veterinaria',nit:'',telefono:'',direccion:'',correo:''};
  productos:ProductoInventario[]=[];productoIva={id:0,porcentaje:0};guardandoEspecialidad=false;guardandoTarifa=false;
  constructor(private api:TarifasService,private consultas:ConsultasService,private inventario:InventarioService,private cdr:ChangeDetectorRef){}
  ngOnInit():void{
    this.cargar();
    this.api.datosClinica().subscribe({next:d=>{this.clinica=d;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar los datos de la clínica.';this.cdr.detectChanges();}});
    this.consultas.veterinarios().subscribe({next:v=>{this.veterinarios=this.veterinariosSinDuplicados(v);this.asignacion.veterinarioId=this.veterinarios[0]?.id||0;this.asignacion.especialidadId=this.veterinarios[0]?.especialidad?.id||0;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar los veterinarios.';this.cdr.detectChanges();}});
    this.inventario.productos().subscribe({next:p=>{this.productos=p.filter(x=>x.tipo==='MEDICAMENTO');this.productoIva.id=this.productos[0]?.id||0;this.productoIva.porcentaje=this.productos[0]?.ivaPorcentaje||0;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar los productos.';this.cdr.detectChanges();}});
  }
  cargar():void{
    this.api.especialidades().subscribe({next:x=>{this.especialidades=x;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar especialidades.';this.cdr.detectChanges();}});
    this.api.listar().subscribe({next:x=>{this.tarifas=x;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron cargar las tarifas.';this.cdr.detectChanges();}});
  }
  guardarEspecialidad():void{if(this.guardandoEspecialidad)return;this.error='';this.mensaje='';this.guardandoEspecialidad=true;this.cdr.detectChanges();const op=this.editEspId?this.api.actualizarEspecialidad(this.editEspId,this.espForm):this.api.crearEspecialidad(this.espForm);op.subscribe({next:()=>{this.guardandoEspecialidad=false;this.mensaje='Especialidad guardada.';this.espForm={nombre:'',descripcion:''};this.editEspId=null;this.cargar();this.cdr.detectChanges();},error:e=>{this.guardandoEspecialidad=false;this.error=e?.error?.mensaje||'No se pudo guardar la especialidad.';this.cdr.detectChanges();}});}
  editarEspecialidad(x:Especialidad):void{this.editEspId=x.id;this.espForm={nombre:x.nombre,descripcion:x.descripcion||''};}
  activarEspecialidad(x:Especialidad):void{this.api.estadoEspecialidad(x.id,!x.activa).subscribe({next:()=>{this.mensaje='Estado de especialidad actualizado.';this.cargar();this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo cambiar el estado.';this.cdr.detectChanges();}});}
  guardarTarifa():void{if(this.guardandoTarifa)return;this.error='';this.mensaje='';this.guardandoTarifa=true;this.cdr.detectChanges();const op=this.api.guardar(this.tarifaForm,this.editTarifaId||undefined);op.subscribe({next:()=>{this.guardandoTarifa=false;this.mensaje='Tarifa guardada y cambio registrado en el historial.';this.nuevaTarifa();this.cargar();this.cdr.detectChanges();},error:e=>{this.guardandoTarifa=false;this.error=e?.error?.mensaje||'No se pudo guardar la tarifa.';this.cdr.detectChanges();}});}
  nuevaTarifa():void{this.editTarifaId=null;this.tarifaForm={codigo:'',nombre:'',descripcion:'',tipoServicio:'CONSULTA',especialidadId:null,valor:50000,ivaPorcentaje:0,activa:true,usuario:'administrador'};}
  editarTarifa(x:Tarifa):void{this.editTarifaId=x.id;this.tarifaForm={codigo:x.codigo,nombre:x.nombre,descripcion:x.descripcion||'',tipoServicio:x.tipoServicio,especialidadId:x.especialidad?.id||null,valor:x.valor,ivaPorcentaje:x.ivaPorcentaje,activa:x.activa,usuario:'administrador'};}
  estadoTarifa(x:Tarifa):void{this.api.estado(x.id,!x.activa).subscribe({next:()=>{this.cargar();this.mensaje='Estado actualizado y auditado.';this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo actualizar.';this.cdr.detectChanges();}});}
  verHistorial(x:Tarifa):void{this.logs=[];this.api.historial(x.id).subscribe({next:l=>{this.logs=l;this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo cargar el historial.';this.cdr.detectChanges();}});}
  cancelarEdicion():void{this.nuevaTarifa();}
  cambiarVeterinario():void{this.asignacion.especialidadId=this.veterinarios.find(v=>v.id===Number(this.asignacion.veterinarioId))?.especialidad?.id||0;}
  private veterinariosSinDuplicados(veterinarios:VeterinarioCita[]):VeterinarioCita[]{
    const unicos=new Map<string,VeterinarioCita>();
    for(const veterinario of veterinarios.filter(v=>v.activo)){
      const nombre=`${veterinario.nombre} ${veterinario.apellido}`.normalize('NFD').replace(/[\u0300-\u036f]/g,'').trim().replace(/\s+/g,' ').toLocaleLowerCase();
      const existente=unicos.get(nombre);
      if(!existente||(!existente.especialidad&&veterinario.especialidad))unicos.set(nombre,veterinario);
    }
    return [...unicos.values()];
  }
  asignar():void{this.consultas.asignarEspecialidad(this.asignacion.veterinarioId,this.asignacion.especialidadId).subscribe({next:()=>{this.mensaje='Especialidad del veterinario actualizada.';this.consultas.veterinarios().subscribe({next:v=>{this.veterinarios=this.veterinariosSinDuplicados(v);this.asignacion.especialidadId=this.veterinarios.find(x=>x.id===this.asignacion.veterinarioId)?.especialidad?.id||this.asignacion.especialidadId;this.cdr.detectChanges();},error:()=>this.cdr.detectChanges()});this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo asignar la especialidad.';this.cdr.detectChanges();}});}
  guardarClinica():void{this.api.guardarDatosClinica(this.clinica).subscribe({next:d=>{this.clinica=d;this.mensaje='Datos de la clínica guardados y aparecerán en las facturas.';this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudieron guardar los datos de la clínica.';this.cdr.detectChanges();}});}
  seleccionarProductoIva():void{this.productoIva.porcentaje=this.productos.find(p=>p.id===Number(this.productoIva.id))?.ivaPorcentaje||0;}
  guardarIvaProducto():void{this.inventario.actualizarIva(this.productoIva.id,Number(this.productoIva.porcentaje)).subscribe({next:p=>{this.productos=this.productos.map(x=>x.id===p.id?{...x,ivaPorcentaje:this.productoIva.porcentaje}:x);this.mensaje='IVA del producto actualizado; se aplicará a dispensaciones futuras.';this.cdr.detectChanges();},error:e=>{this.error=e?.error?.mensaje||'No se pudo actualizar el IVA del producto.';this.cdr.detectChanges();}});}
}
