import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token-service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);

  if (
    req.url.includes('api.cloudinary.com') ||
    req.url.includes('nominatim.openstreetmap.org') ||
    req.url.includes('photon.komoot.io')
  ) {
    return next(req);
  }

  if (tokenService.hasValidToken()) {
    const token = tokenService.getToken();
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(cloned);
  }

  return next(req);
};
