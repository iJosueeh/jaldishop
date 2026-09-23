import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './error-interceptor';
import { ToastService } from '../services/toast.service';
import { TokenService } from '../services/token-service';
import { provideRouter, Router } from '@angular/router';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let toastService: ToastService;
  let tokenService: TokenService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: class {} }]),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
    tokenService = TestBed.inject(TokenService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('debe interceptar error 401, remover token y notificar sesión expirada', () => {
    vi.spyOn(tokenService, 'removeToken');
    vi.spyOn(toastService, 'warning');
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    httpClient.get('/api/v1/stores/me').subscribe({
      next: () => expect.fail('debe fallar'),
      error: (err) => {
        expect(err.status).toBe(401);
        expect(err.code).toBe('UNAUTHORIZED');
      },
    });

    const req = httpTestingController.expectOne('/api/v1/stores/me');
    req.flush({ code: 'UNAUTHORIZED', message: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(tokenService.removeToken).toHaveBeenCalled();
    expect(toastService.warning).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: 'true' } });
  });

  it('debe interceptar error 403 y emitir toast de error de permisos', () => {
    vi.spyOn(toastService, 'error');

    httpClient.get('/api/v1/admin/resource').subscribe({
      next: () => expect.fail('debe fallar'),
      error: (err) => {
        expect(err.status).toBe(403);
      },
    });

    const req = httpTestingController.expectOne('/api/v1/admin/resource');
    req.flush({ code: 'FORBIDDEN', message: 'Sin acceso' }, { status: 403, statusText: 'Forbidden' });

    expect(toastService.error).toHaveBeenCalled();
  });

  it('debe interceptar error de conexión (status 0) y emitir toast de red', () => {
    vi.spyOn(toastService, 'error');

    httpClient.get('/api/v1/stores/me').subscribe({
      next: () => expect.fail('debe fallar'),
      error: (err) => {
        expect(err.status).toBe(0);
        expect(err.code).toBe('NETWORK_ERROR');
      },
    });

    const req = httpTestingController.expectOne('/api/v1/stores/me');
    req.error(new ProgressEvent('error'), { status: 0 });

    expect(toastService.error).toHaveBeenCalled();
  });
});
