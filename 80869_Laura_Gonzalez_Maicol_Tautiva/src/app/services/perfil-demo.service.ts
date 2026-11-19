import { Injectable } from '@angular/core';
export type PerfilClinica = 'ADMINISTRADOR' | 'SECRETARIA' | 'VETERINARIO' | 'DESPACHO_MEDICO';
@Injectable({ providedIn: 'root' })
export class PerfilDemoService {
  get perfil(): PerfilClinica { return (localStorage.getItem('clinica.rol') || localStorage.getItem('clinica.perfil') || 'SECRETARIA') as PerfilClinica; }
  set perfil(value: PerfilClinica) { localStorage.setItem('clinica.perfil', value); }
  get veterinarioId(): number { return Number(localStorage.getItem('clinica.veterinarioId') || 0); }
  set veterinarioId(value: number) { localStorage.setItem('clinica.veterinarioId', String(value)); }
}
