import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsSystemTab } from './admin-settings-system-tab';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';

describe('AdminSettingsSystemTab', () => {
  let component: AdminSettingsSystemTab;
  let fixture: ComponentFixture<AdminSettingsSystemTab>;

  const mockSettings: AdminPlatformSettings = {
    allowMerchantRegistration: true,
    requireStoreApproval: false,
    maintenanceMode: false,
    maintenanceNotice: 'Mantenimiento en curso',
    sessionTimeoutHours: 24,
    enforce2FAForAdmins: true,
    notifyOnNewStoreRegistration: true,
    notifyOnStoreSuspension: true,
    adminAlertEmail: 'admin@jaldishop.com',
    platformVersion: '1.2.0',
    environment: 'production',
    marketplaceCommissionRate: 5.5,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSettingsSystemTab],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsSystemTab);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('settings', mockSettings);
    fixture.detectChanges();
  });

  it('debe crearse correctamente y renderizar datos de versión y configuración', () => {
    expect(component).toBeTruthy();
    expect(component.settings().platformVersion).toBe('1.2.0');
    expect(component.settings().marketplaceCommissionRate).toBe(5.5);
  });

  it('debe emitir cambios al alternar modo de mantenimiento', () => {
    const emitSpy = vi.spyOn(component.settingsChange, 'emit');
    component.onToggleMaintenance();
    expect(emitSpy).toHaveBeenCalledWith({ maintenanceMode: true });
  });

  it('debe emitir cambios al alternar registro de comerciantes', () => {
    const emitSpy = vi.spyOn(component.settingsChange, 'emit');
    component.onToggleRegistration();
    expect(emitSpy).toHaveBeenCalledWith({ allowMerchantRegistration: false });
  });

  it('debe emitir cambios al actualizar la tasa de comisión', () => {
    const emitSpy = vi.spyOn(component.settingsChange, 'emit');
    const event = { target: { value: '7.5' } } as unknown as Event;
    component.onCommissionChange(event);
    expect(emitSpy).toHaveBeenCalledWith({ marketplaceCommissionRate: 7.5 });
  });

  it('debe emitir cambios al actualizar el tiempo de expiración de sesión', () => {
    const emitSpy = vi.spyOn(component.settingsChange, 'emit');
    const event = { target: { value: '48' } } as unknown as Event;
    component.onSessionTimeoutChange(event);
    expect(emitSpy).toHaveBeenCalledWith({ sessionTimeoutHours: 48 });
  });
});
