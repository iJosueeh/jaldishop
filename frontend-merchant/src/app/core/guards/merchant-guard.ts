import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth-service';

export const merchantGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isMerchant()) {
    return true;
  }

  if (authService.isAdmin()) {
    return router.createUrlTree(['/admin/dashboard']);
  }

  if (authService.isAuthenticated()) {
    return router.createUrlTree(['/register'], { queryParams: { flow: 'open-store' } });
  }

  return router.createUrlTree(['/login']);
};
