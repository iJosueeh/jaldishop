import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Register } from './register';
import { AuthService } from '../../../core/services/auth-service';
import { Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import {
  RegisterStep1Data,
  RegisterStep2Data,
  RegisterStep3Data,
} from './interface/register.models';
import { AuthResult } from '../../../core/models/auth.models';

describe('Register (Componente Principal del Flujo de Registro)', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let authService: AuthService;
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
    pickupEnabled: true,
    deliveryEnabled: true,
    address: 'Calle Los Olivos 123',
  };

  const mockStep3: RegisterStep3Data = {
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
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
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

  describe('onStep3Submit() y Conexión con Backend', () => {
    it('debe redirigir al paso 1 si falta step1Data', () => {
      component.onStep3Submit(mockStep3);
      expect(component.currentStep()).toBe(1);
    });

    it('debe construir el payload correcto, llamar a registerMerchant y navegar a /login en caso de éxito', () => {
      component.onStep1Submit(mockStep1);
      component.onStep2Submit(mockStep2);

      const registerSpy = vi
        .spyOn(authService, 'registerMerchant')
        .mockReturnValue(of(mockAuthResult));
      const navigateSpy = vi.spyOn(router, 'navigate');

      component.onStep3Submit(mockStep3);

      expect(registerSpy).toHaveBeenCalledWith({
        firstName: 'Josué',
        lastName: 'Tanta',
        email: 'josue@jaldishop.com',
        password: 'password123',
        phone: '999888777',
        storeName: 'Dulces Clara',
        businessType: 'Repostería y pastelería',
        storeContactPhone: '999888777',
        address: 'Calle Los Olivos 123',
        pickupEnabled: true,
        deliveryEnabled: true,
      });

      expect(component.isLoading()).toBe(false);
      expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    });

    it('debe capturar el mensaje de error del backend y desactivar isLoading si la petición falla', () => {
      component.onStep1Submit(mockStep1);
      component.onStep2Submit(mockStep2);

      const errorResponse = {
        error: {
          code: 'EMAIL_ALREADY_EXISTS',
          message: 'El correo ya está registrado como comerciante.',
        },
      };

      vi.spyOn(authService, 'registerMerchant').mockReturnValue(
        throwError(() => errorResponse),
      );

      component.onStep3Submit(mockStep3);

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toBe('El correo ya está registrado como comerciante.');
    });
  });
});
