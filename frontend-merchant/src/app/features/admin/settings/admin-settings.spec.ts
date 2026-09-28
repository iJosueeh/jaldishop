import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettings } from './admin-settings';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { signal } from '@angular/core';
import { AdminPlatformSettings } from '../../../core/models/admin.models';

describe('AdminSettings', () => {
  let component: AdminSettings;
  let fixture: ComponentFixture<AdminSettings>;
  let adminServiceMock: any;
  let toastServiceMock: any;

  const initialSettings: AdminPlatformSettings = {
    allowMerchantRegistration: true,
    requireStoreApproval: false,
    maintenanceMode: false,
    maintenanceNotice: 'Mantenimiento programado',
    sessionTimeoutHours: 24,
    enforce2FAForAdmins: true,
    notifyOnNewStoreRegistration: true,
    notifyOnStoreSuspension: true,
    adminAlertEmail: 'admin@jaldishop.com',
    platformVersion: '1.2.0',
    environment: 'production',
  };

  beforeEach(async () => {
    adminServiceMock = {
      settings: signal(initialSettings),
      updateSettings: vi.fn(),
      saveSettings: vi.fn(),
    };

    toastServiceMock = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminSettings],
      providers: [
        { provide: AdminService, useValue: adminServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e iniciar en la pestaña general', () => {
    expect(component).toBeTruthy();
    expect(component.currentTab()).toBe('general');
  });

  it('debe cambiar de pestaña con setTab()', () => {
    component.setTab('security');
    expect(component.currentTab()).toBe('security');

    component.setTab('notifications');
    expect(component.currentTab()).toBe('notifications');

    component.setTab('system');
    expect(component.currentTab()).toBe('system');
  });

  it('debe delegar la actualización de configuración a AdminService', () => {
    component.onUpdateSettings({ allowMerchantRegistration: false });
    expect(adminServiceMock.updateSettings).toHaveBeenCalledWith({ allowMerchantRegistration: false });
  });

  it('debe delegar el guardado de configuración a AdminService', () => {
    component.onSaveSettings();
    expect(adminServiceMock.saveSettings).toHaveBeenCalled();
  });
});
