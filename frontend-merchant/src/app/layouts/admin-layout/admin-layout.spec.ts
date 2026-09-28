import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminLayout } from './admin-layout';
import { AdminService } from '../../core/services/admin.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth-service';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

describe('AdminLayout', () => {
  let component: AdminLayout;
  let fixture: ComponentFixture<AdminLayout>;
  let adminServiceMock: any;
  let profileServiceMock: any;
  let authServiceMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      users: vi.fn().mockReturnValue([]),
      stores: vi.fn().mockReturnValue([]),
      metrics: vi.fn().mockReturnValue({ totalUsers: 5, totalStores: 2 }),
      loadUsers: vi.fn().mockReturnValue(of([])),
      loadStores: vi.fn().mockReturnValue(of([])),
    };
    profileServiceMock = {
      currentProfile: vi.fn().mockReturnValue(null),
      fullName: vi.fn().mockReturnValue('Super Admin'),
      getMyProfile: vi.fn().mockReturnValue(of({})),
    };
    authServiceMock = {
      currentUser: vi.fn().mockReturnValue({ fullName: 'Super Admin' }),
      isMerchant: vi.fn().mockReturnValue(false),
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminLayout],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: adminServiceMock },
        { provide: ProfileService, useValue: profileServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente e inicializar el menú móvil cerrado', () => {
    expect(component).toBeTruthy();
    expect(component.isMobileMenuOpen()).toBe(false);
  });

  it('debe alternar y cerrar el menú móvil', () => {
    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(true);

    component.closeMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(false);
  });

  it('debe abrir y cerrar la command palette', () => {
    expect(component.isCommandPaletteOpen()).toBe(false);

    component.openCommandPalette();
    expect(component.isCommandPaletteOpen()).toBe(true);

    component.closeCommandPalette();
    expect(component.isCommandPaletteOpen()).toBe(false);
  });
});
