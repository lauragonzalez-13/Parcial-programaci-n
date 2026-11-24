import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const perfilRutaGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  if (!localStorage.getItem('clinica.token')) return router.createUrlTree(['/login']);
  const perfil = localStorage.getItem('clinica.rol') || '';
  const path = route.routeConfig?.path ?? route.url.map(segment => segment.path).join('/');
  if (perfil === 'DESPACHO_MEDICO') return path === 'despacho-medico' || path === 'inventario' ? true : router.createUrlTree(['/despacho-medico']);
  if (path === 'despacho-medico' && !['ADMINISTRADOR', 'VETERINARIO', 'SECRETARIA'].includes(perfil)) return router.createUrlTree([perfil === 'VETERINARIO' ? '/calendario-medico' : '/citas']);
  const requerido = route.data['perfil'];
  if (!requerido || requerido === 'NO_VETERINARIO' && perfil !== 'VETERINARIO' || perfil === requerido) return true;
  return router.createUrlTree([perfil === 'VETERINARIO' ? '/calendario-medico' : '/citas']);
};
