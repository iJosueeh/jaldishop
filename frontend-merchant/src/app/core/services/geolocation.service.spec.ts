import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { GeolocationService } from './geolocation.service';

describe('GeolocationService', () => {
  let service: GeolocationService;

  beforeEach(() => {
    service = new GeolocationService();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('debe inicializarse con isLocating en false y errorMessage en null', () => {
    expect(service.isLocating()).toBe(false);
    expect(service.errorMessage()).toBeNull();
  });

  it('debe obtener las coordenadas exitosamente cuando navigator.geolocation responde', async () => {
    const mockCoords = {
      latitude: -12.046374,
      longitude: -77.042793,
      accuracy: 15,
    };

    const mockGeolocation = {
      getCurrentPosition: vi.fn((successCallback) => {
        successCallback({
          coords: mockCoords,
        });
      }),
    };

    vi.stubGlobal('navigator', {
      geolocation: mockGeolocation,
    });

    const pos = await service.getCurrentPosition();

    expect(pos.latitude).toBe(-12.046374);
    expect(pos.longitude).toBe(-77.042793);
    expect(pos.accuracy).toBe(15);
    expect(service.isLocating()).toBe(false);
    expect(service.errorMessage()).toBeNull();
  });

  it('debe manejar error cuando el usuario deniega los permisos de geolocalización', async () => {
    const mockGeolocation = {
      getCurrentPosition: vi.fn((_, errorCallback) => {
        errorCallback({
          code: 1, // PERMISSION_DENIED
          PERMISSION_DENIED: 1,
        });
      }),
    };

    vi.stubGlobal('navigator', {
      geolocation: mockGeolocation,
    });

    await expect(service.getCurrentPosition()).rejects.toThrow('Permiso de ubicación denegado por el navegador.');
    expect(service.isLocating()).toBe(false);
    expect(service.errorMessage()).toBe('Permiso de ubicación denegado por el navegador.');
  });

  it('debe manejar error cuando ocurre un timeout al solicitar la ubicación', async () => {
    const mockGeolocation = {
      getCurrentPosition: vi.fn((_, errorCallback) => {
        errorCallback({
          code: 3, // TIMEOUT
          TIMEOUT: 3,
        });
      }),
    };

    vi.stubGlobal('navigator', {
      geolocation: mockGeolocation,
    });

    await expect(service.getCurrentPosition()).rejects.toThrow('El tiempo de espera para obtener la ubicación se ha agotado.');
    expect(service.isLocating()).toBe(false);
    expect(service.errorMessage()).toBe('El tiempo de espera para obtener la ubicación se ha agotado.');
  });
});
