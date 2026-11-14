import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ForgotPassword } from './forgot-password';
import { AuthService } from '../../../core/services/auth-service';
import { of, Subject, throwError, TimeoutError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

describe('ForgotPassword Component', () => {
  let component: ForgotPassword;
  let fixture: ComponentFixture<ForgotPassword>;
  let authServiceMock: { forgotPassword: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authServiceMock = {
      forgotPassword: vi.fn().mockReturnValue(
        of({ message: 'Si el correo corresponde a una cuenta habilitada, recibirás instrucciones.' })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar el formulario inválido', () => {
    expect(component).toBeTruthy();
    expect(component.forgotForm.valid).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  });

  it('debe validar formato y restricciones de correo electrónico', () => {
    const emailControl = component.forgotForm.controls.email;

    emailControl.setValue('');
    expect(emailControl.hasError('required')).toBe(true);

    emailControl.setValue('correo-invalido');
    expect(emailControl.hasError('email')).toBe(true);

    const longEmail = 'a'.repeat(246) + '@test.com'; // 255 chars
    emailControl.setValue(longEmail);
    expect(emailControl.hasError('maxlength')).toBe(true);

    emailControl.setValue('comerciante@negocio.pe');
    expect(emailControl.valid).toBe(true);
  });

  it('no debe enviar el formulario si es inválido y marcar touched', () => {
    component.handleSubmit();
    expect(authServiceMock.forgotPassword).not.toHaveBeenCalled();
    expect(component.isSubmitted()).toBe(false);
    expect(component.forgotForm.controls.email.touched).toBe(true);
  });

  it('debe enviar la solicitud al servicio y cambiar a estado enviado al responder 202', () => {
    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(authServiceMock.forgotPassword).toHaveBeenCalledWith('lucia@postres.pe');
    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(true);
    expect(component.sentEmail()).toBe('lucia@postres.pe');
    expect(component.errorMessage()).toBeNull();
  });

  it('debe mantener isLoading activo durante el procesamiento de la solicitud', () => {
    const responseSubject = new Subject<{ message: string }>();
    authServiceMock.forgotPassword.mockReturnValue(responseSubject.asObservable());

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(true);
    expect(component.isSubmitted()).toBe(false);

    responseSubject.next({ message: 'OK' });
    responseSubject.complete();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(true);
  });

  it('debe prevenir solicitudes duplicadas si ya existe una solicitud en progreso', () => {
    const responseSubject = new Subject<{ message: string }>();
    authServiceMock.forgotPassword.mockReturnValue(responseSubject.asObservable());

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(authServiceMock.forgotPassword).toHaveBeenCalledTimes(1);
    expect(component.isLoading()).toBe(true);

    // Segundo intento mientras isLoading está activo
    component.handleSubmit();
    expect(authServiceMock.forgotPassword).toHaveBeenCalledTimes(1);
  });

  it('debe manejar timeout de red y conservar el correo en el formulario', () => {
    authServiceMock.forgotPassword.mockReturnValue(throwError(() => new TimeoutError()));

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toContain('tiempo límite');
    expect(component.forgotForm.controls.email.value).toBe('lucia@postres.pe');
  });

  it('debe manejar error de conexión (status 0) y conservar el correo en el formulario', () => {
    const errorResponse = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' });
    authServiceMock.forgotPassword.mockReturnValue(throwError(() => errorResponse));

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toContain('No se pudo conectar con el servidor');
    expect(component.forgotForm.controls.email.value).toBe('lucia@postres.pe');
  });

  it('debe manejar límite de tasa excedido (status 429) y conservar el correo', () => {
    const errorResponse = new HttpErrorResponse({ status: 429, statusText: 'Too Many Requests' });
    authServiceMock.forgotPassword.mockReturnValue(throwError(() => errorResponse));

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toContain('límite de intentos');
    expect(component.forgotForm.controls.email.value).toBe('lucia@postres.pe');
  });

  it('debe manejar error interno del servidor (status 500) y conservar el correo', () => {
    const errorResponse = new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' });
    authServiceMock.forgotPassword.mockReturnValue(throwError(() => errorResponse));

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toContain('El servidor experimentó un inconveniente temporal');
    expect(component.forgotForm.controls.email.value).toBe('lucia@postres.pe');
  });

  it('debe manejar error 400 mostrando mensaje del backend si existe', () => {
    const errorResponse = new HttpErrorResponse({
      status: 400,
      statusText: 'Bad Request',
      error: { message: 'El correo electrónico no es válido según los estándares del servidor.' },
    });
    authServiceMock.forgotPassword.mockReturnValue(throwError(() => errorResponse));

    component.forgotForm.controls.email.setValue('test@invalido.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toBe('El correo electrónico no es válido según los estándares del servidor.');
  });

  it('debe permitir resetear el formulario para probar otro correo', () => {
    component.sentEmail.set('test@jaldi.pe');
    component.isSubmitted.set(true);
    component.errorMessage.set('Algún error previo');

    component.resetForm();

    expect(component.isSubmitted()).toBe(false);
    expect(component.errorMessage()).toBeNull();
    expect(component.forgotForm.value.email).toBeFalsy();
  });
});
