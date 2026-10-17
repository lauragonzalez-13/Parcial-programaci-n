import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClienteCita, ConsultaCita, MascotaCita } from './consultas.service';

export interface HistoriaClinica { id: number; fechaCreacion: string; mascota: MascotaCita; }
export interface AnotacionClinica {
  id: number; fechaHora: string; descripcion: string; observacion?: string; diagnostico?: string;
  historiaClinica: HistoriaClinica; consulta: ConsultaCita; veterinario: { id: number; nombre: string; apellido: string };
}
export interface HistoriaRequest {
  clienteId: number; mascotaId: number; consultaId: number;
  descripcion: string; observacion: string; diagnostico: string;
}
export interface AnotacionRequest { consultaId: number; descripcion: string; observacion: string; diagnostico: string; }

@Injectable({ providedIn: 'root' })
export class HistoriasClinicasService {
  private readonly api = 'http://localhost:8080/clinica/v1';
  constructor(private http: HttpClient) {}
  listar(): Observable<AnotacionClinica[]> { return this.http.get<AnotacionClinica[]>(`${this.api}/historias-clinicas`); }
  buscar(id: number): Observable<HistoriaClinica> { return this.http.get<HistoriaClinica>(`${this.api}/historias-clinicas/${id}`); }
  crear(datos: HistoriaRequest): Observable<HistoriaClinica> { return this.http.post<HistoriaClinica>(`${this.api}/historias-clinicas`, datos); }
  anotaciones(historiaId: number): Observable<AnotacionClinica[]> { return this.http.get<AnotacionClinica[]>(`${this.api}/historias-clinicas/${historiaId}/anotaciones`); }
  detalleAnotacion(historiaId: number, anotacionId: number): Observable<AnotacionClinica> { return this.http.get<AnotacionClinica>(`${this.api}/historias-clinicas/${historiaId}/anotaciones/${anotacionId}`); }
  agregarAnotacion(historiaId: number, datos: AnotacionRequest): Observable<AnotacionClinica> { return this.http.post<AnotacionClinica>(`${this.api}/historias-clinicas/${historiaId}/anotaciones`, datos); }
  clientes(): Observable<ClienteCita[]> { return this.http.get<ClienteCita[]>(`${this.api}/clientes`); }
  mascotas(clienteId: number): Observable<MascotaCita[]> { return this.http.get<MascotaCita[]>(`${this.api}/clientes/${clienteId}/mascotas`); }
  citas(): Observable<ConsultaCita[]> { return this.http.get<ConsultaCita[]>(`${this.api}/consultas`); }
}
