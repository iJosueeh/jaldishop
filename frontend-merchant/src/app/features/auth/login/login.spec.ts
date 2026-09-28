import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login';
import { AuthService } from '../../../core/services/auth-service';
import { ToastService } from '../../../core/services/toast.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceMock: any;
  let toastMock: any;
  let router: Router;

  beforeEach(async () => {
    authServiceMock = {
      login: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
      isMerchant: vi.fn().mockReturnValue(true),
    };
    toastMock = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ToastService, useValue: toastMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  });

  it('debe redirigir a /dashboard si el usuario es MERCHANT', () => {
    authServiceMock.login.mockReturnValue(of({ token: 'fake-jwt', user: {} }));
    authServiceMock.isAdmin.mockReturnValue(false);
    authServiceMock.isMerchant.mockReturnValue(true);

    component.onLogin({ email: 'test@store.com', password: 'password123' });

    expect(authServiceMock.login).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('debe redirigir a /admin/dashboard si el usuario es ADMIN puro', () => {
    authServiceMock.login.mockReturnValue(of({ token: 'fake-jwt', user: {} }));
    authServiceMock.isAdmin.mockReturnValue(true);
    authServiceMock.isMerchant.mockReturnValue(false);

    component.onLogin({ email: 'admin@jaldishop.com', password: 'password123' });

    expect(router.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('debe manejar error 401 mostrando mensaje de credenciales incorrectas', () => {
    authServiceMock.login.mockReturnValue(throwError(() => ({ status: 401 })));

    component.onLogin({ email: 'wrong@test.com', password: 'wrong' });

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Correo o contraseña incorrectos.');
  });
});
