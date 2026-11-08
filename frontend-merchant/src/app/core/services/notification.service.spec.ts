import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Notification, UnreadCountResponse } from '../models/notification.models';
import { NotificationService } from './notification.service';
import { TokenService } from './token-service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;
  let tokenServiceMock: any;

  const mockNotifications: Notification[] = [
    {
      id: 'notif-1',
      userId: 'user-1',
      type: 'NEW_ORDER',
      title: 'Nuevo pedido #ORD-101',
      message: 'Tienes un nuevo pedido para atender.',
      status: 'UNREAD',
      createdAt: '2026-10-31T10:00:00Z',
      readAt: null,
    },
    {
      id: 'notif-2',
      userId: 'user-1',
      type: 'SYSTEM',
      title: 'Mantenimiento del sistema',
      message: 'La plataforma se actualizará esta noche.',
      status: 'READ',
      createdAt: '2026-10-30T10:00:00Z',
      readAt: '2026-10-30T10:30:00Z',
    },
  ];

  beforeEach(() => {
    tokenServiceMock = {
      getUserId: vi.fn().mockReturnValue('user-1'),
      hasValidToken: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TokenService, useValue: tokenServiceMock },
      ],
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe inicializarse con señales vacías y valores por defecto', () => {
    expect(service.notifications()).toEqual([]);
    expect(service.unreadCount()).toBe(0);
    expect(service.isLoading()).toBe(false);
    expect(service.error()).toBeNull();
    expect(service.hasUnread()).toBe(false);
    expect(service.unreadBadgeText()).toBe('');
  });

  describe('getNotifications', () => {
    it('debe consultar GET /api/v1/notifications con page y size, y actualizar signals', () => {
      service.getNotifications(0, 20).subscribe((result) => {
        expect(result.length).toBe(2);
        expect(result).toEqual(mockNotifications);
      });

      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(
        (r) =>
          r.url === `${environment.apiUrl}/notifications` &&
          r.params.get('page') === '0' &&
          r.params.get('size') === '20',
      );
      expect(req.request.method).toBe('GET');

      req.flush(mockNotifications);

      expect(service.isLoading()).toBe(false);
      expect(service.notifications()).toEqual(mockNotifications);
      expect(service.error()).toBeNull();
    });

    it('debe manejar lista vacía correctamente', () => {
      service.getNotifications().subscribe((result) => {
        expect(result).toEqual([]);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=20`);
      req.flush([]);

      expect(service.notifications()).toEqual([]);
      expect(service.isLoading()).toBe(false);
    });

    it('debe registrar error y actualizar isLoading a false en fallo de red', () => {
      service.getNotifications().subscribe({
        next: () => expect.fail('Debió fallar'),
        error: (err) => {
          expect(err.status).toBe(500);
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=20`);
      req.flush({ message: 'Error interno del servidor' }, { status: 500, statusText: 'Internal Server Error' });

      expect(service.notifications()).toEqual([]);
      expect(service.error()).toBe('Error interno del servidor');
      expect(service.isLoading()).toBe(false);
    });

    it('debe descartar respuestas de solicitudes obsoletas si se emite una más reciente', () => {
      service.getNotifications(0, 10).subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=10`);

      service.getNotifications(1, 10).subscribe();
      const req2 = httpMock.expectOne(`${environment.apiUrl}/notifications?page=1&size=10`);

      const secondResult: Notification[] = [mockNotifications[1]];
      req2.flush(secondResult);

      expect(service.notifications()).toEqual(secondResult);

      req1.flush(mockNotifications);
      expect(service.notifications()).toEqual(secondResult);
    });

    it('debe retornar lista vacía y no consultar la API si la sesión es inválida', () => {
      tokenServiceMock.hasValidToken.mockReturnValue(false);
      tokenServiceMock.getUserId.mockReturnValue(null);

      let emitted: Notification[] | undefined;
      service.getNotifications().subscribe((res) => (emitted = res));

      expect(emitted).toEqual([]);
      httpMock.expectNone(`${environment.apiUrl}/notifications?page=0&size=20`);
    });
  });

  describe('getUnreadCount', () => {
    it('debe consultar GET /api/v1/notifications/unread/count y actualizar unreadCount', () => {
      const response: UnreadCountResponse = { unreadCount: 5 };

      service.getUnreadCount().subscribe((res) => {
        expect(res.unreadCount).toBe(5);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);
      expect(req.request.method).toBe('GET');
      req.flush(response);

      expect(service.unreadCount()).toBe(5);
      expect(service.hasUnread()).toBe(true);
      expect(service.unreadBadgeText()).toBe('5');
    });

    it('debe formatear badgeText como 99+ cuando supere 99', () => {
      service.unreadCount.set(120);
      expect(service.unreadBadgeText()).toBe('99+');
    });

    it('debe proteger el conteo frente a respuestas lentas desfasadas', () => {
      service.getUnreadCount().subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);

      service.getUnreadCount().subscribe();
      const req2 = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);

      req2.flush({ unreadCount: 2 });
      expect(service.unreadCount()).toBe(2);

      req1.flush({ unreadCount: 9 });
      expect(service.unreadCount()).toBe(2);
    });
  });

  describe('markAsRead & Consistencia 422 NOTIFICATION_ALREADY_READ', () => {
    beforeEach(() => {
      service.notifications.set([...mockNotifications]);
      service.unreadCount.set(1);
    });

    it('debe enviar PATCH /api/v1/notifications/:id/read y sincronizar contador', () => {
      const updatedFromServer: Notification = {
        ...mockNotifications[0],
        status: 'READ',
        readAt: '2026-11-01T12:00:00Z',
      };

      service.markAsRead('notif-1').subscribe((result) => {
        expect(result.status).toBe('READ');
        expect(result.readAt).toBe('2026-11-01T12:00:00Z');
      });

      expect(service.isMarkingRead('notif-1')).toBe(true);

      const patchReq = httpMock.expectOne(`${environment.apiUrl}/notifications/notif-1/read`);
      expect(patchReq.request.method).toBe('PATCH');
      patchReq.flush(updatedFromServer);

      // applyLocalReadStatus dispara getUnreadCount para sincronización segura
      const countReq = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);
      countReq.flush({ unreadCount: 0 });

      expect(service.isMarkingRead('notif-1')).toBe(false);
      expect(service.notifications()[0].status).toBe('READ');
      expect(service.unreadCount()).toBe(0);
    });

    it('debe prevenir peticiones HTTP duplicadas ante doble clic concurrente', () => {
      let callCount = 0;
      service.markAsRead('notif-1').subscribe(() => callCount++);
      service.markAsRead('notif-1').subscribe(() => callCount++);

      const patchRequests = httpMock.match(`${environment.apiUrl}/notifications/notif-1/read`);
      expect(patchRequests.length).toBe(1);

      patchRequests[0].flush({
        ...mockNotifications[0],
        status: 'READ',
        readAt: '2026-11-01T12:00:00Z',
      });

      const countReq = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);
      countReq.flush({ unreadCount: 0 });

      expect(callCount).toBe(2);
    });

    it('ante 422 NOTIFICATION_ALREADY_READ NO debe decrementar a ciegas sino reconsultar el conteo real del backend', () => {
      // Simular que el contador local ya estaba en 1
      service.unreadCount.set(1);

      service.markAsRead('notif-1').subscribe({
        next: (res) => {
          expect(res.status).toBe('READ');
        },
        error: () => expect.fail('No debió arrojar error para NOTIFICATION_ALREADY_READ'),
      });

      const patchReq = httpMock.expectOne(`${environment.apiUrl}/notifications/notif-1/read`);
      patchReq.flush(
        {
          code: 'NOTIFICATION_ALREADY_READ',
          message: 'La notificación ya fue marcada como leída.',
        },
        { status: 422, statusText: 'Unprocessable Content' },
      );

      // Debe dispararse de inmediato getUnreadCount autoritativo
      const countReq = httpMock.expectOne(`${environment.apiUrl}/notifications/unread/count`);
      // Si el servidor informa que ya había 0 sin leer, el contador se ajusta a 0 sin desfaces
      countReq.flush({ unreadCount: 0 });

      expect(service.notifications()[0].status).toBe('READ');
      expect(service.unreadCount()).toBe(0);
    });

    it('NO debe ocultar otros errores HTTP 422 con códigos diferentes de regla de negocio', () => {
      let caughtError: any = null;
      service.markAsRead('notif-1').subscribe({
        next: () => expect.fail('Debió fallar ante un 422 genérico o diferente'),
        error: (err) => {
          caughtError = err;
        },
      });

      const patchReq = httpMock.expectOne(`${environment.apiUrl}/notifications/notif-1/read`);
      patchReq.flush(
        { code: 'VALIDATION_ERROR', message: 'Parámetro inválido' },
        { status: 422, statusText: 'Unprocessable Content' },
      );

      expect(caughtError).toBeTruthy();
      expect(caughtError.status).toBe(422);
      expect(caughtError.error.code).toBe('VALIDATION_ERROR');
    });

    it('debe propagar otros errores de red y de servidor (ej. 404 o 500)', () => {
      let caughtError: any = null;
      service.markAsRead('notif-1').subscribe({
        next: () => expect.fail('Debió fallar'),
        error: (err) => {
          caughtError = err;
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/notifications/notif-1/read`);
      req.flush(
        { code: 'RESOURCE_NOT_FOUND', message: 'Notificación no encontrada' },
        { status: 404, statusText: 'Not Found' },
      );

      expect(caughtError).toBeTruthy();
      expect(caughtError.status).toBe(404);
      expect(service.isMarkingRead('notif-1')).toBe(false);
    });
  });

  describe('Aislamiento de Sesiones', () => {
    it('debe limpiar caché automáticamente si el usuario cambia respecto a la sesión anterior', () => {
      // 1. Sesión con user-1
      service.getNotifications().subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=20`);
      req1.flush(mockNotifications);

      expect(service.notifications().length).toBe(2);

      // 2. Cambio a user-2
      tokenServiceMock.getUserId.mockReturnValue('user-2');

      service.getNotifications().subscribe();
      const req2 = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=20`);

      // Antes de que req2 responda, la lista de user-1 ya no debe ser visible
      expect(service.notifications()).toEqual([]);

      req2.flush([]);
      expect(service.notifications()).toEqual([]);
    });

    it('debe descartar respuestas pendientes si ocurre logout antes de completarse la petición', () => {
      service.getNotifications().subscribe();
      const req = httpMock.expectOne(`${environment.apiUrl}/notifications?page=0&size=20`);

      // Logout explícito
      service.clearCache();
      tokenServiceMock.hasValidToken.mockReturnValue(false);
      tokenServiceMock.getUserId.mockReturnValue(null);

      // Llega la respuesta atrasada de la sesión previa
      req.flush(mockNotifications);

      // El estado debe permanecer completamente limpio
      expect(service.notifications()).toEqual([]);
      expect(service.unreadCount()).toBe(0);
    });

    it('debe resetear señales e invalidar secuencias en clearCache', () => {
      service.notifications.set([...mockNotifications]);
      service.unreadCount.set(3);
      service.error.set('Error previo');

      service.clearCache();

      expect(service.notifications()).toEqual([]);
      expect(service.unreadCount()).toBe(0);
      expect(service.isLoading()).toBe(false);
      expect(service.error()).toBeNull();
      expect(service.hasUnread()).toBe(false);
    });
  });
});
