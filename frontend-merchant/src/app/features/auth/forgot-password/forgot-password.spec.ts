import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ForgotPassword } from './forgot-password';

describe('ForgotPassword Component', () => {
  let component: ForgotPassword;
  let fixture: ComponentFixture<ForgotPassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [
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
  });

  it('debe validar formato de correo electrónico', () => {
    const emailControl = component.forgotForm.controls.email;

    emailControl.setValue('correo-invalido');
    expect(emailControl.hasError('email')).toBe(true);

    emailControl.setValue('comerciante@negocio.pe');
    expect(emailControl.valid).toBe(true);
  });

  it('no debe enviar el formulario si es inválido y marcar touched', () => {
    component.handleSubmit();
    expect(component.isSubmitted()).toBe(false);
    expect(component.forgotForm.controls.email.touched).toBe(true);
  });

  it('debe procesar el envío correctamente cuando el formulario es válido', () => {
    vi.useFakeTimers();

    component.forgotForm.controls.email.setValue('lucia@postres.pe');
    component.handleSubmit();

    expect(component.isLoading()).toBe(true);

    vi.advanceTimersByTime(600);

    expect(component.isLoading()).toBe(false);
    expect(component.isSubmitted()).toBe(true);
    expect(component.sentEmail()).toBe('lucia@postres.pe');

    vi.useRealTimers();
  });

  it('debe permitir resetear el formulario para probar otro correo', () => {
    component.sentEmail.set('test@jaldi.pe');
    component.isSubmitted.set(true);

    component.resetForm();

    expect(component.isSubmitted()).toBe(false);
    expect(component.forgotForm.value.email).toBeFalsy();
  });
});
