import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsPaymentsDeliveryTab } from './settings-payments-delivery-tab';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { provideRouter } from '@angular/router';

describe('SettingsPaymentsDeliveryTab', () => {
  let component: SettingsPaymentsDeliveryTab;
  let fixture: ComponentFixture<SettingsPaymentsDeliveryTab>;
  let mockSettingsService: any;
  let mockStoreService: any;

  beforeEach(async () => {
    mockSettingsService = {
      settings: vi.fn().mockReturnValue({
        cancellationPolicy: 'Demo',
        refundPolicy: 'Demo',
        requireCustomerDni: false,
        allowOrderNotes: true,
        onlinePaymentsEnabled: true,
        cashOnDeliveryEnabled: true,
        acceptsPosOnDelivery: false,
        minDeliveryAmount: 15.0,
        estimatedPrepTimeMinutes: 25,
        orderSoundAlert: true,
        soundVolume: 80,
        lowStockVisualAlert: true,
      }),
      saveSettings: vi.fn(),
      isSaving: vi.fn().mockReturnValue(false),
    };

    mockStoreService = {
      currentStore: vi.fn().mockReturnValue({ id: 'store-test-1' }),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsPaymentsDeliveryTab],
      providers: [
        provideRouter([]),
        { provide: SettingsService, useValue: mockSettingsService },
        { provide: StoreService, useValue: mockStoreService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsPaymentsDeliveryTab);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializarse con los valores de cobro y despacho', () => {
    expect(component.onlinePayments()).toBe(true);
    expect(component.codEnabled()).toBe(true);
    expect(component.prepTimeMinutes()).toBe(25);
    expect(component.minAmount()).toBe(15.0);
  });

  it('debe alternar toggles y guardar correctamente', () => {
    component.onlinePayments.set(false);
    component.codEnabled.set(true);
    component.acceptsPos.set(true);
    component.saveChanges();

    expect(mockSettingsService.saveSettings).toHaveBeenCalledWith(
      expect.objectContaining({
        onlinePaymentsEnabled: false,
        cashOnDeliveryEnabled: true,
        acceptsPosOnDelivery: true,
      }),
      'store-test-1'
    );
  });

  it('debe bloquear guardado si no hay ningún método de pago habilitado', () => {
    component.onlinePayments.set(false);
    component.codEnabled.set(false);

    expect(component.noPaymentMethods()).toBe(true);
    expect(component.hasErrors()).toBe(true);

    component.saveChanges();
    expect(mockSettingsService.saveSettings).not.toHaveBeenCalled();
  });

  it('debe validar que el monto mínimo sea mayor a cero si está activo', () => {
    component.hasMinAmount.set(true);
    component.minAmount.set(0);

    expect(component.invalidMinAmount()).toBe(true);
    expect(component.hasErrors()).toBe(true);

    component.saveChanges();
    expect(mockSettingsService.saveSettings).not.toHaveBeenCalled();
  });

  it('debe validar rango permitido de tiempo de preparación (5 a 480 min)', () => {
    component.prepTimeMinutes.set(3);
    expect(component.invalidPrepTime()).toBe(true);
    expect(component.hasErrors()).toBe(true);

    component.prepTimeMinutes.set(500);
    expect(component.invalidPrepTime()).toBe(true);
    expect(component.hasErrors()).toBe(true);

    component.prepTimeMinutes.set(30);
    expect(component.invalidPrepTime()).toBe(false);
  });
});
