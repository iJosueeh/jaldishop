import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Store } from './store';

describe('Store', () => {
  let component: Store;
  let fixture: ComponentFixture<Store>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Store],
    }).compileComponents();

    fixture = TestBed.createComponent(Store);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe marcar error requireAtLeastOneFulfillment si se desactivan pickup y delivery', () => {
    component.storeForm.patchValue({
      pickupEnabled: false,
      deliveryEnabled: false,
    });
    expect(component.storeForm.hasError('requireAtLeastOneFulfillment')).toBe(true);
    expect(component.storeForm.valid).toBe(false);

    component.storeForm.patchValue({
      pickupEnabled: true,
      deliveryEnabled: false,
    });
    expect(component.storeForm.hasError('requireAtLeastOneFulfillment')).toBe(false);
  });

  it('debe marcar error coordinatesParityMismatch si solo se ingresa latitud o longitud', () => {
    component.storeForm.patchValue({
      latitude: -12.046374,
      longitude: null,
    });
    expect(component.storeForm.hasError('coordinatesParityMismatch')).toBe(true);

    component.storeForm.patchValue({
      latitude: null,
      longitude: -77.042793,
    });
    expect(component.storeForm.hasError('coordinatesParityMismatch')).toBe(true);

    component.storeForm.patchValue({
      latitude: -12.046374,
      longitude: -77.042793,
    });
    expect(component.storeForm.hasError('coordinatesParityMismatch')).toBe(false);

    component.storeForm.patchValue({
      latitude: null,
      longitude: null,
    });
    expect(component.storeForm.hasError('coordinatesParityMismatch')).toBe(false);
  });

  it('debe validar el rango de latitud (-90 a 90) y longitud (-180 a 180)', () => {
    const latControl = component.storeForm.get('latitude');
    const lngControl = component.storeForm.get('longitude');

    latControl?.setValue(-95);
    expect(latControl?.hasError('min')).toBe(true);

    latControl?.setValue(95);
    expect(latControl?.hasError('max')).toBe(true);

    lngControl?.setValue(-190);
    expect(lngControl?.hasError('min')).toBe(true);

    lngControl?.setValue(190);
    expect(lngControl?.hasError('max')).toBe(true);
  });
});
