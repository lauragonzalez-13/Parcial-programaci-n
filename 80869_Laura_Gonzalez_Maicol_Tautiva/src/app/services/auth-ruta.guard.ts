import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authRutaGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  if (!token) return router.createUrlTree(['/login']);
  return auth.validateToken(token).pipe(
    map(response => {
      if (!response.logueoExitoso) {
        limpiarSesion();
        return router.createUrlTree(['/login']);
      }
      auth.guardarSesion(response);
      return true;
    }),
    catchError(() => {
      limpiarSesion();
      return of(router.createUrlTree(['/login']));
    })
  );
};

function limpiarSesion(): void {
  for (const key of ['clinica.token', 'clinica.usuario', 'clinica.usuarioId', 'clinica.rol', 'clinica.perfil']) localStorage.removeItem(key);
}
