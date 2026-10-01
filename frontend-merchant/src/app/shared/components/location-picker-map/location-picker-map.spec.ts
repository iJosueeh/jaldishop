import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LocationPickerMapComponent } from './location-picker-map';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { of } from 'rxjs';
import { GeocodingResult, GeocodingService } from '../../../core/services/geocoding.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const mockMap = {
  setView: vi.fn().mockReturnThis(),
  panTo: vi.fn().mockReturnThis(),
  flyTo: vi.fn().mockReturnThis(),
  getZoom: vi.fn().mockReturnValue(15),
  on: vi.fn().mockReturnThis(),
  remove: vi.fn(),
  removeLayer: vi.fn(),
  invalidateSize: vi.fn(),
  getCenter: vi.fn().mockReturnValue({ lat: -12.046374, lng: -77.042793 }),
};

const mockMarker = {
  addTo: vi.fn().mockReturnThis(),
  setLatLng: vi.fn().mockReturnThis(),
  getLatLng: vi.fn().mockReturnValue({ lat: -12.046374, lng: -77.042793 }),
  on: vi.fn().mockReturnThis(),
};

const mockCircle = {
  addTo: vi.fn().mockReturnThis(),
};

vi.mock('leaflet', () => ({
  map: vi.fn(() => mockMap),
  tileLayer: vi.fn(() => ({ addTo: vi.fn().mockReturnThis() })),
  control: { zoom: vi.fn(() => ({ addTo: vi.fn().mockReturnThis() })) },
  marker: vi.fn(() => mockMarker),
  circle: vi.fn(() => mockCircle),
  divIcon: vi.fn(() => ({})),
}));

describe('LocationPickerMapComponent', () => {
  let component: LocationPickerMapComponent;
  let fixture: ComponentFixture<LocationPickerMapComponent>;
  let geocodingService: GeocodingService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationPickerMapComponent],
      providers: [GeocodingService, provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(LocationPickerMapComponent);
    component = fixture.componentInstance;
    (component as any).map = mockMap;
    (component as any).marker = mockMarker;
    geocodingService = TestBed.inject(GeocodingService);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe tener inputs con valores por defecto', () => {
    expect(component.latitude()).toBeNull();
    expect(component.longitude()).toBeNull();
    expect(component.radiusKm()).toBeNull();
    expect(component.interactive()).toBe(true);
    expect(component.zoom()).toBe(15);
  });

  it('debe ejecutar la busqueda cuando la consulta tiene al menos 3 caracteres', () => {
    const mockResults: GeocodingResult[] = [
      {
        displayName: 'Av. Larco 450, Miraflores',
        latitude: -12.1215,
        longitude: -77.0298,
        road: 'Av. Larco',
      },
    ];

    vi.spyOn(geocodingService, 'search').mockReturnValue(of(mockResults));

    component.searchQuery.set('Larco');
    component.onSearchSubmit();

    expect(geocodingService.search).toHaveBeenCalledWith('Larco', expect.any(Number), expect.any(Number));
    expect(component.showResults()).toBe(true);
  });

  it('debe seleccionar un resultado de busqueda y emitir coordenadas y direccion', () => {
    const coordsSpy = vi.fn();
    const addressSpy = vi.fn();
    component.coordinatesChange.subscribe(coordsSpy);
    component.addressSelected.subscribe(addressSpy);

    const mockResult: GeocodingResult = {
      displayName: 'Av. Larco 450, Miraflores',
      latitude: -12.1215,
      longitude: -77.0298,
      road: 'Av. Larco',
    };

    component.onSelectResult(mockResult);

    expect(coordsSpy).toHaveBeenCalledWith({ lat: -12.1215, lng: -77.0298 });
    expect(addressSpy).toHaveBeenCalledWith(mockResult);
    expect(component.showResults()).toBe(false);
    expect(component.searchQuery()).toBe('Av. Larco 450, Miraflores');
  });

  it('debe recentrar la camara en el pin con recenterOnMarker', () => {
    (component as any).map = mockMap;
    (component as any).marker = mockMarker;
    component.recenterOnMarker();
    expect(mockMap.flyTo).toHaveBeenCalled();
  });

  it('debe cerrar la lista de resultados con closeSearchResults', () => {
    component.showResults.set(true);
    component.closeSearchResults();
    expect(component.showResults()).toBe(false);
  });
});
