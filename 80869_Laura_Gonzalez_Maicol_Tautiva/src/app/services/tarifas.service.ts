import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Especialidad { id: number; nombre: string; descripcion?: string; activa: boolean; }
export interface Tarifa { id: number; codigo: string; nombre: string; descripcion?: string; tipoServicio: string; especialidad?: Especialidad; valor: number; ivaPorcentaje: number; activa: boolean; fechaActualizacion: string; }
export interface LogTarifa { id: number; accion: string; valorAnterior?: number; valorNuevo: number; ivaAnterior?: number; ivaNuevo: number; usuario: string; fecha: string; }
export interface TarifaRequest { codigo: string; nombre: string; descripcion: string; tipoServicio: string; especialidadId: number | null; valor: number; ivaPorcentaje: number; activa: boolean; usuario: string; }
export interface DatosClinica { nombre:string;nit:string;telefono:string;direccion:string;correo:string; }

@Injectable({ providedIn: 'root' })
export class TarifasService {
  private readonly api='http://localhost:8080/clinica/v1/administracion';
  constructor(private http:HttpClient){}
  especialidades():Observable<Especialidad[]>{return this.http.get<Especialidad[]>(`${this.api}/especialidades`);}
  datosClinica():Observable<DatosClinica>{return this.http.get<DatosClinica>(`${this.api}/clinica`);}
  guardarDatosClinica(data:DatosClinica):Observable<DatosClinica>{return this.http.put<DatosClinica>(`${this.api}/clinica`,data);}
  crearEspecialidad(data:{nombre:string;descripcion:string}):Observable<Especialidad>{return this.http.post<Especialidad>(`${this.api}/especialidades`,data);}
  actualizarEspecialidad(id:number,data:{nombre:string;descripcion:string}):Observable<Especialidad>{return this.http.put<Especialidad>(`${this.api}/especialidades/${id}`,data);}
  estadoEspecialidad(id:number,activa:boolean):Observable<Especialidad>{return this.http.patch<Especialidad>(`${this.api}/especialidades/${id}/estado`,{activa});}
  listar():Observable<Tarifa[]>{return this.http.get<Tarifa[]>(`${this.api}/tarifas`);}
  guardar(data:TarifaRequest,id?:number):Observable<Tarifa>{return id?this.http.put<Tarifa>(`${this.api}/tarifas/${id}`,data):this.http.post<Tarifa>(`${this.api}/tarifas`,data);}
  estado(id:number,activa:boolean):Observable<Tarifa>{return this.http.patch<Tarifa>(`${this.api}/tarifas/${id}/estado`,{activa,usuario:'administrador'});}
  historial(id:number):Observable<LogTarifa[]>{return this.http.get<LogTarifa[]>(`${this.api}/tarifas/${id}/historial`);}
}
