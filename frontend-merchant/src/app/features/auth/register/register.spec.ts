import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Register } from './register';
import { AuthService } from '../../../core/services/auth-service';
import { StoreService } from '../../../core/services/store.service';
import { Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import {
  RegisterStep1Data,
  RegisterStep2Data,
  RegisterStep3Data,
  RegisterStep4Data,
} from '../../../core/models/register.models';
import { AuthResult } from '../../../core/models/auth.models';

describe('Register (Componente Principal del Flujo de Registro en 4 Pasos)', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let authService: AuthService;
  let storeService: StoreService;
  let router: Router;

  const mockStep1: RegisterStep1Data = {
    firstName: 'Josué',
    lastName: 'Tanta',
    email: 'josue@jaldishop.com',
    password: 'password123',
    passwordConfirm: 'password123',
    terms: true,
  };

  const mockStep2: RegisterStep2Data = {
    name: 'Dulces Clara',
    businessType: 'Repostería y pastelería',
    contactPhone: '999888777',
    logoUrl: 'https://res.cloudinary.com/demo/image/upload/logo.png',
    bannerUrl: 'https://res.cloudinary.com/demo/image/upload/banner.png',
    categoryIds: ['cat-reposteria'],
  };

  const mockStep3: RegisterStep3Data = {
    pickupEnabled: true,
    deliveryEnabled: true,
    address: 'Calle Los Olivos 123',
    addressReference: 'Frente al parque',
    latitude: -12.0463,
    longitude: -77.0427,
  };

  const mockStep4: RegisterStep4Data = {
    dailyOrderLimit: 12,
    prepTime: '30 - 60 min',
    openingTime: '09:00',
    closingTime: '19:00',
    operatingDays: ['L', 'M', 'X', 'J', 'V', 'S'],
    autoPauseOnLimit: true,
  };

  const mockAuthResult: AuthResult = {
    token: 'jwt.token.mock',
    userId: 'uuid-1234',
    email: 'josue@jaldishop.com',
    fullName: 'Josué Tanta',
    roles: ['CUSTOMER', 'MERCHANT'],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        AuthService,
        StoreService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    storeService = TestBed.inject(StoreService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('debe crearse e iniciar en el paso 1', () => {
    expect(component).toBeTruthy();
    expect(component.currentStep()).toBe(1);
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  });

  it('debe avanzar al paso 2 cuando se completa el paso 1', () => {
    component.onStep1Submit(mockStep1);

    expect(component.step1Data()).toEqual(mockStep1);
    expect(component.currentStep()).toBe(2);
    expect(component.errorMessage()).toBeNull();
  });

  it('debe regresar al paso 1 desde el paso 2 con onStep2Back', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Back();

    expect(component.currentStep()).toBe(1);
  });

  it('debe avanzar al paso 3 cuando se completa el paso 2', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Submit(mockStep2);

    expect(component.step2Data()).toEqual(mockStep2);
    expect(component.currentStep()).toBe(3);
  });

  it('debe asignar datos por defecto y avanzar a paso 3 si se omite el paso 2', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Skip();

    expect(component.step2Data()).toBeTruthy();
    expect(component.step2Data()?.name).toBe('Mi negocio');
    expect(component.currentStep()).toBe(3);
  });

  it('debe regresar al paso 2 desde el paso 3 con onStep3Back', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Submit(mockStep2);
    component.onStep3Back();

    expect(component.currentStep()).toBe(2);
  });

  it('debe avanzar al paso 4 cuando se completa el paso 3', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Submit(mockStep2);
    component.onStep3Submit(mockStep3);

    expect(component.step3Data()).toEqual(mockStep3);
    expect(component.currentStep()).toBe(4);
  });

  it('debe asignar datos por defecto y avanzar a paso 4 si se omite el paso 3', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Submit(mockStep2);
    component.onStep3Skip();

    expect(component.step3Data()).toBeTruthy();
    expect(component.currentStep()).toBe(4);
  });

  it('debe regresar al paso 3 desde el paso 4 con onStep4Back', () => {
    component.onStep1Submit(mockStep1);
    component.onStep2Submit(mockStep2);
    component.onStep3Submit(mockStep3);
    component.onStep4Back();

    expect(component.currentStep()).toBe(3);
  });

  describe('onStep4Submit() y Conexión con Backend', () => {
    it('debe redirigir al paso 1 si falta step1Data', () => {
      component.onStep4Submit(mockStep4);
      expect(component.currentStep()).toBe(1);
    });

    it('debe construir el payload correcto con ubicación y multimedia, llamar a registerMerchant y navegar a /login', () => {
      component.onStep1Submit(mockStep1);
      component.onStep2Submit(mockStep2);
      component.onStep3Submit(mockStep3);

      const registerSpy = vi
        .spyOn(authService, 'registerMerchant')
        .mockReturnValue(of(mockAuthResult));
      const navigateSpy = vi.spyOn(router, 'navigate');

      component.onStep4Submit(mockStep4);

      expect(registerSpy).toHaveBeenCalledWith({
        firstName: 'Josué',
        lastName: 'Tanta',
        email: 'josue@jaldishop.com',
        password: 'password123',
        phone: '+51999888777',
        storeName: 'Dulces Clara',
        businessType: 'Repostería y pastelería',
        storeContactPhone: '+51999888777',
        address: 'Calle Los Olivos 123',
        addressReference: 'Frente al parque',
        latitude: -12.0463,
        longitude: -77.0427,
        pickupEnabled: true,
        deliveryEnabled: true,
        logoUrl: 'https://res.cloudinary.com/demo/image/upload/logo.png',
        bannerUrl: 'https://res.cloudinary.com/demo/image/upload/banner.png',
        categoryIds: ['cat-reposteria'],
      });

      expect(component.isLoading()).toBe(false);
      expect(navigateSpy).toHaveBeenCalledWith(['/dashboard'], { replaceUrl: true });
    });

    it('debe capturar el mensaje de error del backend y desactivar isLoading si la petición falla', () => {
      component.onStep1Submit(mockStep1);
      component.onStep2Submit(mockStep2);
      component.onStep3Submit(mockStep3);

      const errorResponse = {
        error: {
          code: 'EMAIL_ALREADY_EXISTS',
          message: 'El correo ya está registrado como comerciante.',
        },
      };

      vi.spyOn(authService, 'registerMerchant').mockReturnValue(
        throwError(() => errorResponse),
      );

      component.onStep4Submit(mockStep4);

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toBe('El correo ya está registrado como comerciante.');
    });
  });

  describe('Flujo de Onboarding para Usuario Existente (isExistingUser)', () => {
    it('debe iniciar en paso 2 si el usuario ya está autenticado pero no es comerciante', () => {
      vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);
      vi.spyOn(authService, 'isMerchant').mockReturnValue(false);

      component.ngOnInit();

      expect(component.isExistingUser()).toBe(true);
      expect(component.currentStep()).toBe(2);
    });

    it('debe redirigir al /dashboard en ngOnInit si el usuario ya está autenticado y es comerciante', () => {
      vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);
      vi.spyOn(authService, 'isMerchant').mockReturnValue(true);
      const navigateSpy = vi.spyOn(router, 'navigate');

      component.ngOnInit();

      expect(navigateSpy).toHaveBeenCalledWith(['/dashboard'], { replaceUrl: true });
    });

    it('debe cerrar sesión al hacer back en paso 2 si es usuario existente', () => {
      const logoutSpy = vi.spyOn(authService, 'logout');
      component.isExistingUser.set(true);
      component.currentStep.set(2);

      component.onStep2Back();

      expect(logoutSpy).toHaveBeenCalled();
    });

    it('debe crear la tienda con storeService.createStore y refrescar token al completar paso 4', () => {
      component.isExistingUser.set(true);
      component.onStep2Submit(mockStep2);
      component.onStep3Submit(mockStep3);

      const storeResponseMock = {
        id: 'store-uuid-999',
        name: mockStep2.name,
      } as any;

      const createStoreSpy = vi.spyOn(storeService, 'createStore').mockReturnValue(of(storeResponseMock));
      const refreshTokenSpy = vi.spyOn(authService, 'refreshToken').mockReturnValue(of(mockAuthResult));
      const navigateSpy = vi.spyOn(router, 'navigate');

      component.onStep4Submit(mockStep4);

      expect(createStoreSpy).toHaveBeenCalled();
      expect(refreshTokenSpy).toHaveBeenCalled();
      expect(component.isLoading()).toBe(false);
      expect(navigateSpy).toHaveBeenCalledWith(['/dashboard'], { replaceUrl: true });
    });
  });
});
