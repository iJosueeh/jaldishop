import { ADMIN_ROUTES } from './admin.routes';

describe('ADMIN_ROUTES', () => {
  it('debe tener configurada la ruta audit-logs hacia AdminAuditLogs', async () => {
    const route = ADMIN_ROUTES.find((r) => r.path === 'audit-logs');
    expect(route).toBeDefined();
    expect(route?.loadComponent).toBeDefined();

    const loaded = await (route!.loadComponent as any)();
    expect(loaded).toBeDefined();
    expect(loaded.name).toContain('AdminAuditLogs');
  });

  it('debe tener configurada la ruta settings hacia AdminSettings', async () => {
    const route = ADMIN_ROUTES.find((r) => r.path === 'settings');
    expect(route).toBeDefined();
    expect(route?.loadComponent).toBeDefined();

    const loaded = await (route!.loadComponent as any)();
    expect(loaded).toBeDefined();
    expect(loaded.name).toContain('AdminSettings');
  });
});
