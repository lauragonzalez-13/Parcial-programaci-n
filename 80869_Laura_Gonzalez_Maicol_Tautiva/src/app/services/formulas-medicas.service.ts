
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface Mascota {
  id: number;
  nombre: string;
  especie: string;
  raza: string;
  edad: number;
  activa: boolean;
  cliente: {
    id: number;
    nombre: string;
    apellido: string;
    activo: boolean;
  };
}

export interface Consulta {
  id: number;
  fechaHora: string;
  diagnostico: string;
  motivo: string;
  activa: boolean;
  estado: 'PENDIENTE' | 'CONFIRMADA' | 'ATENDIDA' | 'CANCELADA' | 'PROGRAMADA';
  mascota: {
    id: number;
  };
  veterinario: {
    id: number;
    nombre: string;
    apellido: string;
    activo: boolean;
  };
}

export interface FormulaMedica {
  id: number;
  fechaEmision: string;
  medicamento: string;
  dosis: string;
  frecuencia: string;
  duracionDias: number;
  indicaciones: string;
  viaAdministracion?: string;
  activa: boolean;
  mascota: { id: number; nombre?: string; especie?: string };
  cliente: { id: number; nombre?: string; apellido: string };
  veterinario: { id: number; nombre?: string; apellido: string };
  consulta: { id: number; fechaHora?: string };
  medicamentoInventario?: { id: number; codigo: string; nombre: string };
  cantidadRequerida?: number;
  unidadesPorToma?: number;
  tomasPorDia?: number;
  cantidadDispensada?: number;
  estadoDispensacion?: 'PENDIENTE' | 'PARCIAL' | 'DISPENSADA';
}

export interface NuevaFormulaMedica {
  fechaEmision: string;
  medicamento: string;
  dosis: string;
  frecuencia: string;
  viaAdministracion?: string;
  duracionDias: number;
  indicaciones: string;
  mascota: { id: number };
  consulta: { id: number };
  medicamentoInventario: { id: number };
  cantidadRequerida: number;
  unidadesPorToma: number;
  tomasPorDia: number;
}

@Injectable({ providedIn: 'root' })
export class FormulasMedicasService {
  private apiUrl =
    'http://localhost:8080/clinica/v1';

  constructor(private http: HttpClient) { }

  listar(): Observable<FormulaMedica[]> {
    return this.http.get<FormulaMedica[]>(
      `${this.apiUrl}/formulas-medicas`
    );
  }
  listarPorConsulta(id: number): Observable<FormulaMedica[]> { return this.http.get<FormulaMedica[]>(`${this.apiUrl}/formulas-medicas/consulta/${id}`); }
  descargarPdf(id: number): Observable<Blob> { return this.http.get(`${this.apiUrl}/atencion-medica/formulas/${id}.pdf`, { responseType: 'blob' }); }

  listarMascotas(): Observable<Mascota[]> {
    return this.http.get<Mascota[]>(
      `${this.apiUrl}/mascotas`
    );
  }

  listarConsultas(): Observable<Consulta[]> {
    return this.http.get<Consulta[]>(
      `${this.apiUrl}/consultas`
    );
  }

  guardar(
    formula: NuevaFormulaMedica
  ): Observable<FormulaMedica> {
    return this.http.post<FormulaMedica>(
      `${this.apiUrl}/formulas-medicas`,
      formula
    );
  }

  actualizar(id: number, formula: NuevaFormulaMedica) {
    return this.http.put<FormulaMedica>(
      `${this.apiUrl}/formulas-medicas/${id}`,
      formula
    );
  }

  desactivar(id: number) {
    return this.http.patch<FormulaMedica>(
      `${this.apiUrl}/formulas-medicas/${id}/desactivar`,
      {}
    );
  }

  dispensar(id: number, usuarioResponsable: string) {
    return this.http.patch<FormulaMedica>(`${this.apiUrl}/formulas-medicas/${id}/dispensar`, { usuarioResponsable });
  }
}
