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
    marketplaceCommissionRate: 5.5,
    version: 1,
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

  it('debe crearse correctamente y renderizar tasa de comisión', () => {
    expect(component).toBeTruthy();
    expect(component.settings().marketplaceCommissionRate).toBe(5.5);
  });

  it('debe emitir cambios al actualizar la tasa de comisión', () => {
    const emitSpy = vi.spyOn(component.settingsChange, 'emit');
    const event = { target: { value: '7.5' } } as unknown as Event;
    component.onCommissionChange(event);
    expect(emitSpy).toHaveBeenCalledWith({ marketplaceCommissionRate: 7.5 });
  });
});
