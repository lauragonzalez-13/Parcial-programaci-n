import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClienteMascota { id: number; nombre: string; apellido: string; activo: boolean; }
export interface MascotaPaciente {
  id: number; nombre: string; especie: string; raza?: string; sexo?: string;
  edad?: number; activa: boolean; cliente?: ClienteMascota;
}
export interface MascotaRequest {
  nombre: string; especie: string; raza: string; sexo: string;
  edad: number | null; cliente: { id: number };
}

@Injectable({ providedIn: 'root' })
export class MascotasService {
  private readonly api = 'http://localhost:8080/clinica/v1';
  constructor(private http: HttpClient) {}
  listar(): Observable<MascotaPaciente[]> { return this.http.get<MascotaPaciente[]>(`${this.api}/mascotas`); }
  clientes(): Observable<ClienteMascota[]> { return this.http.get<ClienteMascota[]>(`${this.api}/clientes`); }
  crearCliente(nombre: string): Observable<ClienteMascota> { return this.http.post<ClienteMascota>(`${this.api}/clientes`, { nombre }); }
  crear(data: MascotaRequest): Observable<MascotaPaciente> { return this.http.post<MascotaPaciente>(`${this.api}/mascotas`, data); }
  actualizar(id: number, data: MascotaRequest): Observable<MascotaPaciente> { return this.http.put<MascotaPaciente>(`${this.api}/mascotas/${id}`, data); }
  desactivar(id: number): Observable<MascotaPaciente> { return this.http.patch<MascotaPaciente>(`${this.api}/mascotas/${id}/desactivar`, {}); }
}
