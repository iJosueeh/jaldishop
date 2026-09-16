import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreLocationCard } from './store-location-card';
import { FormControl, FormGroup } from '@angular/forms';

describe('StoreLocationCard', () => {
  let component: StoreLocationCard;
  let fixture: ComponentFixture<StoreLocationCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreLocationCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreLocationCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', new FormGroup({
      address: new FormControl(''),
      addressReference: new FormControl(''),
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
