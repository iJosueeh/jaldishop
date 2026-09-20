import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { StorePreviewCard } from './store-preview-card';

describe('StorePreviewCard', () => {
  let component: StorePreviewCard;
  let fixture: ComponentFixture<StorePreviewCard>;
  let testForm: FormGroup;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, StorePreviewCard],
    }).compileComponents();

    testForm = new FormGroup({
      name: new FormControl('Panadería Don Pepe'),
      description: new FormControl('Panes artesanales y pasteles'),
      contactPhone: new FormControl('987654321'),
      address: new FormControl('Av. Larco 450'),
      addressReference: new FormControl('Frente al parque'),
      pickupEnabled: new FormControl(true),
      deliveryEnabled: new FormControl(true),
      deliveryFeeAmount: new FormControl(7.5),
      deliveryFeeCurrency: new FormControl('PEN'),
      taxApplies: new FormControl(true),
    });

    fixture = TestBed.createComponent(StorePreviewCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', testForm);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reflect initial form values', () => {
    expect(component.name()).toBe('Panadería Don Pepe');
    expect(component.description()).toBe('Panes artesanales y pasteles');
    expect(component.contactPhone()).toBe('987654321');
    expect(component.address()).toBe('Av. Larco 450');
    expect(component.addressReference()).toBe('Frente al parque');
    expect(component.deliveryFeeAmount()).toBe(7.5);
    expect(component.taxApplies()).toBe(true);
  });

  it('should update preview values in real time when form changes', () => {
    testForm.patchValue({
      name: 'Nueva Pastelería Gourmet',
      contactPhone: '912345678',
      deliveryFeeAmount: 10.0,
      taxApplies: false,
    });
    fixture.detectChanges();

    expect(component.name()).toBe('Nueva Pastelería Gourmet');
    expect(component.contactPhone()).toBe('912345678');
    expect(component.deliveryFeeAmount()).toBe(10.0);
    expect(component.taxApplies()).toBe(false);
  });
});
