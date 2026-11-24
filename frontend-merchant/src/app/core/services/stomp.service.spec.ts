import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getWebSocketUrl, StompService } from './stomp.service';
import { TokenService } from './token-service';
import { InvalidationBusService } from './invalidation-bus.service';
import { OrderConfirmedDomainEvent, OrderStatusChangedDomainEvent } from '../models/order.models';

describe('StompService', () => {
  let service: StompService;
  let invalidationBus: InvalidationBusService;
  let tokenServiceMock: {
    getToken: ReturnType<typeof vi.fn>;
    isTokenExpired: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    tokenServiceMock = {
      getToken: vi.fn(),
      isTokenExpired: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        StompService,
        InvalidationBusService,
        { provide: TokenService, useValue: tokenServiceMock },
      ],
    });

    service = TestBed.inject(StompService);
    invalidationBus = TestBed.inject(InvalidationBusService);
  });

  describe('getWebSocketUrl', () => {
    it('debe convertir http:// en ws:// y apuntar al path /ws nativo', () => {
      const wsUrl = getWebSocketUrl('http://localhost:8080/api/v1');
      expect(wsUrl).toBe('ws://localhost:8080/ws');
    });

    it('debe convertir https:// en wss:// y apuntar al path /ws nativo', () => {
      const wsUrl = getWebSocketUrl('https://jaldishop-api.onrender.com/api/v1');
      expect(wsUrl).toBe('wss://jaldishop-api.onrender.com/ws');
    });
  });

  describe('Ciclo de vida y autenticación', () => {
    it('debe inicializarse en estado DISCONNECTED y sin suscripciones activas', () => {
      expect(service.connectionState()).toBe('DISCONNECTED');
      expect(service.isSubscribed()).toBe(false);
      expect(service.currentStoreId()).toBeNull();
    });

    it('debe transicionar a AUTH_ERROR si no hay token al intentar conectar', () => {
      tokenServiceMock.getToken.mockReturnValue(null);

      service.connect('store-123');

      expect(service.connectionState()).toBe('AUTH_ERROR');
      expect(service.isSubscribed()).toBe(false);
      expect(service.getClient()).toBeNull();
    });

    it('debe transicionar a AUTH_ERROR si el token está expirado', () => {
      tokenServiceMock.getToken.mockReturnValue('expired.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(true);

      service.connect('store-123');

      expect(service.connectionState()).toBe('AUTH_ERROR');
      expect(service.getClient()).toBeNull();
    });

    it('debe instanciar el cliente STOMP con Authorization dinámica en CONNECT', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-123');

      const client = service.getClient();
      expect(client).toBeTruthy();
      expect(service.connectionState()).toBe('CONNECTING');

      // Simular beforeConnect
      client?.beforeConnect?.(client);
      expect(client?.connectHeaders).toEqual({
        Authorization: 'Bearer valid.jwt.token',
      });
    });

    it('debe detener la conexión en beforeConnect si el token expira antes del handshake', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-123');
      const client = service.getClient();

      // Antes de conectar se invalida el token
      tokenServiceMock.isTokenExpired.mockReturnValue(true);
      const deactivateSpy = vi.spyOn(client!, 'deactivate');

      client?.beforeConnect?.(client!);
      expect(service.connectionState()).toBe('AUTH_ERROR');
      expect(deactivateSpy).toHaveBeenCalled();
    });
  });

  describe('Suscripción y eventos de tienda', () => {
    it('debe suscribirse al topic /topic/stores/{storeId}/orders al conectar exitosamente', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;

      // Simular cliente conectado
      Object.defineProperty(client, 'connected', { value: true, configurable: true });
      const subscribeSpy = vi.spyOn(client, 'subscribe').mockReturnValue({
        id: 'sub-1',
        unsubscribe: vi.fn(),
      });

      // Disparar onConnect
      client.onConnect?.({} as any);

      expect(service.connectionState()).toBe('CONNECTED');
      expect(subscribeSpy).toHaveBeenCalledWith(
        '/topic/stores/store-uuid-abc/orders',
        expect.any(Function),
      );
      expect(service.isSubscribed()).toBe(true);
      expect(service.currentStoreId()).toBe('store-uuid-abc');
    });

    it('debe emitir ORDER_CONFIRMED en orderConfirmed$ al recibir un mensaje STOMP válido de la tienda activa', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });

      let messageCallback: (msg: any) => void = () => {};
      vi.spyOn(client, 'subscribe').mockImplementation((_dest, cb) => {
        messageCallback = cb as any;
        return { id: 'sub-1', unsubscribe: vi.fn() };
      });

      client.onConnect?.({} as any);

      const receivedEvents: OrderConfirmedDomainEvent[] = [];
      const busEvents: any[] = [];
      service.orderConfirmed$.subscribe((event) => receivedEvents.push(event));
      invalidationBus.orderCreated$.subscribe((event) => busEvents.push(event));

      const eventPayload: OrderConfirmedDomainEvent = {
        eventId: 'evt-1',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-999',
        orderNumber: '#PED-999',
        storeId: 'store-uuid-abc',
        totalAmount: 145.0,
        currency: 'PEN',
        occurredAt: '2026-10-31T12:00:00Z',
      };

      messageCallback({ body: JSON.stringify(eventPayload) });

      expect(receivedEvents.length).toBe(1);
      expect(receivedEvents[0].orderNumber).toBe('#PED-999');
      expect(receivedEvents[0].totalAmount).toBe(145.0);
      expect(busEvents.length).toBe(1);
      expect(busEvents[0]).toEqual({
        storeId: 'store-uuid-abc',
        orderId: 'ord-999',
        orderNumber: '#PED-999',
        origin: undefined,
        source: 'WEBSOCKET',
      });
    });

    it('debe descartar frames duplicados con el mismo eventId de forma reactiva mediante caché LRU', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });

      let messageCallback: (msg: any) => void = () => {};
      vi.spyOn(client, 'subscribe').mockImplementation((_dest, cb) => {
        messageCallback = cb as any;
        return { id: 'sub-1', unsubscribe: vi.fn() };
      });

      client.onConnect?.({} as any);

      const receivedEvents: OrderConfirmedDomainEvent[] = [];
      service.orderConfirmed$.subscribe((event) => receivedEvents.push(event));

      const eventPayload: OrderConfirmedDomainEvent = {
        eventId: 'evt-duplicate-1',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-111',
        orderNumber: '#PED-111',
        storeId: 'store-uuid-abc',
        totalAmount: 50.0,
        currency: 'PEN',
        occurredAt: '2026-10-31T12:00:00Z',
      };

      // Primer frame: debe procesarse
      messageCallback({ body: JSON.stringify(eventPayload) });
      expect(receivedEvents.length).toBe(1);

      // Segundo frame idéntico (reentrega STOMP): debe ser descartado
      messageCallback({ body: JSON.stringify(eventPayload) });
      expect(receivedEvents.length).toBe(1);
    });

    it('debe desalojar el eventId más antiguo al exceder la capacidad máxima de la caché LRU', () => {
      for (let i = 0; i < 200; i++) {
        expect(service.isDuplicateEvent(`event-${i}`)).toBe(false);
      }
      // 'event-0' está en caché
      expect(service.isDuplicateEvent('event-0')).toBe(true);

      // Insertar el elemento 201 (provoca desalojo de 'event-0')
      expect(service.isDuplicateEvent('event-200')).toBe(false);

      // 'event-0' ya fue desalojado por LRU
      expect(service.isDuplicateEvent('event-0')).toBe(false);
    });

    it('debe recibir y emitir eventos ORDER_STATUS_CHANGED para la tienda activa', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });

      let messageCallback: (msg: any) => void = () => {};
      vi.spyOn(client, 'subscribe').mockImplementation((_dest, cb) => {
        messageCallback = cb as any;
        return { id: 'sub-1', unsubscribe: vi.fn() };
      });

      client.onConnect?.({} as any);

      const receivedEvents: OrderStatusChangedDomainEvent[] = [];
      const busEvents: any[] = [];
      service.orderStatusChanged$.subscribe((event) => receivedEvents.push(event));
      invalidationBus.orderMutated$.subscribe((event) => busEvents.push(event));

      const eventPayload: OrderStatusChangedDomainEvent = {
        eventId: 'evt-200',
        eventType: 'ORDER_STATUS_CHANGED',
        orderId: 'ord-100',
        orderNumber: '#PED-100',
        customerId: 'cust-1',
        storeId: 'store-uuid-abc',
        previousStatus: 'CONFIRMED',
        newStatus: 'IN_PREPARATION',
        occurredAt: '2026-10-31T12:05:00Z',
      };

      messageCallback({ body: JSON.stringify(eventPayload) });

      expect(receivedEvents.length).toBe(1);
      expect(receivedEvents[0].orderNumber).toBe('#PED-100');
      expect(receivedEvents[0].newStatus).toBe('IN_PREPARATION');
      expect(busEvents.length).toBe(1);
      expect(busEvents[0]).toEqual({
        storeId: 'store-uuid-abc',
        orderId: 'ord-100',
        status: 'IN_PREPARATION',
        previousStatus: 'CONFIRMED',
        source: 'WEBSOCKET',
      });
    });

    it('debe descartar eventos con storeId ajeno a la tienda activa', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });

      let messageCallback: (msg: any) => void = () => {};
      vi.spyOn(client, 'subscribe').mockImplementation((_dest, cb) => {
        messageCallback = cb as any;
        return { id: 'sub-1', unsubscribe: vi.fn() };
      });

      client.onConnect?.({} as any);

      const receivedEvents: OrderConfirmedDomainEvent[] = [];
      service.orderConfirmed$.subscribe((event) => receivedEvents.push(event));

      const eventAjenas: OrderConfirmedDomainEvent = {
        eventId: 'evt-2',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-999',
        orderNumber: '#PED-999',
        storeId: 'store-otra-tienda',
        totalAmount: 50.0,
        currency: 'PEN',
        occurredAt: '2026-10-31T12:00:00Z',
      };

      messageCallback({ body: JSON.stringify(eventAjenas) });
      expect(receivedEvents.length).toBe(0);
    });

    it('debe emitir reconnected$ en reconexiones posteriores para activar reconciliación REST', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });
      vi.spyOn(client, 'subscribe').mockReturnValue({ id: 'sub-1', unsubscribe: vi.fn() });

      let reconnectedCount = 0;
      let busReconnectedCount = 0;
      service.reconnected$.subscribe(() => reconnectedCount++);
      invalidationBus.reconnected$.subscribe(() => busReconnectedCount++);

      // Primera conexión
      client.onConnect?.({} as any);
      expect(reconnectedCount).toBe(0);
      expect(busReconnectedCount).toBe(0);

      // Desconexión temporal y reconexión
      client.onWebSocketClose?.({} as any);
      expect(service.connectionState()).toBe('RECONNECTING');

      client.onConnect?.({} as any);
      expect(service.connectionState()).toBe('CONNECTED');
      expect(reconnectedCount).toBe(1);
      expect(busReconnectedCount).toBe(1);
    });

    it('debe detener la reconexión y transicionar a AUTH_ERROR si el backend envía error de permisos o credenciales', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;

      client.onStompError?.({
        command: 'ERROR',
        headers: { message: 'AuthenticationCredentialsNotFoundException: Token expired' },
        body: 'Access Denied',
      } as any);

      expect(service.connectionState()).toBe('AUTH_ERROR');
      expect(service.isSubscribed()).toBe(false);
    });

    it('debe cancelar la suscripción limpiamente al invocar unsubscribe()', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      Object.defineProperty(client, 'connected', { value: true, configurable: true });

      const unsubscribeSpy = vi.fn();
      vi.spyOn(client, 'subscribe').mockReturnValue({
        id: 'sub-1',
        unsubscribe: unsubscribeSpy,
      });

      client.onConnect?.({} as any);
      expect(service.isSubscribed()).toBe(true);

      service.unsubscribe();

      expect(unsubscribeSpy).toHaveBeenCalled();
      expect(service.isSubscribed()).toBe(false);
      expect(service.currentStoreId()).toBeNull();
    });

    it('debe desconectar completamente y desactivar el cliente al invocar disconnect()', () => {
      tokenServiceMock.getToken.mockReturnValue('valid.jwt.token');
      tokenServiceMock.isTokenExpired.mockReturnValue(false);

      service.connect('store-uuid-abc');
      const client = service.getClient()!;
      const deactivateSpy = vi.spyOn(client, 'deactivate');

      service.disconnect();

      expect(deactivateSpy).toHaveBeenCalled();
      expect(service.connectionState()).toBe('DISCONNECTED');
      expect(service.getClient()).toBeNull();
    });
  });
});
