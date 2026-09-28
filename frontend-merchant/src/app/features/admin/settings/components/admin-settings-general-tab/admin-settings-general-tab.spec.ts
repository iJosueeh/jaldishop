import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsGeneralTab } from './admin-settings-general-tab';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';

describe('AdminSettingsGeneralTab', () => {
  let component: AdminSettingsGeneralTab;
  let fixture: ComponentFixture<AdminSettingsGeneralTab>;

  const mockSettings: AdminPlatformSettings = {
    allowMerchantRegistration: true,
    requireStoreApproval: false,
    maintenanceMode: false,
    maintenanceNotice: 'Mantenimiento',
    sessionTimeoutHours: 24,
    enforce2FAForAdmins: true,
    notifyOnNewStoreRegistration: true,
    notifyOnStoreSuspension: true,
    adminAlertEmail: 'admin@jaldishop.com',
    platformVersion: '1.2.0',
    environment: 'production',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSettingsGeneralTab],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsGeneralTab);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('settings', mockSettings);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir cambios al alternar el registro de comerciantes', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleMerchantRegistration();
    expect(spy).toHaveBeenCalledWith({ allowMerchantRegistration: false });
  });

  it('debe emitir cambios al alternar la aprobación de tiendas', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleStoreApproval();
    expect(spy).toHaveBeenCalledWith({ requireStoreApproval: true });
  });

  it('debe emitir cambios al alternar modo mantenimiento', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleMaintenanceMode();
    expect(spy).toHaveBeenCalledWith({ maintenanceMode: true });
  });
});
