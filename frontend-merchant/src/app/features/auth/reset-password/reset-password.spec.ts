import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ResetPassword } from './reset-password';
import { AuthService } from '../../../core/services/auth-service';
import { of, Subject, throwError, TimeoutError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('ResetPassword Component', () => {
  let component: ResetPassword;
  let fixture: ComponentFixture<ResetPassword>;
  let authServiceMock: { resetPassword: ReturnType<typeof vi.fn> };
  const validToken = 'B'.repeat(43);

  const createComponentWithToken = async (tokenValue: string | null = validToken) => {
    authServiceMock = {
      resetPassword: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [ResetPassword],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([
          { path: 'login', component: class {} },
          { path: 'forgot-password', component: class {} },
        ]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(tokenValue ? { token: tokenValue } : {}),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('Inicialización y lectura segura del token', () => {
    it('debe crearse correctamente con token válido en la URL y sanitizar la barra de direcciones', async () => {
      const replaceStateSpy = vi.spyOn(window.history, 'replaceState');

      await createComponentWithToken(validToken);

      expect(component).toBeTruthy();
      expect(component.token()).toBe(validToken);
      expect(component.isTokenInvalid()).toBe(false);
      expect(component.isSuccess()).toBe(false);
      expect(component.isLoading()).toBe(false);

      // Limpieza de URL
      expect(replaceStateSpy).toHaveBeenCalled();

      // No guardar token en ningún almacenamiento del navegador
      expect(localStorage.getItem('token')).toBeNull();
      expect(sessionStorage.getItem('token')).toBeNull();
      expect(document.cookie).not.toContain(validToken);
    });

    it('debe marcar token como inválido si el token está ausente en la URL', async () => {
      await createComponentWithToken(null);

      expect(component.isTokenInvalid()).toBe(true);
      expect(component.token()).toBeNull();
    });

    it('debe marcar token como inválido si el formato no cumple con base64url de 43 caracteres', async () => {
      // Longitud menor
      await createComponentWithToken('token-corto');
      expect(component.isTokenInvalid()).toBe(true);

      // Caracteres no permitidos (+, /, =)
      TestBed.resetTestingModule();
      await createComponentWithToken('A'.repeat(42) + '+');
      expect(component.isTokenInvalid()).toBe(true);
    });
  });

  describe('Validaciones del formulario de contraseña', () => {
    beforeEach(async () => {
      await createComponentWithToken(validToken);
    });

    it('debe validar longitud mínima de 8 caracteres y requerimiento de nueva contraseña', () => {
      const passControl = component.resetForm.controls.newPassword;

      passControl.setValue('');
      expect(passControl.hasError('required')).toBe(true);

      passControl.setValue('1234567');
      expect(passControl.hasError('minlength')).toBe(true);

      passControl.setValue('12345678');
      expect(passControl.valid).toBe(true);
    });

    it('debe validar longitud máxima de 72 caracteres y 72 bytes UTF-8', () => {
      const passControl = component.resetForm.controls.newPassword;

      passControl.setValue('a'.repeat(73));
      expect(passControl.hasError('maxlength')).toBe(true);

      // 25 emojis de 4 bytes = 100 bytes (aunque son 25 caracteres)
      passControl.setValue('🔐'.repeat(20));
      expect(passControl.hasError('maxBytes')).toBe(true);

      passControl.setValue('PasswordSegura123!');
      expect(passControl.valid).toBe(true);
    });

    it('debe validar que las contraseñas coincidan exactamente', () => {
      component.resetForm.controls.newPassword.setValue('Password123!');
      component.resetForm.controls.confirmPassword.setValue('PasswordDiferente');

      expect(component.resetForm.hasError('mismatch')).toBe(true);

      component.resetForm.controls.confirmPassword.setValue('Password123!');
      expect(component.resetForm.hasError('mismatch')).toBe(false);
      expect(component.resetForm.valid).toBe(true);
    });

    it('no debe enviar el formulario si es inválido y marcar todos los campos touched', () => {
      component.handleSubmit();

      expect(authServiceMock.resetPassword).not.toHaveBeenCalled();
      expect(component.resetForm.controls.newPassword.touched).toBe(true);
      expect(component.resetForm.controls.confirmPassword.touched).toBe(true);
    });
  });

  describe('Envío, Loading y Prevención de Solicitudes Duplicadas', () => {
    beforeEach(async () => {
      await createComponentWithToken(validToken);
    });

    it('no debe procesar envío duplicado si isLoading ya está activo', () => {
      component.isLoading.set(true);
      component.resetForm.controls.newPassword.setValue('PasswordSegura123!');
      component.resetForm.controls.confirmPassword.setValue('PasswordSegura123!');

      component.handleSubmit();

      expect(authServiceMock.resetPassword).not.toHaveBeenCalled();
    });

    it('no debe enviar si el token está marcado como inválido', () => {
      component.isTokenInvalid.set(true);
      component.resetForm.controls.newPassword.setValue('PasswordSegura123!');
      component.resetForm.controls.confirmPassword.setValue('PasswordSegura123!');

      component.handleSubmit();

      expect(authServiceMock.resetPassword).not.toHaveBeenCalled();
    });

    it('debe activar loading durante la solicitud y responder con éxito (HTTP 204)', () => {
      const resetSubject = new Subject<void>();
      authServiceMock.resetPassword.mockReturnValue(resetSubject.asObservable());

      component.resetForm.controls.newPassword.setValue('MiNuevaClaveSegura123!');
      component.resetForm.controls.confirmPassword.setValue('MiNuevaClaveSegura123!');

      component.handleSubmit();

      expect(component.isLoading()).toBe(true);
      expect(component.errorMessage()).toBeNull();
      expect(authServiceMock.resetPassword).toHaveBeenCalledWith({
        token: validToken,
        newPassword: 'MiNuevaClaveSegura123!',
      });

      // Simular respuesta HTTP 204
      resetSubject.next();
      resetSubject.complete();

      expect(component.isLoading()).toBe(false);
      expect(component.isSuccess()).toBe(true);
      // Token purgado de memoria
      expect(component.token()).toBeNull();
    });
  });

  describe('Manejo de Errores y Códigos de Respuesta HTTP', () => {
    beforeEach(async () => {
      await createComponentWithToken(validToken);
      component.resetForm.controls.newPassword.setValue('MiNuevaClaveSegura123!');
      component.resetForm.controls.confirmPassword.setValue('MiNuevaClaveSegura123!');
    });

    it('debe mostrar pantalla de enlace expirado/utilizado ante HTTP 422', () => {
      const error422 = new HttpErrorResponse({
        status: 422,
        error: { code: 'INVALID_PASSWORD_RESET_TOKEN', message: 'Token expirado' },
      });
      authServiceMock.resetPassword.mockReturnValue(throwError(() => error422));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.isTokenInvalid()).toBe(true);
      expect(component.token()).toBeNull();
    });

    it('debe mostrar error de validación cuando el servidor responde HTTP 400', () => {
      const error400 = new HttpErrorResponse({
        status: 400,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'La contraseña contiene caracteres no admitidos.',
        },
      });
      authServiceMock.resetPassword.mockReturnValue(throwError(() => error400));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toBe('La contraseña contiene caracteres no admitidos.');
      // Campos preservados para corrección
      expect(component.resetForm.controls.newPassword.value).toBe('MiNuevaClaveSegura123!');
    });

    it('debe mostrar mensaje de límite excedido cuando el servidor responde HTTP 429', () => {
      const error429 = new HttpErrorResponse({
        status: 429,
        error: { code: 'RATE_LIMIT_EXCEEDED' },
      });
      authServiceMock.resetPassword.mockReturnValue(throwError(() => error429));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toContain('límite de intentos');
    });

    it('debe permitir reintentar sin perder los campos ante error de conexión o status 0', () => {
      const error0 = new HttpErrorResponse({ status: 0 });
      authServiceMock.resetPassword.mockReturnValue(throwError(() => error0));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toContain('No se pudo conectar con el servidor');
      expect(component.resetForm.controls.newPassword.value).toBe('MiNuevaClaveSegura123!');
      expect(component.resetForm.controls.confirmPassword.value).toBe('MiNuevaClaveSegura123!');
    });

    it('debe permitir reintentar sin perder los campos ante error de servidor 5xx', () => {
      const error500 = new HttpErrorResponse({ status: 500 });
      authServiceMock.resetPassword.mockReturnValue(throwError(() => error500));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toContain('inconveniente temporal');
      expect(component.resetForm.controls.newPassword.value).toBe('MiNuevaClaveSegura123!');
    });

    it('debe manejar TimeoutError adecuadamente informando tiempo excedido', () => {
      authServiceMock.resetPassword.mockReturnValue(throwError(() => new TimeoutError()));

      component.handleSubmit();

      expect(component.isLoading()).toBe(false);
      expect(component.errorMessage()).toContain('excedió el tiempo límite');
    });
  });

  describe('Alternancia de visibilidad de contraseñas', () => {
    beforeEach(async () => {
      await createComponentWithToken(validToken);
    });

    it('debe alternar visibilidad de contraseña y confirmación', () => {
      expect(component.showPassword()).toBe(false);
      component.togglePassword();
      expect(component.showPassword()).toBe(true);
      component.togglePassword();
      expect(component.showPassword()).toBe(false);

      expect(component.showConfirmPassword()).toBe(false);
      component.toggleConfirmPassword();
      expect(component.showConfirmPassword()).toBe(true);
      component.toggleConfirmPassword();
      expect(component.showConfirmPassword()).toBe(false);
    });
  });
});
