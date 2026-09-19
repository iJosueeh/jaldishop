import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileSecurityCard } from './profile-security-card';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('ProfileSecurityCard', () => {
  let component: ProfileSecurityCard;
  let fixture: ComponentFixture<ProfileSecurityCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileSecurityCard],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileSecurityCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
