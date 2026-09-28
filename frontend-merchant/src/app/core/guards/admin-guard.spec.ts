import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { adminGuard } from './admin-guard';
import { AuthService } from '../services/auth-service';

describe('adminGuard', () => {
  let authServiceMock: {
    isAdmin: ReturnType<typeof vi.fn>;
    isAuthenticated: ReturnType<typeof vi.fn>;
  };
  let routerMock: {
    createUrlTree: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    authServiceMock = {
      isAdmin: vi.fn(),
      isAuthenticated: vi.fn(),
    };
    routerMock = {
      createUrlTree: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });
  });

  it('permite el acceso si el usuario tiene rol ADMIN', () => {
    authServiceMock.isAdmin.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('redirige a /unauthorized si el usuario está autenticado pero no es ADMIN', () => {
    authServiceMock.isAdmin.mockReturnValue(false);
    authServiceMock.isAuthenticated.mockReturnValue(true);
    const mockUrlTree = {} as UrlTree;
    routerMock.createUrlTree.mockReturnValue(mockUrlTree);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/unauthorized']);
    expect(result).toBe(mockUrlTree);
  });

  it('redirige a /login si el usuario no está autenticado', () => {
    authServiceMock.isAdmin.mockReturnValue(false);
    authServiceMock.isAuthenticated.mockReturnValue(false);
    const mockUrlTree = {} as UrlTree;
    routerMock.createUrlTree.mockReturnValue(mockUrlTree);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBe(mockUrlTree);
  });
});
