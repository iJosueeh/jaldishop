import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsAuditTab } from './admin-settings-audit-tab';
import { AdminService } from '../../../../../core/services/admin.service';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminAuditLog } from '../../../../../core/models/admin.models';

describe('AdminSettingsAuditTab', () => {
  let component: AdminSettingsAuditTab;
  let fixture: ComponentFixture<AdminSettingsAuditTab>;
  let adminServiceMock: any;

  const mockLogs: AdminAuditLog[] = [
    {
      id: 'log-1-uuid-1234',
      createdAt: '2026-11-23T10:00:00Z',
      actorId: 'admin-user-id-1',
      actorEmail: 'superadmin@jaldishop.com',
      action: 'SUSPEND_STORE',
      targetType: 'STORE',
      targetId: 'store-uuid-abc',
      status: 'SUCCESS',
      ipAddress: '192.168.1.100',
      details: { reason: 'Violación de políticas comerciales' },
    },
    {
      id: 'log-2-uuid-5678',
      createdAt: '2026-11-23T10:15:00Z',
      actorId: 'admin-user-id-2',
      actorEmail: 'admin2@jaldishop.com',
      action: 'PROMOTE_ADMIN',
      targetType: 'USER',
      targetId: 'user-uuid-def',
      status: 'FAILURE',
      ipAddress: '192.168.1.101',
      details: { error: 'MERCHANT_PROMOTION_RESTRICTED' },
    },
  ];

  beforeEach(async () => {
    adminServiceMock = {
      auditLogs: signal<AdminAuditLog[]>(mockLogs),
      isLoadingAuditLogs: signal<boolean>(false),
      loadAuditLogs: vi.fn().mockReturnValue(of({ content: mockLogs, totalElements: 2, totalPages: 1, size: 30, number: 0 })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminSettingsAuditTab],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsAuditTab);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente y listar los logs de auditoría', () => {
    expect(component).toBeTruthy();
    expect(component.auditLogs().length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('superadmin@jaldishop.com');
    expect(compiled.textContent).toContain('SUSPEND_STORE');
    expect(compiled.textContent).toContain('ÉXITO');
    expect(compiled.textContent).toContain('FALLO');
  });

  it('debe abrir y cerrar el modal de inspección de detalles', () => {
    expect(component.selectedLog()).toBeNull();

    component.inspectLog(mockLogs[0]);
    fixture.detectChanges();

    expect(component.selectedLog()?.id).toBe('log-1-uuid-1234');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Detalle de Auditoría Transaccional');
    expect(compiled.textContent).toContain('Violación de políticas comerciales');

    component.closeModal();
    fixture.detectChanges();

    expect(component.selectedLog()).toBeNull();
  });

  it('debe llamar a loadAuditLogs al refrescar', () => {
    component.refreshLogs();
    expect(adminServiceMock.loadAuditLogs).toHaveBeenCalledWith({ page: 0, size: 30 });
  });
});
