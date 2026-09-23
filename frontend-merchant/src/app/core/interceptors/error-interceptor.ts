import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ErrorHandlerService } from '../services/error-handler.service';
import { ToastService } from '../services/toast.service';
import { TokenService } from '../services/token-service';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorHandler = inject(ErrorHandlerService);
  const toastService = inject(ToastService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      const normalizedError = errorHandler.normalize(error);

      if (normalizedError.status === 401 && !req.url.includes('/auth/login')) {
        tokenService.removeToken();
        toastService.warning('Tu sesión ha expirado. Ingresa nuevamente.', 'Sesión expirada');
        router.navigate(['/login'], { queryParams: { expired: 'true' } });
      } else if (normalizedError.status === 403) {
        toastService.error(
          'No tienes permisos suficientes para realizar esta acción.',
          'Acceso denegado',
        );
      } else if (normalizedError.status === 0) {
        toastService.error('No hay conexion con el servidor. Revisa tu red.', 'Sin conexion');
      } else if (normalizedError.status === 500) {
        toastService.error('Ocurrio un error inesperado en el servidor.', 'Error interno');
      }

      return throwError(() => normalizedError);
    }),
  );
};
