import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StoreService } from '../../features/store/services/store.service';
import { map } from 'rxjs';

export const hasStoreGuard: CanActivateFn = () => {
  const storeService = inject(StoreService);
  const router = inject(Router);

  if (storeService.hasStore()) {
    return true;
  }

  return storeService.getMyStore().pipe(
    map(store => {
      if (store) {
        return true;
      }
      return router.createUrlTree(['/onboarding']);
    })
  );
};

export const noStoreGuard: CanActivateFn = () => {
  const storeService = inject(StoreService);
  const router = inject(Router);

  if (storeService.hasStore()) {
    return router.createUrlTree(['mi-tienda']);
  }

  return storeService.getMyStore().pipe(
    map(store => {
      if (!store) {
        return true;
      }
      return router.createUrlTree(['/mi-tienda'])
    })
  )
}