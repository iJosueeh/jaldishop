import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreDeliveryCard } from './store-delivery-card';
import { FormControl, FormGroup } from '@angular/forms';

describe('StoreDeliveryCard', () => {
  let component: StoreDeliveryCard;
  let fixture: ComponentFixture<StoreDeliveryCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreDeliveryCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreDeliveryCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', new FormGroup({
      pickupEnabled: new FormControl(true),
      deliveryEnabled: new FormControl(true),
      deliveryFeeAmount: new FormControl(5.0),
      taxApplies: new FormControl(false),
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
