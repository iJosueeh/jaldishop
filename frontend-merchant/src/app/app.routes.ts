import { Routes } from '@angular/router';
import { merchantGuard } from './core/guards/merchant-guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'terms',
    loadComponent: () =>
      import('./features/legal/legal').then(
        (m) => m.Legal,
      ),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then(
        (m) => m.ForgotPassword,
      )
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./shared/components/unauthorized/unauthorized').then(
        (m) => m.Unauthorized,
      ),
  },
  {
    path: '404',
    loadComponent: () =>
      import('./shared/components/not-found/not-found').then(
        (m) => m.NotFound,
      ),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layouts/merchant-layout/merchant-layout').then(
        (m) => m.MerchantLayout
      ),
    canActivate: [merchantGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => 
          import('./features/dashboards/dashboards').then((m) => m.Dashboards),
      },
      {
        path: 'store',
        loadComponent: () =>
          import('./features/store/store').then((m) => m.Store),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile').then((m) => m.Profile),
      },
      {
        path: 'capacity',
        loadComponent: () => 
          import('./features/capacity/capacity').then((m) => m.Capacity),
      }
    ]
  },
  {
    path: '**',
    redirectTo: '404',
  },
];
