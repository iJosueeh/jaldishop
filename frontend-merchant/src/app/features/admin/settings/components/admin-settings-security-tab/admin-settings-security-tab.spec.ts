import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsSecurityTab } from './admin-settings-security-tab';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';

describe('AdminSettingsSecurityTab', () => {
  let component: AdminSettingsSecurityTab;
  let fixture: ComponentFixture<AdminSettingsSecurityTab>;

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
      imports: [AdminSettingsSecurityTab],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsSecurityTab);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('settings', mockSettings);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir cambios al alternar el 2FA obligatorio', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleEnforce2FA();
    expect(spy).toHaveBeenCalledWith({ enforce2FAForAdmins: false });
  });

  it('debe emitir cambios al actualizar la expiración de sesión', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    const selectElement = fixture.nativeElement.querySelector('select');
    selectElement.value = '12';
    selectElement.dispatchEvent(new Event('change'));
    expect(spy).toHaveBeenCalledWith({ sessionTimeoutHours: 12 });
  });
});
