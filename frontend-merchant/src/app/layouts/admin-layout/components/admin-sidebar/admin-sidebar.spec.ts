import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminSidebar } from './admin-sidebar';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth-service';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

describe('AdminSidebar', () => {
  let component: AdminSidebar;
  let fixture: ComponentFixture<AdminSidebar>;
  let adminServiceMock: any;
  let authServiceMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      users: vi.fn().mockReturnValue([]),
      stores: vi.fn().mockReturnValue([]),
      metrics: vi.fn().mockReturnValue({
        totalUsers: 10,
        totalStores: 5,
        totalMerchants: 4,
      }),
      loadUsers: vi.fn().mockReturnValue(of([])),
      loadStores: vi.fn().mockReturnValue(of([])),
    };
    authServiceMock = {
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminSidebar],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: adminServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSidebar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente y cargar datos si no están cargados', () => {
    expect(component).toBeTruthy();
    expect(adminServiceMock.loadUsers).toHaveBeenCalled();
    expect(adminServiceMock.loadStores).toHaveBeenCalled();
  });

  it('debe llamar a logout() al presionar el botón de cerrar sesión', () => {
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
