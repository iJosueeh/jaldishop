import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminHeader } from './admin-header';
import { AuthService } from '../../../../core/services/auth-service';
import { ProfileService } from '../../../../core/services/profile.service';
import { provideRouter } from '@angular/router';

describe('AdminHeader', () => {
  let component: AdminHeader;
  let fixture: ComponentFixture<AdminHeader>;
  let authServiceMock: any;
  let profileServiceMock: any;

  beforeEach(async () => {
    authServiceMock = {
      isMerchant: vi.fn().mockReturnValue(false),
      currentUser: vi.fn().mockReturnValue({ fullName: 'Josué Ramos' }),
      logout: vi.fn(),
    };
    profileServiceMock = {
      fullName: vi.fn().mockReturnValue('Josué Ramos'),
    };

    await TestBed.configureTestingModule({
      imports: [AdminHeader],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ProfileService, useValue: profileServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente y calcular las iniciales', () => {
    expect(component).toBeTruthy();
    expect(component.adminName()).toBe('Josué Ramos');
    expect(component.adminInitials()).toBe('JR');
  });

  it('debe llamar a logout() al invocar el método', () => {
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
