import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { merchantGuard } from './merchant-guard';
import { AuthService } from '../services/auth-service';

describe('merchantGuard', () => {
  let authServiceMock: {
    isMerchant: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
    isAuthenticated: ReturnType<typeof vi.fn>;
  };
  let routerMock: {
    createUrlTree: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    authServiceMock = {
      isMerchant: vi.fn(),
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

  it('permite el acceso si el usuario tiene rol MERCHANT', () => {
    authServiceMock.isMerchant.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => merchantGuard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('redirige a /admin/dashboard si el usuario es ADMIN y no MERCHANT', () => {
    authServiceMock.isMerchant.mockReturnValue(false);
    authServiceMock.isAdmin.mockReturnValue(true);
    const mockUrlTree = {} as UrlTree;
    routerMock.createUrlTree.mockReturnValue(mockUrlTree);

    const result = TestBed.runInInjectionContext(() => merchantGuard({} as any, {} as any));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/admin/dashboard']);
    expect(result).toBe(mockUrlTree);
  });

  it('redirige a /unauthorized si el usuario está autenticado pero no es MERCHANT ni ADMIN', () => {
    authServiceMock.isMerchant.mockReturnValue(false);
    authServiceMock.isAdmin.mockReturnValue(false);
    authServiceMock.isAuthenticated.mockReturnValue(true);
    const mockUrlTree = {} as UrlTree;
    routerMock.createUrlTree.mockReturnValue(mockUrlTree);

    const result = TestBed.runInInjectionContext(() => merchantGuard({} as any, {} as any));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/unauthorized']);
    expect(result).toBe(mockUrlTree);
  });

  it('redirige a /login si el usuario no está autenticado', () => {
    authServiceMock.isMerchant.mockReturnValue(false);
    authServiceMock.isAdmin.mockReturnValue(false);
    authServiceMock.isAuthenticated.mockReturnValue(false);
    const mockUrlTree = {} as UrlTree;
    routerMock.createUrlTree.mockReturnValue(mockUrlTree);

    const result = TestBed.runInInjectionContext(() => merchantGuard({} as any, {} as any));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBe(mockUrlTree);
  });
});
