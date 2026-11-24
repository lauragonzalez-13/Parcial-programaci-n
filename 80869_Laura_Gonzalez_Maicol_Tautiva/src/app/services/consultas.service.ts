import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoConsulta = 'PENDIENTE' | 'CONFIRMADA' | 'EN_ATENCION' | 'ATENDIDA' | 'CANCELADA' | 'PROGRAMADA';
export interface ClienteCita { id: number; nombre: string; apellido: string; activo: boolean; }
export interface MascotaCita { id: number; nombre: string; especie: string; activa: boolean; cliente: ClienteCita; }
export interface VeterinarioCita { id: number; nombre: string; apellido: string; activo: boolean; especialidad?: { id: number; nombre: string; activa?: boolean }; }
export interface CrearVeterinarioRequest { nombre:string; apellido:string; documento:string; tarjetaProfesional?:string; correo?:string; }
export interface ConsultaCita {
  id: number; fechaHora: string; motivo: string; diagnostico?: string; activa: boolean;
  estado: EstadoConsulta; duracionMinutos?: number; mascota: MascotaCita; veterinario: VeterinarioCita;
}
export interface ConsultaRequest {
  clienteId: number; mascotaId: number; veterinarioId: number;
  fechaHora: string; motivo: string; estado: EstadoConsulta; duracionMinutos?: number;
}
export interface Disponibilidad { id: number; diaSemana: number; horaInicio: string; horaFin: string; activa: boolean; }

@Injectable({ providedIn: 'root' })
export class ConsultasService {
  private readonly api = 'http://localhost:8080/clinica/v1';
  constructor(private http: HttpClient) {}
  listar(): Observable<ConsultaCita[]> { return this.http.get<ConsultaCita[]>(`${this.api}/consultas`); }
  clientes(): Observable<ClienteCita[]> { return this.http.get<ClienteCita[]>(`${this.api}/clientes`); }
  veterinarios(): Observable<VeterinarioCita[]> { return this.http.get<VeterinarioCita[]>(`${this.api}/veterinarios`); }
  crearVeterinario(datos: CrearVeterinarioRequest): Observable<VeterinarioCita> { return this.http.post<VeterinarioCita>(`${this.api}/veterinarios`, datos); }
  asignarEspecialidad(veterinarioId:number,especialidadId:number):Observable<VeterinarioCita>{return this.http.put<VeterinarioCita>(`${this.api}/veterinarios/${veterinarioId}/especialidad`,{especialidadId});}
  mascotas(clienteId: number): Observable<MascotaCita[]> { return this.http.get<MascotaCita[]>(`${this.api}/clientes/${clienteId}/mascotas`); }
  crear(datos: ConsultaRequest): Observable<ConsultaCita> { return this.http.post<ConsultaCita>(`${this.api}/consultas`, datos); }
  actualizar(id: number, datos: ConsultaRequest): Observable<ConsultaCita> { return this.http.put<ConsultaCita>(`${this.api}/consultas/${id}`, datos); }
  cancelar(id: number): Observable<ConsultaCita> { return this.http.patch<ConsultaCita>(`${this.api}/consultas/${id}/cancelar`, {}); }
  atender(id: number): Observable<ConsultaCita> { return this.http.patch<ConsultaCita>(`${this.api}/consultas/${id}/atender`, {}); }
  porVeterinario(id: number, desde: string, hasta: string): Observable<ConsultaCita[]> { return this.http.get<ConsultaCita[]>(`${this.api}/consultas/veterinario/${id}`, { params: { desde, hasta } }); }
  buscar(id: number): Observable<ConsultaCita> { return this.http.get<ConsultaCita>(`${this.api}/consultas/${id}`); }
  historialMascota(id: number): Observable<ConsultaCita[]> { return this.http.get<ConsultaCita[]>(`${this.api}/consultas/mascota/${id}/historial`); }
  reporteVeterinario(id: number, desde: string, hasta: string): Observable<Blob> { return this.http.get(`${this.api}/consultas/reportes/veterinario/${id}`, { params: { desde, hasta }, responseType: 'blob' }); }
  disponibilidad(id: number): Observable<Disponibilidad[]> { return this.http.get<Disponibilidad[]>(`${this.api}/veterinarios/${id}/disponibilidad`); }
  guardarDisponibilidad(id: number, horario: { diaSemana: number; horaInicio: string; horaFin: string }): Observable<Disponibilidad> { return this.http.post<Disponibilidad>(`${this.api}/veterinarios/${id}/disponibilidad`, horario); }
  borrarDisponibilidad(id: number, horarioId: number): Observable<void> { return this.http.delete<void>(`${this.api}/veterinarios/${id}/disponibilidad/${horarioId}`); }
}
