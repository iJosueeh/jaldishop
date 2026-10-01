import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { GeocodingService } from './geocoding.service';

describe('GeocodingService', () => {
  let service: GeocodingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GeocodingService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(GeocodingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
    expect(service.isSearching()).toBe(false);
    expect(service.results()).toEqual([]);
  });

  it('debe normalizar abreviaciones peruanas y separar números pegados a palabras', () => {
    expect(service.normalizeQuery('C.Coronel Alfonso Ugarte 521Villa María del Triunfo 15818'))
      .toBe('Calle Coronel Alfonso Ugarte 521 Villa María del Triunfo 15818');
    expect(service.normalizeQuery('Av.Larco 450')).toBe('Avenida Larco 450');
    expect(service.normalizeQuery('Jr.Junin 100')).toBe('Jirón Junin 100');
    expect(service.normalizeQuery('VMT')).toBe('Villa María del Triunfo');
  });

  it('no debe realizar peticion si el query tiene menos de 3 caracteres', () => {
    service.search('a').subscribe((res) => {
      expect(res).toEqual([]);
    });

    httpMock.expectNone((req) => req.url.includes('photon') || req.url.includes('nominatim'));
    expect(service.results()).toEqual([]);
  });

  it('debe buscar y mapear los resultados correctamente desde Photon API', () => {
    const mockPhotonResponse = {
      features: [
        {
          geometry: { coordinates: [-77.0298, -12.1215] },
          properties: {
            name: 'Avenida José Larco',
            street: 'Avenida José Larco',
            district: 'Miraflores',
            city: 'Lima',
            country: 'Perú',
          },
        },
      ],
    };

    service.search('Av. Larco 450').subscribe((results) => {
      expect(results.length).toBe(1);
      expect(results[0].displayName).toContain('Avenida José Larco');
      expect(results[0].latitude).toBe(-12.1215);
      expect(results[0].longitude).toBe(-77.0298);
      expect(results[0].road).toBe('Avenida José Larco');
    });

    const req = httpMock.expectOne((r) => r.url.includes('photon.komoot.io/api'));
    expect(req.request.method).toBe('GET');
    req.flush(mockPhotonResponse);

    expect(service.isSearching()).toBe(false);
    expect(service.results().length).toBe(1);
  });

  it('debe hacer fallback a Nominatim si Photon no tiene resultados', () => {
    const mockPhotonEmpty = { features: [] };
    const mockNominatimResponse = [
      {
        display_name: 'Calle Las Begonias, San Isidro, Lima, Perú',
        lat: '-12.095',
        lon: '-77.032',
        address: { road: 'Calle Las Begonias', city: 'Lima' },
      },
    ];

    service.search('Las Begonias').subscribe((results) => {
      expect(results.length).toBe(1);
      expect(results[0].displayName).toBe('Calle Las Begonias, San Isidro, Lima, Perú');
    });

    const reqPhoton = httpMock.expectOne((r) => r.url.includes('photon.komoot.io/api'));
    reqPhoton.flush(mockPhotonEmpty);

    const reqNominatim = httpMock.expectOne((r) => r.url.includes('nominatim.openstreetmap.org/search'));
    reqNominatim.flush(mockNominatimResponse);

    expect(service.isSearching()).toBe(false);
  });

  it('debe realizar reverseGeocode y formatear dirección de forma amigable', () => {
    const mockReverseResponse = {
      display_name: 'Sala Luis Miró Quesada Garland, Avenida José Larco 400, Miraflores, Lima, Perú',
      lat: '-12.1215',
      lon: '-77.0298',
      address: {
        road: 'Avenida José Larco',
        house_number: '400',
        suburb: 'Miraflores',
        city: 'Lima',
      },
    };

    service.reverseGeocode(-12.1215, -77.0298).subscribe((result) => {
      expect(result).not.toBeNull();
      expect(result?.displayName).toBe('Avenida José Larco 400, Miraflores, Lima, Perú');
      expect(result?.road).toBe('Avenida José Larco');
      expect(result?.latitude).toBe(-12.1215);
    });

    const req = httpMock.expectOne((r) => r.url.includes('nominatim.openstreetmap.org/reverse'));
    expect(req.request.method).toBe('GET');
    req.flush(mockReverseResponse);
  });

  it('debe limpiar y deduplicar repeticiones de distritos, provincias y códigos postales', () => {
    const rawWithDuplicates =
      'Jirón Alfonso Ugarte, Tablada de Lurín, Villa María del Triunfo, Lima, Lima Metropolitana, Lima, 15818, Perú';
    const cleaned = service.cleanDisplayName(rawWithDuplicates);

    expect(cleaned).toBe('Jirón Alfonso Ugarte, Tablada de Lurín, Villa María del Triunfo, Lima, Perú');
  });

  it('debe limpiar segmentos vacíos o redundantes correctamente', () => {
    const segments = ['Calle Alfonso Ugarte 521', 'Tablada de Lurín', 'Villa María del Triunfo', 'Lima', 'Lima Metropolitana', null, '15818', 'Perú'];
    const cleaned = service.cleanAddressSegments(segments);

    expect(cleaned).toBe('Calle Alfonso Ugarte 521, Tablada de Lurín, Villa María del Triunfo, Lima, Perú');
  });
});
