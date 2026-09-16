import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Unauthorized } from './unauthorized';
import { AuthService } from '../../../core/services/auth-service';
import { TokenService } from '../../../core/services/token-service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('Unauthorized (Página 403)', () => {
  let component: Unauthorized;
  let fixture: ComponentFixture<Unauthorized>;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Unauthorized],
      providers: [
        AuthService,
        TokenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Unauthorized);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe renderizar el mensaje de acceso restringido 403', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('403');
    expect(compiled.textContent).toContain('No tienes permisos para esta sección');
  });

  it('debe llamar a authService.logout() al ejecutar handleLogout()', () => {
    const logoutSpy = vi.spyOn(authService, 'logout');
    component.handleLogout();
    expect(logoutSpy).toHaveBeenCalled();
  });
});
