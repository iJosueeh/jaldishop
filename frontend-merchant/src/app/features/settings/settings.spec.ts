import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Settings } from './settings';
import { SettingsService } from '../../core/services/settings.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('Settings Component', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;
  let mockSettingsService: any;

  beforeEach(async () => {
    mockSettingsService = {
      settings: vi.fn().mockReturnValue({
        cancellationPolicy: 'Política demo',
        refundPolicy: 'Reembolso demo',
        requireCustomerDni: false,
        allowOrderNotes: true,
        onlinePaymentsEnabled: true,
        cashOnDeliveryEnabled: true,
        acceptsPosOnDelivery: false,
        minDeliveryAmount: null,
        estimatedPrepTimeMinutes: 30,
        orderSoundAlert: true,
        soundVolume: 80,
        lowStockVisualAlert: true,
      }),
      loadSettings: vi.fn(),
      saveSettings: vi.fn(),
      isSaving: vi.fn().mockReturnValue(false),
      isLoading: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: SettingsService, useValue: mockSettingsService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e iniciar en la pestaña de payments (Cobros y Despacho)', () => {
    expect(component).toBeTruthy();
    expect(component.currentTab()).toBe('payments');
    expect(mockSettingsService.loadSettings).toHaveBeenCalled();
  });

  it('debe cambiar de tab reactivamente al invocar setTab', () => {
    component.setTab('policies');
    expect(component.currentTab()).toBe('policies');

    component.setTab('alerts');
    expect(component.currentTab()).toBe('alerts');
  });
});
