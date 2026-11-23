import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsAlertsTab } from './settings-alerts-tab';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';

describe('SettingsAlertsTab', () => {
  let component: SettingsAlertsTab;
  let fixture: ComponentFixture<SettingsAlertsTab>;
  let mockSettingsService: any;
  let mockStoreService: any;

  beforeEach(async () => {
    mockSettingsService = {
      settings: vi.fn().mockReturnValue({
        orderSoundAlert: true,
        soundVolume: 85,
        lowStockVisualAlert: false,
      }),
      saveSettings: vi.fn(),
      playNotificationSound: vi.fn(),
      isSaving: vi.fn().mockReturnValue(false),
    };

    mockStoreService = {
      currentStore: vi.fn().mockReturnValue({ id: 'store-100' }),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsAlertsTab],
      providers: [
        { provide: SettingsService, useValue: mockSettingsService },
        { provide: StoreService, useValue: mockStoreService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsAlertsTab);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializarse con los valores de alertas y volumen', () => {
    expect(component.soundAlert()).toBe(true);
    expect(component.soundVolume()).toBe(85);
    expect(component.lowStockAlert()).toBe(false);
  });

  it('debe llamar a playNotificationSound al probar el sonido', () => {
    component.playTestSound();
    expect(mockSettingsService.playNotificationSound).toHaveBeenCalled();
  });

  it('debe guardar cambios de alertas e invocar el servicio con storeId', () => {
    component.soundAlert.set(false);
    component.soundVolume.set(90);
    component.lowStockAlert.set(true);
    component.saveAlerts();

    expect(mockSettingsService.saveSettings).toHaveBeenCalledWith(
      expect.objectContaining({
        orderSoundAlert: false,
        soundVolume: 90,
        lowStockVisualAlert: true,
      }),
      'store-100'
    );
  });
});
