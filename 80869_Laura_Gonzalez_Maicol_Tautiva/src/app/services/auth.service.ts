import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs';

export type RolClinica = 'ADMINISTRADOR' | 'SECRETARIA' | 'VETERINARIO' | 'DESPACHO_MEDICO';
export interface LoginResponse { username:string; usuarioId:number; login:boolean; token:string; rol:RolClinica; veterinarioId:number|null; }
export interface ValidarTokenResponse { usuarioId:number|null; token:string; username:string|null; logueoExitoso:boolean; rol:RolClinica|null; veterinarioId:number|null; }
export interface UsuarioSistema { usuarioId:number; username:string; rol:RolClinica; veterinarioId:number|null; activo:boolean; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = 'http://localhost:8080/clinica/v1/auth';
  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.api}/login`, { username, password });
  }
  validateToken(token: string): Observable<ValidarTokenResponse> {
    return this.http.post<ValidarTokenResponse>(`${this.api}/validate-token`, { token });
  }
  usuarios(): Observable<UsuarioSistema[]> { return this.http.get<UsuarioSistema[]>(`${this.api}/usuarios`); }
  crearUsuario(data: {username:string;password:string;rol:RolClinica;veterinarioId:number|null}): Observable<UsuarioSistema> {
    return this.http.post<UsuarioSistema>(`${this.api}/usuarios`, data);
  }
  eliminarUsuario(usuarioId: number): Observable<void> { return this.http.delete<void>(`${this.api}/usuarios/${usuarioId}`); }
  logout(): Observable<void> { return this.http.post<void>(`${this.api}/logout`, {}); }
  guardarSesion(response: LoginResponse | ValidarTokenResponse): void {
    const exito = 'login' in response ? response.login : response.logueoExitoso;
    if (!exito) return;
    const role = response.rol;
    const username = response.username;
    const userId = response.usuarioId;
    if (!role || !username || userId == null || !response.token) return;
    localStorage.setItem('clinica.token', response.token);
    localStorage.setItem('clinica.usuario', username);
    localStorage.setItem('clinica.usuarioId', String(userId));
    localStorage.setItem('clinica.rol', role);
    localStorage.setItem('clinica.perfil', role);
    if (response.veterinarioId != null) localStorage.setItem('clinica.veterinarioId', String(response.veterinarioId));
    else localStorage.removeItem('clinica.veterinarioId');
  }
  get token(): string | null { return localStorage.getItem('clinica.token'); }
  get username(): string { return localStorage.getItem('clinica.usuario') || ''; }
  get rol(): RolClinica | null { return localStorage.getItem('clinica.rol') as RolClinica | null; }
  cerrarSesion(): void {
    const limpiar = () => {
      for (const key of ['clinica.token', 'clinica.usuario', 'clinica.usuarioId', 'clinica.rol', 'clinica.perfil', 'clinica.veterinarioId']) localStorage.removeItem(key);
      void this.router.navigateByUrl('/login');
    };
    if (!this.token) { limpiar(); return; }
    this.logout().pipe(finalize(limpiar)).subscribe({ error: () => undefined });
  }
}
