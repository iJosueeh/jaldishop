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
      imports: [AdminSettingsSystemTab],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSettingsSystemTab);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('settings', mockSettings);
    fixture.detectChanges();
  });

  it('debe crearse correctamente y renderizar datos de versión', () => {
    expect(component).toBeTruthy();
    expect(component.settings().platformVersion).toBe('1.2.0');
  });
});
