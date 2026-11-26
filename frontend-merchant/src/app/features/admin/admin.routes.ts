import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/admin-dashboard').then((m) => m.AdminDashboard),
  },
  {
    path: 'analytics',
    loadComponent: () =>
      import('./analytics/admin-analytics').then((m) => m.AdminAnalytics),
  },
  {
    path: 'reconciliation',
    loadComponent: () =>
      import('./reconciliation/admin-reconciliation').then(
        (m) => m.AdminReconciliation
      ),
  },
  {
    path: 'promo-codes',
    loadComponent: () =>
      import('./promo-codes/admin-promo-codes').then((m) => m.AdminPromoCodes),
  },
  {
    path: 'users',
    loadComponent: () => import('./users/admin-users').then((m) => m.AdminUsers),
  },
  {
    path: 'stores',
    loadComponent: () => import('./stores/admin-stores').then((m) => m.AdminStores),
  },
  {
    path: 'merchants',
    loadComponent: () =>
      import('./merchants/admin-merchants').then((m) => m.AdminMerchants),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./settings/admin-settings').then((m) => m.AdminSettings),
  },
  {
    path: 'audit-logs',
    loadComponent: () =>
      import('./audit-logs/admin-audit-logs').then((m) => m.AdminAuditLogs),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
];
