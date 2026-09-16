import { TestBed } from '@angular/core/testing';
import { ErrorHandlerService } from './error-handler.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('ErrorHandlerService', () => {
  let service: ErrorHandlerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ErrorHandlerService);
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe normalizar error de red (status 0)', () => {
    const error = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' });
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(0);
    expect(normalized.code).toBe('NETWORK_ERROR');
    expect(normalized.message).toContain('conexión');
    expect(normalized.fieldErrors).toEqual({});
  });

  it('debe normalizar error 400 con ApiError y errores de campos', () => {
    const apiErrorPayload = {
      code: 'VALIDATION_ERROR',
      message: 'Los datos enviados no son válidos',
      errors: {
        name: 'El nombre es obligatorio',
        contactPhone: 'Formato inválido',
      },
    };

    const error = new HttpErrorResponse({
      status: 400,
      error: apiErrorPayload,
    });

    const normalized = service.normalize(error);

    expect(normalized.status).toBe(400);
    expect(normalized.code).toBe('VALIDATION_ERROR');
    expect(normalized.message).toBe('Los datos enviados no son válidos');
    expect(normalized.fieldErrors).toEqual({
      name: 'El nombre es obligatorio',
      contactPhone: 'Formato inválido',
    });
  });

  it('debe normalizar error 401 Unauthorized', () => {
    const error = new HttpErrorResponse({ status: 401 });
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(401);
    expect(normalized.code).toBe('UNAUTHORIZED');
    expect(normalized.message).toContain('sesión');
  });

  it('debe normalizar error 403 Forbidden', () => {
    const error = new HttpErrorResponse({ status: 403 });
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(403);
    expect(normalized.code).toBe('FORBIDDEN');
    expect(normalized.message).toContain('permisos');
  });

  it('debe normalizar error 404 Not Found', () => {
    const error = new HttpErrorResponse({ status: 404 });
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(404);
    expect(normalized.code).toBe('NOT_FOUND');
    expect(normalized.message).toContain('no fue encontrado');
  });

  it('debe normalizar error 409 Conflict', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'STORE_SLUG_ALREADY_EXISTS', message: 'El slug ya existe' },
    });
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(409);
    expect(normalized.code).toBe('STORE_SLUG_ALREADY_EXISTS');
    expect(normalized.message).toBe('El slug ya existe');
  });

  it('debe normalizar errores desconocidos o no-HTTP', () => {
    const error = new Error('Error local de JS');
    const normalized = service.normalize(error);

    expect(normalized.status).toBe(0);
    expect(normalized.code).toBe('UNKNOWN_ERROR');
    expect(normalized.fieldErrors).toEqual({});
  });
});
