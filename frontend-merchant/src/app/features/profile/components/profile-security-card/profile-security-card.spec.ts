import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileSecurityCard } from './profile-security-card';

describe('ProfileSecurityCard', () => {
  let component: ProfileSecurityCard;
  let fixture: ComponentFixture<ProfileSecurityCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileSecurityCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileSecurityCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
