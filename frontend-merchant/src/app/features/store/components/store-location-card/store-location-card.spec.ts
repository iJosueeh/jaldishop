import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreLocationCard } from './store-location-card';
import { FormControl, FormGroup } from '@angular/forms';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { GeolocationService } from '../../../../core/services/geolocation.service';

const mockMap = {
  setView: vi.fn().mockReturnThis(),
  panTo: vi.fn().mockReturnThis(),
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

describe('StoreLocationCard', () => {
  let component: StoreLocationCard;
  let fixture: ComponentFixture<StoreLocationCard>;
  let geoService: GeolocationService;
  let form: FormGroup;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreLocationCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreLocationCard);
    component = fixture.componentInstance;
    geoService = TestBed.inject(GeolocationService);

    form = new FormGroup({
      address: new FormControl(''),
      addressReference: new FormControl(''),
      latitude: new FormControl<number | null>(null),
      longitude: new FormControl<number | null>(null),
    });

    fixture.componentRef.setInput('form', form);
    await fixture.whenStable();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
    expect(component.showManualGps()).toBe(false);
  });

  it('debe alternar la visibilidad de los campos manuales de GPS', () => {
    expect(component.showManualGps()).toBe(false);
    component.toggleManualGps();
    expect(component.showManualGps()).toBe(true);
    component.toggleManualGps();
    expect(component.showManualGps()).toBe(false);
  });

  it('debe actualizar el formulario cuando se emiten nuevas coordenadas desde el mapa', () => {
    component.onCoordinatesChange({ lat: -12.1215, lng: -77.0298 });
    expect(form.get('latitude')?.value).toBe(-12.1215);
    expect(form.get('longitude')?.value).toBe(-77.0298);
    expect(form.dirty).toBe(true);
  });

  it('debe invocar GeolocationService y actualizar el formulario con la ubicación actual', async () => {
    vi.spyOn(geoService, 'getCurrentPosition').mockResolvedValue({
      latitude: -12.099887,
      longitude: -77.033221,
      accuracy: 10,
    });

    await component.onUseCurrentLocation();

    expect(form.get('latitude')?.value).toBe(-12.099887);
    expect(form.get('longitude')?.value).toBe(-77.033221);
    expect(form.dirty).toBe(true);
  });

  it('debe autocompletar la direccion si el campo address esta vacio al seleccionar resultado', () => {
    form.get('address')?.setValue('');
    component.onAddressSelected({
      displayName: 'Av. Arequipa 1234, Lima',
      latitude: -12.08,
      longitude: -77.03,
    });

    expect(form.get('address')?.value).toBe('Av. Arequipa 1234, Lima');
    expect(form.dirty).toBe(true);
  });
});
