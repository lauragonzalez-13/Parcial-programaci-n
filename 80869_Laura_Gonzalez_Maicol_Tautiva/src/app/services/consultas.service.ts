import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoConsulta = 'PENDIENTE' | 'CONFIRMADA' | 'ATENDIDA' | 'CANCELADA' | 'PROGRAMADA';
export interface ClienteCita { id: number; nombre: string; apellido: string; activo: boolean; }
export interface MascotaCita { id: number; nombre: string; especie: string; activa: boolean; cliente: ClienteCita; }
export interface VeterinarioCita { id: number; nombre: string; apellido: string; activo: boolean; }
export interface ConsultaCita {
  id: number; fechaHora: string; motivo: string; diagnostico?: string; activa: boolean;
  estado: EstadoConsulta; mascota: MascotaCita; veterinario: VeterinarioCita;
}
export interface ConsultaRequest {
  clienteId: number; mascotaId: number; veterinarioId: number;
  fechaHora: string; motivo: string; estado: EstadoConsulta;
}

@Injectable({ providedIn: 'root' })
export class ConsultasService {
  private readonly api = 'http://localhost:8080/clinica/v1';
  constructor(private http: HttpClient) {}
  listar(): Observable<ConsultaCita[]> { return this.http.get<ConsultaCita[]>(`${this.api}/consultas`); }
  clientes(): Observable<ClienteCita[]> { return this.http.get<ClienteCita[]>(`${this.api}/clientes`); }
  veterinarios(): Observable<VeterinarioCita[]> { return this.http.get<VeterinarioCita[]>(`${this.api}/veterinarios`); }
  mascotas(clienteId: number): Observable<MascotaCita[]> { return this.http.get<MascotaCita[]>(`${this.api}/clientes/${clienteId}/mascotas`); }
  crear(datos: ConsultaRequest): Observable<ConsultaCita> { return this.http.post<ConsultaCita>(`${this.api}/consultas`, datos); }
  actualizar(id: number, datos: ConsultaRequest): Observable<ConsultaCita> { return this.http.put<ConsultaCita>(`${this.api}/consultas/${id}`, datos); }
  cancelar(id: number): Observable<ConsultaCita> { return this.http.patch<ConsultaCita>(`${this.api}/consultas/${id}/cancelar`, {}); }
  atender(id: number): Observable<ConsultaCita> { return this.http.patch<ConsultaCita>(`${this.api}/consultas/${id}/atender`, {}); }
}
