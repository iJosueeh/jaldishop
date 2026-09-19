import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileInfoCard } from './profile-info-card';
import { FormControl, FormGroup } from '@angular/forms';

describe('ProfileInfoCard', () => {
  let component: ProfileInfoCard;
  let fixture: ComponentFixture<ProfileInfoCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileInfoCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileInfoCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput(
      'form',
      new FormGroup({
        firstName: new FormControl(''),
        lastName: new FormControl(''),
        phone: new FormControl(''),
      })
    );
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir el evento save al llamar a onSubmit', () => {
    let emitted = false;
    component.save.subscribe(() => {
      emitted = true;
    });

    component.onSubmit();
    expect(emitted).toBe(true);
  });
});
