import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth-service';
import { TokenService } from './token-service';
import { Router } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthResult, LoginRequest, RegisterMerchantRequest } from '../models/auth.models';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;
  let tokenService: TokenService;
  let router: Router;

  const mockAuthResult: AuthResult = {
    token: 'jwt.token.here',
    userId: 'uuid-1234',
    email: 'josue@jaldishop.com',
    fullName: 'Josue Tanta',
    roles: ['CUSTOMER', 'MERCHANT'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        TokenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
    router = TestBed.inject(Router);

    sessionStorage.clear();
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  describe('login()', () => {
    it('debe autenticar al usuario, guardar el token y actualizar señales reactivas', () => {
      const credentials: LoginRequest = {
        email: 'josue@jaldishop.com',
        password: 'password123',
      };

      // Simular que tokenService retorna roles válidos tras setToken
      vi.spyOn(tokenService, 'getRoles').mockReturnValue(['MERCHANT', 'CUSTOMER']);

      service.login(credentials).subscribe((result) => {
        expect(result).toEqual(mockAuthResult);
        expect(service.token()).toBe(mockAuthResult.token);
        expect(service.currentUser()).toEqual(mockAuthResult);
        expect(service.isAuthenticated()).toBe(true);
        expect(service.isMerchant()).toBe(true);
      });

      const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(mockAuthResult);
    });

    it('debe rechazar el acceso si el usuario no tiene rol MERCHANT o ADMIN', () => {
      const credentials: LoginRequest = {
        email: 'cliente@jaldishop.com',
        password: 'password123',
      };

      vi.spyOn(tokenService, 'getRoles').mockReturnValue(['CUSTOMER']);
      const logoutSpy = vi.spyOn(service, 'logout');

      service.login(credentials).subscribe({
        next: () => {
          throw new Error('No debió autenticar');
        },
        error: (err) => {
          expect(err.message).toContain('Acceso denegado');
          expect(logoutSpy).toHaveBeenCalled();
        },
      });

      const req = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });
  });

  describe('registerMerchant()', () => {
    it('debe enviar la petición de registro con datos de usuario y tienda y actualizar estado', () => {
      const registerPayload: RegisterMerchantRequest = {
        email: 'nuevo@jaldishop.com',
        password: 'password123',
        firstName: 'Josue',
        lastName: 'Tanta',
        phone: '999888777',
        storeName: 'Pastelería Dulce',
        businessType: 'Repostería y pastelería',
        storeContactPhone: '999888777',
        address: 'Av. Las Palmeras 123',
        pickupEnabled: true,
        deliveryEnabled: true,
      };

      service.registerMerchant(registerPayload).subscribe((result) => {
        expect(result).toEqual(mockAuthResult);
        expect(service.token()).toBe(mockAuthResult.token);
        expect(service.currentUser()).toEqual(mockAuthResult);
      });

      const req = httpTesting.expectOne(`${environment.apiUrl}/auth/register/merchant`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerPayload);
      req.flush(mockAuthResult);
    });
  });

  describe('logout()', () => {
    it('debe limpiar token, currentUser y redirigir a /login', () => {
      const navigateSpy = vi.spyOn(router, 'navigate');

      service.logout();

      expect(service.token()).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    });
  });
});
