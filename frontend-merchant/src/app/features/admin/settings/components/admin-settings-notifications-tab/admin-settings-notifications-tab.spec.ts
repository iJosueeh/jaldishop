import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSettingsNotificationsTab } from './admin-settings-notifications-tab';
import { AdminPlatformSettings } from '../../../../../core/models/admin.models';

describe('AdminSettingsNotificationsTab', () => {
  let component: AdminSettingsNotificationsTab;
  let fixture: ComponentFixture<AdminSettingsNotificationsTab>;

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
      imports: [AdminSettingsNotificationsTab],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsNotificationsTab);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('settings', mockSettings);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir cambios al alternar notificación de nueva tienda', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleNotifyNewStore();
    expect(spy).toHaveBeenCalledWith({ notifyOnNewStoreRegistration: false });
  });

  it('debe emitir cambios al alternar notificación de suspensión', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    component.toggleNotifySuspension();
    expect(spy).toHaveBeenCalledWith({ notifyOnStoreSuspension: false });
  });

  it('debe emitir cambios al modificar el email de alertas', () => {
    const spy = vi.spyOn(component.settingsChange, 'emit');
    const inputElement = fixture.nativeElement.querySelector('input');
    inputElement.value = 'security@jaldishop.com';
    inputElement.dispatchEvent(new Event('input'));
    expect(spy).toHaveBeenCalledWith({ adminAlertEmail: 'security@jaldishop.com' });
  });
});
