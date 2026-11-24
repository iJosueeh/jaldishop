import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminAuditLogs } from './admin-audit-logs';
import { AdminService } from '../../../core/services/admin.service';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminAuditLog } from '../../../core/models/admin.models';

describe('AdminAuditLogs', () => {
  let component: AdminAuditLogs;
  let fixture: ComponentFixture<AdminAuditLogs>;
  let mockAdminService: any;

  const sampleLogs: AdminAuditLog[] = [
    {
      id: '1',
      actorId: 'admin-uuid',
      actorEmail: 'admin@jaldishop.com',
      action: 'UPDATE_PLATFORM_SETTINGS',
      targetType: 'PLATFORM_SETTINGS',
      targetId: '1',
      status: 'SUCCESS',
      ipAddress: '192.168.1.1',
      details: { maintenanceMode: true },
      createdAt: '2026-11-23T16:00:00Z',
    },
    {
      id: '2',
      actorId: '',
      actorEmail: '',
      action: 'ACCESS_DENIED',
      targetType: 'ENDPOINT',
      targetId: '',
      status: 'DENIED',
      ipAddress: '127.0.0.1',
      details: '{"method":"GET","uri":"/api/v1/admin/settings"}',
      createdAt: '2026-11-23T16:05:00Z',
    },
  ];

  beforeEach(async () => {
    mockAdminService = {
      auditLogs: signal(sampleLogs),
      totalAuditLogs: signal(2),
      isLoadingAuditLogs: signal(false),
      loadAuditLogs: vi.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAuditLogs],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: mockAdminService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAuditLogs);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente y listar pistas de auditoría', () => {
    expect(component).toBeTruthy();
    expect(component.auditLogs().length).toBe(2);
    expect(component.totalItems()).toBe(2);
  });

  it('debe formatear operador anónimo correctamente', () => {
    const displayAuth = component.getActorDisplay(sampleLogs[0]);
    expect(displayAuth.isAnonymous).toBe(false);
    expect(displayAuth.label).toBe('admin@jaldishop.com');

    const displayAnon = component.getActorDisplay(sampleLogs[1]);
    expect(displayAnon.isAnonymous).toBe(true);
    expect(displayAnon.label).toBe('Anónimo / No autenticado');
  });

  it('debe extraer el endpoint uri cuando targetId está vacío', () => {
    const targetWithId = component.getTargetDisplay(sampleLogs[0]);
    expect(targetWithId.type).toBe('PLATFORM_SETTINGS');
    expect(targetWithId.idOrUri).toBe('1');

    const targetEndpoint = component.getTargetDisplay(sampleLogs[1]);
    expect(targetEndpoint.type).toBe('ENDPOINT');
    expect(targetEndpoint.idOrUri).toBe('/api/v1/admin/settings');
  });

  it('debe paginar y llamar a loadAuditLogs en onPageChange', () => {
    component.onPageChange(2);
    expect(component.currentPage()).toBe(2);
    expect(mockAdminService.loadAuditLogs).toHaveBeenCalledWith({
      page: 1,
      size: 20,
    });
  });

  it('debe abrir y cerrar el modal de inspección con animación de salida diferida', () => {
    vi.useFakeTimers();
    try {
      expect(component.selectedLog()).toBeNull();

      component.inspectLog(sampleLogs[0]);
      expect(component.selectedLog()).toEqual(sampleLogs[0]);
      expect(component.isClosingModal()).toBe(false);

      component.closeModal();
      expect(component.isClosingModal()).toBe(true);
      expect(component.selectedLog()).toEqual(sampleLogs[0]);

      vi.advanceTimersByTime(180);
      expect(component.selectedLog()).toBeNull();
      expect(component.isClosingModal()).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('debe cerrar el modal con salida diferida al pulsar la tecla Escape', () => {
    vi.useFakeTimers();
    try {
      component.inspectLog(sampleLogs[0]);
      expect(component.selectedLog()).not.toBeNull();

      component.onEscapePressed();
      expect(component.isClosingModal()).toBe(true);

      vi.advanceTimersByTime(180);
      expect(component.selectedLog()).toBeNull();
      expect(component.isClosingModal()).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });
});
