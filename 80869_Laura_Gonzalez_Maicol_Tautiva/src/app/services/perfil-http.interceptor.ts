import { HttpInterceptorFn } from '@angular/common/http';

export const perfilHttpInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('clinica.token');
  const perfil = localStorage.getItem('clinica.rol') || localStorage.getItem('clinica.perfil') || '';
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (perfil) headers['X-Clinica-Perfil'] = perfil;
  return next(request.clone({ setHeaders: headers }));
};
