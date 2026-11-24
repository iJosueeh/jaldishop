import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SettingsService, DEFAULT_SETTINGS } from './settings.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';
import { MerchantSettings } from '../models/settings.models';

describe('SettingsService', () => {
  let service: SettingsService;
  let httpTesting: HttpTestingController;
  let mockToastService: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let mockBusService: { dispatchCacheInvalidated: ReturnType<typeof vi.fn> };

  const testUrl = `${environment.apiUrl}/api/v1/stores/me/settings`;

  beforeEach(() => {
    mockToastService = {
      success: vi.fn(),
      error: vi.fn(),
    };

    mockBusService = {
      dispatchCacheInvalidated: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        SettingsService,
        { provide: ToastService, useValue: mockToastService },
        { provide: InvalidationBusService, useValue: mockBusService },
      ],
    });

    service = TestBed.inject(SettingsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe inicializarse con DEFAULT_SETTINGS', () => {
    expect(service.settings()).toEqual(DEFAULT_SETTINGS);
  });

  it('debe cargar configuración vía GET /api/v1/stores/me/settings', () => {
    const mockData: MerchantSettings = {
      ...DEFAULT_SETTINGS,
      soundVolume: 95,
      minDeliveryAmount: 25.5,
    };

    service.loadSettings();
    expect(service.isLoading()).toBe(true);

    const req = httpTesting.expectOne(testUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockData);

    expect(service.isLoading()).toBe(false);
    expect(service.settings().soundVolume).toBe(95);
    expect(service.settings().minDeliveryAmount).toBe(25.5);
  });

  it('debe guardar configuración optimista, llamar PUT y disparar InvalidationBus', () => {
    const newSettings: MerchantSettings = {
      ...DEFAULT_SETTINGS,
      requireCustomerDni: true,
      estimatedPrepTimeMinutes: 45,
    };

    service.saveSettings(newSettings, 'store-123');

    // Optimista
    expect(service.settings().requireCustomerDni).toBe(true);
    expect(service.isSaving()).toBe(true);

    const req = httpTesting.expectOne(testUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(newSettings);
    req.flush(newSettings);

    expect(service.isSaving()).toBe(false);
    expect(mockToastService.success).toHaveBeenCalledWith('Configuración guardada correctamente.');
    expect(mockBusService.dispatchCacheInvalidated).toHaveBeenCalledWith({
      storeId: 'store-123',
      module: 'STORE',
    });
  });

  it('debe hacer rollback al estado anterior si la llamada PUT falla', () => {
    const originalSettings = { ...service.settings() };
    const failingSettings: MerchantSettings = {
      ...DEFAULT_SETTINGS,
      soundVolume: 20,
    };

    service.saveSettings(failingSettings, 'store-123');
    expect(service.settings().soundVolume).toBe(20);

    const req = httpTesting.expectOne(testUrl);
    req.flush('Error de servidor', { status: 500, statusText: 'Internal Server Error' });

    expect(service.isSaving()).toBe(false);
    expect(service.settings().soundVolume).toBe(originalSettings.soundVolume);
    expect(mockToastService.error).toHaveBeenCalled();
  });

  it('debe aplicar throttling de 2 segundos a la reproducción de alertas sonoras', () => {
    service.updateSettingsLocally({ orderSoundAlert: true });

    let audioContextCount = 0;
    class MockAudioContext {
      currentTime = 0;
      destination = {};
      createOscillator = vi.fn().mockReturnValue({
        type: 'sine',
        frequency: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
        connect: vi.fn(),
        start: vi.fn(),
        stop: vi.fn(),
      });
      createGain = vi.fn().mockReturnValue({
        gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
        connect: vi.fn(),
      });
      constructor() {
        audioContextCount++;
      }
    }
    vi.stubGlobal('AudioContext', MockAudioContext);

    service.playNotificationSound();
    service.playNotificationSound();
    service.playNotificationSound();

    expect(audioContextCount).toBe(1);
    vi.unstubAllGlobals();
  });
});
