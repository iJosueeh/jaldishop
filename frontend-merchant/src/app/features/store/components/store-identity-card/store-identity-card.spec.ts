import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreIdentityCard } from './store-identity-card';
import { FormControl, FormGroup } from '@angular/forms';

describe('StoreIdentityCard', () => {
  let component: StoreIdentityCard;
  let fixture: ComponentFixture<StoreIdentityCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreIdentityCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreIdentityCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', new FormGroup({
      name: new FormControl(''),
      contactPhone: new FormControl(''),
      description: new FormControl(''),
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
