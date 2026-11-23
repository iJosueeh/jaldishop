import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { ToastService } from './toast.service';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import { InvalidationBusService } from './invalidation-bus.service';
import {
  CreateMerchantOrderRequest,
  MerchantOrderDetailResponse,
  MerchantOrderPageResponse,
  MerchantOrder,
  MerchantOrderStatusCounts,
} from '../models/order.models';
import { getOperatingDateLima } from '../utils/date.utils';
import { environment } from '../../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;
  let storeService: StoreService;
  let invalidationBus: InvalidationBusService;
  let toastMock: any;

  const mockPageResponse: MerchantOrderPageResponse = {
    content: [
      {
        id: 'ord-1039',
        orderNumber: '#PED-1039',
        storeId: 'store-123',
        customerUserId: 'usr-1',
        status: 'IN_PREPARATION',
        deliveryMode: 'DELIVERY',
        serviceDate: '2026-10-31',
        serviceStartTime: '12:00:00',
        serviceEndTime: '13:00:00',
        customerName: 'Valeria Ramos',
        customerPhone: '984552109',
        deliveryAddress: 'Av. José Pardo 450',
        deliveryReference: null,
        totalAmount: 58.0,
        currency: 'PEN',
        totalItems: 1,
        confirmedAt: '2026-10-31T12:00:00Z',
        updatedAt: '2026-10-31T12:00:00Z',
        items: [
          {
            id: 'item-1',
            variantId: 'var-1',
            productName: 'Brownies',
            variantName: 'Caja x6',
            quantity: 1,
            unitPriceAmount: 36,
            subtotalAmount: 36,
          },
        ],
      },
    ],
    page: 0,
    size: 10,
    totalElements: 25,
    totalPages: 3,
    first: true,
    last: false,
  };

  const mockDetailResponse: MerchantOrderDetailResponse = {
    id: 'ord-1039',
    orderNumber: '#PED-1039',
    storeId: 'store-123',
    customerUserId: 'usr-1',
    paymentId: 'pay-1',
    capacityReservationId: 'res-1',
    status: 'IN_PREPARATION',
    deliveryMode: 'DELIVERY',
    serviceDate: '2026-10-31',
    serviceStartTime: '12:00:00',
    serviceEndTime: '13:00:00',
    customerName: 'Valeria Ramos',
    customerPhone: '984552109',
    customerEmail: 'valeria@example.com',
    deliveryAddress: 'Av. José Pardo 450',
    deliveryReference: 'Frente al parque',
    deliveryLatitude: -12.12,
    deliveryLongitude: -77.03,
    currency: 'PEN',
    productsSubtotalAmount: 50.0,
    discountAmount: 0,
    discountCode: null,
    deliveryFeeAmount: 8.0,
    includedTaxAmount: 0,
    totalAmount: 58.0,
    confirmedAt: '2026-10-31T12:00:00Z',
    updatedAt: '2026-10-31T12:00:00Z',
    items: [
      {
        id: 'item-1',
        variantId: 'var-1',
        productName: 'Brownies',
        variantName: 'Caja x6',
        quantity: 1,
        unitPriceAmount: 36,
        subtotalAmount: 36,
      },
    ],
    statusHistory: [
      {
        id: 'sh-1',
        changedByUserId: 'usr-admin',
        status: 'CONFIRMED',
        reason: 'Pago verificado',
        changedAt: '2026-10-31T12:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    toastMock = {
      success: vi.fn(),
      info: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        OrderService,
        StoreService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toastMock },
      ],
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
    storeService = TestBed.inject(StoreService);
    invalidationBus = TestBed.inject(InvalidationBusService);

    // Configurar tienda activa por defecto
    storeService.currentStore.set({
      id: 'store-123',
      merchantUserId: 'usr-merchant',
      name: 'Pastelería Dulce',
      slug: 'pasteleria-dulce',
      pickupEnabled: true,
      deliveryEnabled: true,
      status: 'ACTIVE',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    });
  });

  afterEach(() => {
    service?.ngOnDestroy();
    httpMock.verify();
    vi.clearAllTimers();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('debe crearse correctamente e inicializar vacío', () => {
    expect(service).toBeTruthy();
    expect(service.orders().length).toBe(0);
    expect(service.totalElements()).toBe(0);
    expect(service.totalPages()).toBe(0);
    expect(service.currentPage()).toBe(0);
    expect(service.isEmptyOrders()).toBe(false);
  });

  it('debe realizar GET al listado de pedidos con storeId, page y size correctos', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe((res) => {
      expect(res).toBeTruthy();
      expect(res?.totalElements).toBe(25);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        !r.params.has('status'),
    );
    expect(req.request.method).toBe('GET');

    req.flush(mockPageResponse);

    expect(service.orders().length).toBe(1);
    expect(service.totalElements()).toBe(25);
    expect(service.totalPages()).toBe(3);
    expect(service.currentPage()).toBe(0);
    expect(service.isLoading()).toBe(false);
    expect(service.isLoaded()).toBe(true);
  });

  it('debe enviar el parámetro status cuando el filtro no es ALL', () => {
    service.loadOrders(1, 20, 'IN_PREPARATION', 'store-123').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '20' &&
        r.params.get('status') === 'IN_PREPARATION',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ ...mockPageResponse, page: 1, size: 20 });

    expect(service.currentPage()).toBe(1);
    expect(service.pageSize()).toBe(20);
  });

  it('debe resolver la tienda vía getMyStore si currentStore es nulo', () => {
    storeService.currentStore.set(null);

    service.loadOrders(0, 10, 'ALL').subscribe();

    // 1. Solicitud a /stores/me
    const storeReq = httpMock.expectOne(`${environment.apiUrl}/stores/me`);
    expect(storeReq.request.method).toBe('GET');
    storeReq.flush({ id: 'store-resolved', name: 'Mi Tienda' });

    // 2. Solicitud a /merchants/stores/store-resolved/orders
    const ordersReq = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/merchants/stores/store-resolved/orders`,
    );
    expect(ordersReq.request.method).toBe('GET');
    ordersReq.flush(mockPageResponse);

    expect(service.orders().length).toBe(1);
  });

  it('debe descartar respuestas obsoletas del listado si ocurre otra petición en paralelo (race condition)', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
    service.loadOrders(1, 10, 'READY', 'store-123').subscribe();

    const req1 = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '0',
    );
    const req2 = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '1',
    );

    // Responder la petición 2 primero
    req2.flush({
      ...mockPageResponse,
      page: 1,
      content: [{ ...mockPageResponse.content[0], id: 'ord-page-1' }],
    });

    // Luego llega tarde la petición 1 (obsoleta)
    req1.flush({
      ...mockPageResponse,
      page: 0,
      content: [{ ...mockPageResponse.content[0], id: 'ord-page-0' }],
    });

    // Se conserva la respuesta más reciente (req2)
    expect(service.currentPage()).toBe(1);
    expect(service.orders()[0].id).toBe('ord-page-1');
  });

  it('debe manejar error 403 Forbidden mostrando toast y seteando señal de error', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/merchants/stores/store-123/orders`,
    );
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    expect(service.error()).toBe('No tienes permisos para ver los pedidos de esta tienda');
    expect(toastMock.error).toHaveBeenCalledWith(
      'No tienes permisos para ver los pedidos de esta tienda.',
    );
    expect(service.isLoading()).toBe(false);
  });

  it('debe manejar error 500 del servidor', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/merchants/stores/store-123/orders`,
    );
    req.flush({ message: 'Internal Error' }, { status: 500, statusText: 'Internal Server Error' });

    expect(service.error()).toBe('Error al cargar pedidos del servidor');
    expect(toastMock.error).toHaveBeenCalledWith('No se pudieron cargar los pedidos.');
  });

  it('debe cargar el detalle del pedido vía GET al abrir el drawer', () => {
    const summaryOrder: MerchantOrder = {
      id: 'ord-1039',
      orderNumber: '#PED-1039',
      customerName: 'Valeria Ramos',
      deliveryMode: 'DELIVERY',
      deliveryTimeLabel: '12:00 - 13:00',
      isUrgent: false,
      status: 'IN_PREPARATION',
      totalAmount: 58.0,
      items: [],
      createdAt: '2026-10-31T12:00:00Z',
    };

    service.openDrawer(summaryOrder);
    expect(service.isDrawerOpen()).toBe(true);
    expect(service.selectedOrder()?.id).toBe('ord-1039');
    expect(service.isDetailLoading()).toBe(true);

    const detailReq = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039`,
    );
    expect(detailReq.request.method).toBe('GET');
    detailReq.flush(mockDetailResponse);

    expect(service.isDetailLoading()).toBe(false);
    expect(service.selectedOrderDetail()).toBeTruthy();
    expect(service.selectedOrderDetail()?.customerName).toBe('Valeria Ramos');
    expect(service.selectedOrderDetail()?.items?.length).toBe(1);
  });

  it('debe descartar detalle obsoleto si se selecciona otro pedido antes de que termine la primera petición', () => {
    service.getOrderDetail('store-123', 'ord-A').subscribe();
    service.getOrderDetail('store-123', 'ord-B').subscribe();

    const reqA = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/store-123/orders/ord-A`,
    );
    const reqB = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/store-123/orders/ord-B`,
    );

    // reqB termina primero
    reqB.flush({ ...mockDetailResponse, id: 'ord-B', customerName: 'Cliente B' });
    // reqA llega tarde
    reqA.flush({ ...mockDetailResponse, id: 'ord-A', customerName: 'Cliente A' });

    expect(service.selectedOrderDetail()?.id).toBe('ord-B');
    expect(service.selectedOrderDetail()?.customerName).toBe('Cliente B');
  });

  it('debe cerrar el drawer y resetear las señales de detalle', () => {
    service.closeDrawer();
    expect(service.isDrawerOpen()).toBe(false);
    expect(service.selectedOrder()).toBeNull();
    expect(service.selectedOrderDetail()).toBeNull();
    expect(service.detailError()).toBeNull();
  });

  it('debe reiniciar a la página 0 al cambiar de pestaña activa y cargar nuevos pedidos', () => {
    service.currentPage.set(2);
    service.setActiveTab('READY');

    expect(service.activeTab()).toBe('READY');
    expect(service.currentPage()).toBe(0);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '0' &&
        r.params.get('status') === 'READY',
    );
    req.flush(mockPageResponse);
  });

  it('debe cambiar de página con setPage y ejecutar GET con la página indicada', () => {
    service.setPage(3);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('page') === '3',
    );
    req.flush({ ...mockPageResponse, page: 3 });
    expect(service.currentPage()).toBe(3);
  });

  it('debe enviar el parámetro search cuando appliedSearchQuery o el argumento search contiene texto', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123', 'Carlos').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('search') === 'Carlos',
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPageResponse);
  });

  it('no debe enviar el parámetro search si el término es vacío o solo contiene espacios', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123', '   ').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        !r.params.has('search'),
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPageResponse);
  });

  it('debe recortar el parámetro search a un máximo de 100 caracteres', () => {
    const longQuery = 'A'.repeat(120);
    service.loadOrders(0, 10, 'ALL', 'store-123', longQuery).subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
        r.params.get('search') === 'A'.repeat(100),
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPageResponse);
  });

  describe('updateOrderStatus (PATCH)', () => {
    const existingOrder: MerchantOrder = {
      id: 'ord-1039',
      orderNumber: '#PED-1039',
      customerName: 'Valeria Ramos',
      customerPhone: '984552109',
      deliveryMode: 'DELIVERY',
      deliveryAddress: 'Av. José Pardo 450',
      deliveryTimeLabel: '12:00 - 13:00',
      isUrgent: false,
      status: 'CONFIRMED',
      totalAmount: 58.0,
      items: [],
      createdAt: '2026-10-31T12:00:00Z',
    };

    beforeEach(() => {
      service.orders.set([existingOrder]);
      service.totalElements.set(1);
    });

    it('debe enviar PATCH con URL y body { status } aplicando mutación optimista y confirmando tras HTTP 200', () => {
      expect(service.orders()[0].status).toBe('CONFIRMED');

      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      // Debe reflejar mutación optimista inmediata en memoria (< 5ms) mientras sincroniza
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(service.isUpdating('ord-1039')).toBe(true);

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      expect(patchReq.request.method).toBe('PATCH');
      expect(patchReq.request.body).toEqual({ status: 'IN_PREPARATION' });

      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      // Tras HTTP 200, el estado se actualiza en el store reactivo
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(service.isUpdating('ord-1039')).toBe(false);
      expect(toastMock.success).toHaveBeenCalledWith('Pedido actualizado a en preparación.');
    });

    it('debe revertir (rollback) el estado optimista local al estado anterior si el backend retorna error 422', () => {
      expect(service.orders()[0].status).toBe('CONFIRMED');

      service.updateOrderStatus('ord-1039', 'READY', 'store-123').subscribe();

      // Inmediatamente optimista
      expect(service.orders()[0].status).toBe('READY');

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush(
        {
          code: 'RN-ORD-07',
          message: 'Transición inválida de estado: no se puede pasar de CONFIRMED a READY',
        },
        { status: 422, statusText: 'Unprocessable Entity' },
      );

      // Tras el error, debe haberse revertido al estado anterior
      expect(service.orders()[0].status).toBe('CONFIRMED');
      expect(service.isUpdating('ord-1039')).toBe(false);
      expect(toastMock.error).toHaveBeenCalledWith(
        'Transición inválida de estado: no se puede pasar de CONFIRMED a READY',
      );
    });

    it('debe enviar motivo de cancelación en el payload PATCH cuando se proporciona', () => {
      service.updateOrderStatus('ord-1039', 'CANCELLED', 'store-123', 'Insumos agotados').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      expect(patchReq.request.method).toBe('PATCH');
      expect(patchReq.request.body).toEqual({
        status: 'CANCELLED',
        reason: 'Insumos agotados',
      });

      patchReq.flush({
        ...mockDetailResponse,
        status: 'CANCELLED',
      });
      expect(toastMock.success).toHaveBeenCalledWith('Pedido actualizado a cancelado.');
    });

    it('debe evitar peticiones duplicadas ante clics repetidos mientras la operación esté en vuelo', () => {
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      expect(service.isUpdating('ord-1039')).toBe(true);

      // Segundo intento inmediato mientras el primero está pendiente
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      // Solo una solicitud HTTP debe haber sido enviada
      const requests = httpMock.match(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      expect(requests.length).toBe(1);

      requests[0].flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });
      expect(service.isUpdating('ord-1039')).toBe(false);
    });

    it('debe manejar error 422 de regla de negocio sin alterar el estado local', () => {
      service.updateOrderStatus('ord-1039', 'COMPLETED', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush(
        {
          code: 'RN-ORD-07',
          message: 'Transición inválida de estado: no se puede pasar de CONFIRMED a COMPLETED',
        },
        { status: 422, statusText: 'Unprocessable Entity' },
      );

      // El estado debe preservarse intacto
      expect(service.orders()[0].status).toBe('CONFIRMED');
      expect(service.isUpdating('ord-1039')).toBe(false);
      expect(toastMock.error).toHaveBeenCalledWith(
        'Transición inválida de estado: no se puede pasar de CONFIRMED a COMPLETED',
      );
    });

    it('debe manejar error 403 Forbidden y error 404 Not Found', () => {
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      const req403 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      req403.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
      expect(toastMock.error).toHaveBeenCalledWith(
        'No tienes permisos para modificar pedidos de esta tienda.',
      );

      service.updateOrderStatus('ord-inexistente', 'IN_PREPARATION', 'store-123').subscribe();
      const req404 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-inexistente/status`,
      );
      req404.flush({ message: 'Not Found' }, { status: 404, statusText: 'Not Found' });
      expect(toastMock.error).toHaveBeenCalledWith(
        'El pedido no existe o no pertenece a esta tienda.',
      );
    });

    it('debe conciliar el estado mediante GET ante error de red o 500 por incertidumbre en PostgreSQL', () => {
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      // Fallo de red (status 0) o 500
      patchReq.flush({ message: 'Network Failure' }, { status: 0, statusText: 'Unknown Error' });

      expect(toastMock.error).toHaveBeenCalledWith(
        'Error de comunicación con el servidor. Reconciliando estado del pedido...',
      );

      // Debe dispararse automáticamente GET de reconciliación
      const getReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039`,
      );
      expect(getReq.request.method).toBe('GET');

      // Si el commit en backend sí se aplicó antes de caer la red:
      getReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      // El estado local se reconcilia con la verdad de PostgreSQL
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(toastMock.info).toHaveBeenCalledWith(
        'Estado del pedido sincronizado con el servidor.',
      );
    });

    it('debe sincronizar selectedOrder y selectedOrderDetail en el Drawer', () => {
      service.selectedOrder.set(existingOrder);
      service.selectedOrderDetail.set({
        id: 'ord-1039',
        orderNumber: '#PED-1039',
        customerName: 'Valeria Ramos',
        icon: 'matRestaurantOutline',
        itemSummary: 'Brownies',
        deliveryMode: 'DELIVERY',
        scheduledTime: '12:00',
        deliveryTimeLabel: '12:00 - 13:00',
        isUrgent: false,
        status: 'CONFIRMED',
      });

      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      expect(service.selectedOrder()?.status).toBe('IN_PREPARATION');
      expect(service.selectedOrderDetail()?.status).toBe('IN_PREPARATION');
    });

    it('debe recargar la página actual si el pedido deja de pertenecer al filtro activo', () => {
      service.activeTab.set('CONFIRMED');

      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      // Al no pertenecer a 'CONFIRMED', debe invocar loadOrders para refrescar el filtro y totalElements
      const reloadReq = httpMock.expectOne(
        (r) => r.url.includes('/orders') && r.params.get('status') === 'CONFIRMED',
      );
      expect(reloadReq.request.method).toBe('GET');
      reloadReq.flush({
        ...mockPageResponse,
        content: [],
        totalElements: 0,
      });

      expect(service.orders().length).toBe(0);
      expect(service.totalElements()).toBe(0);
    });

    it('debe marcar el pedido como pendiente de verificación si el PATCH y el GET de reconciliación fallan', () => {
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      // Fallo de red (status 0)
      patchReq.flush({ message: 'Network Failure' }, { status: 0, statusText: 'Unknown Error' });

      // GET de reconciliación también falla
      const getReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039`,
      );
      getReq.flush({ message: 'Server Timeout' }, { status: 504, statusText: 'Gateway Timeout' });

      expect(service.isPendingVerification('ord-1039')).toBe(true);
      expect(toastMock.warning).toHaveBeenCalledWith(
        'No se pudo verificar el estado en el servidor. El pedido quedó marcado como pendiente de verificación.',
      );

      // Bloqueo de acciones ciegas adicionales mientras esté en verificación pendiente
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      httpMock.expectNone(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
    });

    it('debe permitir verificación manual mediante verifyOrderStatus y sincronizar si ya se aplicó el cambio', () => {
      // Pedido previamente en estado pendiente de verificación
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush({ message: 'Network Failure' }, { status: 0, statusText: 'Unknown Error' });
      const getReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039`,
      );
      getReq.flush({ message: 'Error' }, { status: 500, statusText: 'Internal Error' });

      expect(service.isPendingVerification('ord-1039')).toBe(true);

      // Verificación manual
      service.verifyOrderStatus('ord-1039', 'store-123').subscribe();
      const manualGetReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039`,
      );
      manualGetReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      expect(service.isPendingVerification('ord-1039')).toBe(false);
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(toastMock.success).toHaveBeenCalledWith(
        'Estado del pedido verificado y sincronizado con el servidor.',
      );
    });

    it('debe proteger operaciones PATCH en curso contra sobrescrituras causadas por respuestas REST', () => {
      // Simular que ord-1039 está en vuelo con actualización
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      expect(service.isUpdating('ord-1039')).toBe(true);

      // Mientras el PATCH está en vuelo, llega una respuesta de listado REST con el estado antiguo 'CONFIRMED'
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
      const listReq = httpMock.expectOne((r) => r.url.endsWith('/orders'));
      listReq.flush({
        ...mockPageResponse,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-1039',
            status: 'CONFIRMED', // respuesta antigua
          },
        ],
      });

      // El pedido en vuelo optimista no debe haber sido pisoteado
      expect(service.orders()[0].status).toBe('IN_PREPARATION');

      // Ahora el PATCH responde exitosamente
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(service.isUpdating('ord-1039')).toBe(false);
    });

    it('debe descartar una respuesta GET de listado que responda después de que un PATCH haya finalizado con éxito', () => {
      // 1. Se inicia una consulta de listado
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
      const listReq = httpMock.expectOne((r) => r.url.endsWith('/orders'));

      // 2. Antes de que el listado responda, el usuario ejecuta un PATCH sobre ord-1039
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );

      // 3. El PATCH responde primero con HTTP 200 y aplica el estado IN_PREPARATION
      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });
      expect(service.orders()[0].status).toBe('IN_PREPARATION');

      // Se emite la petición de reconciliación porque había una consulta de listado en vuelo
      const reconReq = httpMock.expectOne((r) => r.url.endsWith('/orders'));

      // 4. Ahora llega la respuesta retrasada del listado con el snapshot antiguo CONFIRMED
      listReq.flush({
        ...mockPageResponse,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-1039',
            status: 'CONFIRMED',
          },
        ],
      });

      // 5. La respuesta del listado debe haber sido descartada y el estado confirmado se mantiene
      expect(service.orders()[0].status).toBe('IN_PREPARATION');

      // 6. Responde la reconciliación y confirma estado final
      reconReq.flush({
        ...mockPageResponse,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-1039',
            status: 'IN_PREPARATION',
          },
        ],
      });
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(service.isLoading()).toBe(false);
    });

    it('debe enviar PATCH con status CANCELLED y actualizar el estado', () => {
      service.orders.set([
        {
          id: 'ord-123',
          orderNumber: '#PED-0001',
          customerName: 'Cliente Prueba',
          deliveryMode: 'DELIVERY',
          deliveryTimeLabel: '10:00 - 11:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50,
          items: [],
          createdAt: new Date().toISOString(),
        },
      ]);

      service.updateOrderStatus('ord-123', 'CANCELLED', 'store-123').subscribe();

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-123/status`,
      );
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'CANCELLED' });

      req.flush({
        ...mockDetailResponse,
        id: 'ord-123',
        status: 'CANCELLED',
      });

      expect(service.orders()[0].status).toBe('CANCELLED');
      expect(toastMock.success).toHaveBeenCalledWith('Pedido actualizado a cancelado.');
    });

    it('addOrder no debe agregar pedidos ficticios en memoria (Fase 1)', () => {
      const initialCount = service.orders().length;
      service.addOrder({
        id: 'fake-1',
        orderNumber: '#FAKE-001',
        customerName: 'Fake',
        deliveryMode: 'PICKUP',
        deliveryTimeLabel: '10:00',
        isUrgent: false,
        status: 'CONFIRMED',
        totalAmount: 10,
        items: [],
        createdAt: new Date().toISOString(),
      });

      expect(service.orders().length).toBe(initialCount);
      expect(toastMock.info).toHaveBeenCalledWith(
        'La creación manual de pedidos estará disponible próximamente.',
      );
    });

    it('debe mantener channelFilter bloqueado en ALL y no descartar pedidos por canales no soportados', () => {
      service.orders.set([
        {
          id: 'ord-channel-test',
          orderNumber: '#PED-CH-1',
          customerName: 'Cliente Canal',
          deliveryMode: 'PICKUP',
          deliveryTimeLabel: '10:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50,
          items: [],
          createdAt: new Date().toISOString(),
          channel: undefined,
          channelLabel: undefined,
        },
      ]);

      service.setChannelFilter('WHATSAPP');
      expect(service.channelFilter()).toBe('ALL');
      expect(service.filteredOrders().length).toBe(1);
    });
  });

  describe('Integración de Tiempo Real STOMP con OrderService', () => {
    it('debe disparar loadOrders y notificación cuando StompService emite ORDER_CONFIRMED para la tienda activa', () => {
      const stompService = TestBed.inject(StompService);

      // Simular evento ORDER_CONFIRMED emitido por el socket
      (stompService as any).orderConfirmedSubject.next({
        eventId: 'evt-100',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-new',
        orderNumber: '#PED-2026',
        storeId: 'store-123',
        totalAmount: 95.0,
        currency: 'PEN',
        occurredAt: '2026-10-31T12:00:00Z',
      });

      expect(toastMock.info).toHaveBeenCalledWith('Nuevo pedido recibido: #PED-2026');

      // Debe consultar el listado actualizado al servidor
      const req = httpMock.expectOne((r) => r.url.includes('/orders'));
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });

    it('debe actualizar orden localmente y recargar datos cuando StompService emite ORDER_STATUS_CHANGED para la tienda activa', () => {
      const stompService = TestBed.inject(StompService);

      service.orders.set([
        {
          id: 'ord-status-1',
          orderNumber: '#PED-STATUS',
          customerName: 'Cliente Test',
          deliveryMode: 'PICKUP',
          deliveryTimeLabel: '12:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50.0,
          items: [],
          createdAt: '2026-10-31T12:00:00Z',
        },
      ]);

      let mutatedEvent: any = null;
      service.orderMutated$.subscribe((e) => (mutatedEvent = e));

      (stompService as any).orderStatusChangedSubject.next({
        eventId: 'evt-status-change',
        eventType: 'ORDER_STATUS_CHANGED',
        orderId: 'ord-status-1',
        orderNumber: '#PED-STATUS',
        customerId: 'cust-1',
        storeId: 'store-123',
        previousStatus: 'CONFIRMED',
        newStatus: 'IN_PREPARATION',
        occurredAt: '2026-10-31T12:10:00Z',
      });

      expect(toastMock.info).toHaveBeenCalledWith('Pedido #PED-STATUS cambió a En Preparación');
      expect(service.orders()[0].status).toBe('IN_PREPARATION');
      expect(mutatedEvent).toEqual({
        storeId: 'store-123',
        orderId: 'ord-status-1',
        status: 'IN_PREPARATION',
      });

      const req = httpMock.expectOne((r) => r.url.includes('/orders'));
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });

    it('debe disparar reconciliación REST cuando StompService emite evento reconnected$', () => {
      const stompService = TestBed.inject(StompService);

      // Simular reconexión tras caída de socket
      (stompService as any).reconnectedSubject.next();

      const req = httpMock.expectOne((r) => r.url.includes('/orders'));
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });
  });

  describe('getOrderStatusCounts y gestión reactiva de contadores', () => {
    const mockCountsResponse: MerchantOrderStatusCounts = {
      all: 48,
      confirmed: 12,
      inPreparation: 6,
      ready: 4,
      outForDelivery: 2,
      completed: 20,
      cancelled: 4,
    };

    it('debe consultar contadores históricos sin parámetro date con la URL y método correctos', () => {
      service.getOrderStatusCounts('store-123').subscribe((counts) => {
        expect(counts).toEqual(mockCountsResponse);
        expect(counts.all).toBe(48);
        expect(counts.confirmed).toBe(12);
        expect(counts.cancelled).toBe(4);
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('date')).toBe(false);

      req.flush(mockCountsResponse);
    });

    it('debe consultar contadores diarios con parámetro date en formato YYYY-MM-DD', () => {
      service.getOrderStatusCounts('store-123', '2026-11-14').subscribe((counts) => {
        expect(counts).toEqual(mockCountsResponse);
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=2026-11-14`,
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('date')).toBe('2026-11-14');

      req.flush(mockCountsResponse);
    });

    it('debe propagar error si storeId no puede ser determinado en getOrderStatusCounts', () => {
      storeService.currentStore.set(null);

      service.getOrderStatusCounts().subscribe({
        next: () => {
          throw new Error('No debió resolverse sin storeId');
        },
        error: (err) => {
          expect(err.message).toContain('No se pudo determinar la tienda activa');
        },
      });

      httpMock.expectNone(`${environment.apiUrl}/merchants/stores/null/orders/counts`);
    });

    it('loadOrderCounts debe actualizar señales reactivas de contadores históricos y tabCounts', () => {
      service.loadOrderCounts('store-123').subscribe();

      expect(service.isCountsLoading()).toBe(true);
      expect(service.tabCounts().all).toBe('...');

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );
      req.flush(mockCountsResponse);

      expect(service.isCountsLoading()).toBe(false);
      expect(service.orderCounts()).toEqual(mockCountsResponse);
      expect(service.totalOrdersCount()).toBe(48);
      expect(service.confirmedCount()).toBe(12);
      expect(service.inPrepCount()).toBe(6);
      expect(service.readyCount()).toBe(4);
      expect(service.completedCount()).toBe(20);
      expect(service.remainingDeliveriesCount()).toBe(10); // 6 en preparación + 4 listos
      expect(service.tabCounts().all).toBe(48);
      expect(service.tabCounts().confirmed).toBe(12);
      expect(service.tabCounts().inPreparation).toBe(6);
      expect(service.tabCounts().ready).toBe(4);
      expect(service.tabCounts().completed).toBe(20);
    });

    it('loadTodayOrderCounts debe consultar con la fecha operativa de Lima y actualizar activeOrdersTodayCount', () => {
      service.loadTodayOrderCounts('store-123').subscribe();

      expect(service.isTodayCountsLoading()).toBe(true);
      const todayLima = getOperatingDateLima();

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockCountsResponse);

      expect(service.isTodayCountsLoading()).toBe(false);
      expect(service.todayOrderCounts()).toEqual(mockCountsResponse);
      // Pedidos activos del sidebar: confirmed (12) + inPreparation (6) + ready (4) = 22
      // Excluye outForDelivery (2), completed (20), cancelled (4)
      expect(service.activeOrdersTodayCount()).toBe(22);
    });

    it('debe manejar error HTTP en contadores sin enmascarar en ceros ni destruir listado', () => {
      service.loadOrderCounts('store-123').subscribe();

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );
      req.flush({ message: 'Error interno' }, { status: 500, statusText: 'Server Error' });

      expect(service.isCountsLoading()).toBe(false);
      expect(service.orderCounts()).toBeNull();
      expect(service.countsError()).toBe('Error al consultar contadores de pedidos');
      expect(service.tabCounts().all).toBe('—');
    });

    it('debe descartar respuestas fuera de orden (race condition) si llega una respuesta más reciente', () => {
      service.loadOrderCounts('store-123').subscribe();
      const req1 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );

      service.loadOrderCounts('store-123', true).subscribe();
      const req2 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );

      // req1 responde después con datos viejos
      req1.flush({ ...mockCountsResponse, all: 10 });
      expect(service.orderCounts()).toBeNull();

      // req2 responde con datos vigentes
      req2.flush({ ...mockCountsResponse, all: 50 });
      expect(service.orderCounts()?.all).toBe(50);
    });

    it('debe deduplicar peticiones concurrentes en vuelo para la misma tienda y fecha', () => {
      let emission1: MerchantOrderStatusCounts | null = null;
      let emission2: MerchantOrderStatusCounts | null = null;

      // Dos invocaciones concurrentes (ej. Dashboard y Sidebar simultáneos)
      service.loadTodayOrderCounts('store-123').subscribe((counts) => (emission1 = counts));
      service.loadTodayOrderCounts('store-123').subscribe((counts) => (emission2 = counts));

      const todayLima = service.currentOperatingDate();
      // Solo debe existir UNA petición HTTP en curso
      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );
      expect(req.request.method).toBe('GET');

      req.flush(mockCountsResponse);

      // Ambas suscripciones reciben el mismo resultado deduplicado
      expect(emission1).toEqual(mockCountsResponse);
      expect(emission2).toEqual(mockCountsResponse);
    });

    it('debe permitir forzar un refresco (force=true) tras mutación sin usar caché en vuelo', () => {
      service.loadTodayOrderCounts('store-123').subscribe();
      const todayLima = service.currentOperatingDate();
      const req1 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );

      // Refresco forzado explícito (ej. después de mutación)
      service.loadTodayOrderCounts('store-123', true).subscribe();
      const req2 = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );

      req1.flush(mockCountsResponse);
      req2.flush({ ...mockCountsResponse, confirmed: 15 });

      expect(service.todayOrderCounts()?.confirmed).toBe(15);
    });

    it('debe preservar datos previos y activar isTodayOutdated si falla una actualización diaria', () => {
      // Estado previo válido
      service.todayOrderCounts.set(mockCountsResponse);
      service.todayOperatingDate.set(service.currentOperatingDate());
      service.todayCountsError.set(null);
      expect(service.isTodayOutdated()).toBe(false);

      // Intento de refresco que falla
      service.loadTodayOrderCounts('store-123', true).subscribe();
      const todayLima = service.currentOperatingDate();
      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );
      req.flush({ message: 'Error temporal' }, { status: 500, statusText: 'Server Error' });

      // Los datos previos no son destruidos pero quedan marcados como desactualizados
      expect(service.todayOrderCounts()).toEqual(mockCountsResponse);
      expect(service.todayCountsError()).toBe('Error al consultar contadores del día');
      expect(service.isTodayOutdated()).toBe(true);
    });

    it('debe detectar cruce de medianoche en Lima, invalidar contadores previos y refrescar', () => {
      service.todayOrderCounts.set(mockCountsResponse);
      service.todayOperatingDate.set('2026-11-14');
      service.currentOperatingDate.set('2026-11-14');

      // Simular transición a nuevo día operativo
      (service as any).handleOperatingDateTransition('2026-11-15');

      expect(service.currentOperatingDate()).toBe('2026-11-15');
      // Contadores del día anterior son invalidados
      expect(service.todayOrderCounts()).toBeNull();
      expect(service.todayOperatingDate()).toBeNull();

      // Refresco automático solicitado para la nueva fecha
      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=2026-11-15`,
      );
      req.flush({ ...mockCountsResponse, all: 5, confirmed: 2, ready: 0, inPreparation: 1 });

      expect(service.todayOrderCounts()?.all).toBe(5);
      expect(service.todayOperatingDate()).toBe('2026-11-15');
    });

    it('debe limpiar el temporizador de medianoche al llamar a ngOnDestroy', () => {
      service.ngOnDestroy();
      expect((service as any).midnightTimerId).toBeNull();
    });

    it('debe descartar respuesta de contadores si la tienda activa cambió mientras estaba en vuelo', () => {
      service.loadOrderCounts('store-123').subscribe();
      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );

      // Simular cambio de tienda en StoreService
      storeService.currentStore.set({
        ...storeService.currentStore()!,
        id: 'store-otra',
      });

      req.flush(mockCountsResponse);
      expect(service.orderCounts()).toBeNull();
    });

    it('debe refrescar contadores tras mutación exitosa en updateOrderStatus cuando los contadores están activos', () => {
      service.orderCounts.set(mockCountsResponse);
      service.todayOrderCounts.set(mockCountsResponse);

      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        status: 'IN_PREPARATION',
      });

      const countsReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts`,
      );
      const todayLima = getOperatingDateLima();
      const todayCountsReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/counts?date=${todayLima}`,
      );

      countsReq.flush({
        ...mockCountsResponse,
        confirmed: 11,
        inPreparation: 7,
      });
      todayCountsReq.flush({
        ...mockCountsResponse,
        confirmed: 11,
        inPreparation: 7,
      });

      expect(service.confirmedCount()).toBe(11);
      expect(service.inPrepCount()).toBe(7);
    });

    it('debe limpiar los contadores al llamar a clearOrders', () => {
      service.orderCounts.set(mockCountsResponse);
      service.todayOrderCounts.set(mockCountsResponse);

      service.clearOrders();

      expect(service.orderCounts()).toBeNull();
      expect(service.todayOrderCounts()).toBeNull();
      expect(service.countsError()).toBeNull();
      expect(service.todayCountsError()).toBeNull();
    });
  });

  describe('Búsqueda Global Remota y Pipeline de Filtros', () => {
    it('debe aplicar debounce de 300ms e invalidar peticiones en vuelo previas inmediatamente al cambiar texto', () => {
      vi.useFakeTimers();

      // Petición inicial en vuelo
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
      const initialReq = httpMock.expectOne((r) => r.url.includes('/orders'));

      // Usuario ingresa texto
      service.setSearchQuery('1042');
      expect(service.searchQuery()).toBe('1042');
      expect(service.appliedSearchQuery()).toBe('');
      expect(service.isLoading()).toBe(true);

      // Si la petición inicial responde durante el debounce, debe ser descartada por requestId viejo
      initialReq.flush({
        ...mockPageResponse,
        content: [{ ...mockPageResponse.content[0], orderNumber: '#PED-OLD' }],
      });
      expect(service.orders().length).toBe(0);

      // Avanzar el reloj 299ms: aún no debe haberse enviado la petición
      vi.advanceTimersByTime(299);
      httpMock.expectNone((r) => r.params.has('search'));

      // Completar los 300ms
      vi.advanceTimersByTime(1);
      expect(service.appliedSearchQuery()).toBe('1042');
      expect(service.currentPage()).toBe(0);

      const searchReq = httpMock.expectOne(
        (r) =>
          r.url.includes('/orders') &&
          r.params.get('search') === '1042' &&
          r.params.get('page') === '0',
      );
      searchReq.flush({
        ...mockPageResponse,
        totalElements: 1,
        content: [{ ...mockPageResponse.content[0], orderNumber: '#1042' }],
      });

      expect(service.orders().length).toBe(1);
      expect(service.orders()[0].orderNumber).toBe('#1042');
      expect(service.isLoading()).toBe(false);
    });

    it('debe limpiar la búsqueda de inmediato y recargar página 0 sin search con clearSearch', () => {
      vi.useFakeTimers();
      service.appliedSearchQuery.set('1042');
      service.searchQuery.set('1042');

      service.clearSearch();

      expect(service.searchQuery()).toBe('');
      expect(service.appliedSearchQuery()).toBe('');
      expect(service.currentPage()).toBe(0);

      const req = httpMock.expectOne(
        (r) => r.url.includes('/orders') && !r.params.has('search') && r.params.get('page') === '0',
      );
      req.flush(mockPageResponse);
    });

    it('debe conservar appliedSearchQuery al cambiar de página con setPage', () => {
      service.appliedSearchQuery.set('Valeria');
      service.setPage(2);

      const req = httpMock.expectOne(
        (r) =>
          r.url.includes('/orders') &&
          r.params.get('page') === '2' &&
          r.params.get('search') === 'Valeria',
      );
      req.flush({ ...mockPageResponse, page: 2 });
      expect(service.currentPage()).toBe(2);
    });

    it('debe conservar appliedSearchQuery y reiniciar página a 0 al cambiar de pestaña con setActiveTab', () => {
      service.appliedSearchQuery.set('984552109');
      service.currentPage.set(3);

      service.setActiveTab('IN_PREPARATION');

      expect(service.currentPage()).toBe(0);
      expect(service.activeTab()).toBe('IN_PREPARATION');

      const req = httpMock.expectOne(
        (r) =>
          r.url.includes('/orders') &&
          r.params.get('page') === '0' &&
          r.params.get('status') === 'IN_PREPARATION' &&
          r.params.get('search') === '984552109',
      );
      req.flush(mockPageResponse);
    });

    it('debe descartar la respuesta si la tienda activa cambió mientras la petición estaba en vuelo', () => {
      service.loadOrders(0, 10, 'ALL', 'store-123', 'Burger').subscribe();

      const req = httpMock.expectOne((r) => r.url.includes('/store-123/orders'));

      // Cambiar tienda activa en StoreService
      storeService.currentStore.set({
        ...storeService.currentStore()!,
        id: 'store-otra',
      });

      req.flush(mockPageResponse);

      expect(service.orders().length).toBe(0);
      expect(service.isLoaded()).toBe(false);
    });

    it('no debe recargar el listado de pedidos por evento STOMP si hay un debounce de búsqueda activo', () => {
      vi.useFakeTimers();
      const stompService = TestBed.inject(StompService);

      // Iniciar escritura (debounce activo de 300ms)
      service.setSearchQuery('Nuevo');

      // Llega evento STOMP mientras el debounce está activo
      (stompService as any).orderConfirmedSubject.next({
        eventId: 'evt-101',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-stomp',
        orderNumber: '#PED-STOMP',
        storeId: 'store-123',
        totalAmount: 50.0,
        currency: 'PEN',
        occurredAt: '2026-11-14T12:00:00Z',
      });

      expect(toastMock.info).toHaveBeenCalledWith('Nuevo pedido recibido: #PED-STOMP');
      // No debe haberse disparado solicitud HTTP para la lista aún
      httpMock.expectNone((r) => r.url.includes('/orders') && !r.url.includes('/counts'));

      // Al cumplirse el debounce, se dispara la búsqueda del usuario
      vi.advanceTimersByTime(300);
      const searchReq = httpMock.expectOne(
        (r) => r.url.includes('/orders') && r.params.get('search') === 'Nuevo',
      );
      searchReq.flush(mockPageResponse);
    });

    it('debe recargar el listado incluyendo search en evento STOMP cuando no hay debounce activo', () => {
      const stompService = TestBed.inject(StompService);
      service.appliedSearchQuery.set('Activo');

      (stompService as any).orderConfirmedSubject.next({
        eventId: 'evt-102',
        eventType: 'ORDER_CONFIRMED',
        orderId: 'ord-stomp-2',
        orderNumber: '#PED-STOMP-2',
        storeId: 'store-123',
        totalAmount: 60.0,
        currency: 'PEN',
        occurredAt: '2026-11-14T12:00:00Z',
      });

      const listReq = httpMock.expectOne(
        (r) => r.url.includes('/orders') && r.params.get('search') === 'Activo',
      );
      listReq.flush(mockPageResponse);
    });

    it('debe evaluar correctamente los estados vacíos con búsqueda remota', () => {
      // 1. Estado vacío global de tienda (sin pedidos en absoluto)
      service.totalElements.set(0);
      service.isLoaded.set(true);
      service.isLoading.set(false);
      service.activeTab.set('ALL');
      service.appliedSearchQuery.set('');

      expect(service.isEmptyOrders()).toBe(true);
      expect(service.isFilterEmpty()).toBe(false);
      expect(service.isSearchEmpty()).toBe(false);

      // 2. Estado vacío de filtro por pestaña
      service.activeTab.set('CONFIRMED');
      expect(service.isEmptyOrders()).toBe(false);
      expect(service.isFilterEmpty()).toBe(true);
      expect(service.isSearchEmpty()).toBe(false);

      // 3. Estado vacío de búsqueda (prioritario sobre estado y tienda)
      service.appliedSearchQuery.set('TérminoSinCoincidencias');
      expect(service.isEmptyOrders()).toBe(false);
      expect(service.isFilterEmpty()).toBe(false);
      expect(service.isSearchEmpty()).toBe(true);
    });

    it('los contadores agregados reales no deben verse afectados por la búsqueda remota', () => {
      service.orderCounts.set({
        all: 100,
        confirmed: 20,
        inPreparation: 10,
        ready: 5,
        outForDelivery: 5,
        completed: 55,
        cancelled: 5,
      });

      service.loadOrders(0, 10, 'ALL', 'store-123', 'TerminoFiltro').subscribe();
      const req = httpMock.expectOne((r) => r.params.get('search') === 'TerminoFiltro');
      req.flush({
        ...mockPageResponse,
        totalElements: 2, // Solo 2 coinciden con la búsqueda
      });

      // El totalElements de la búsqueda es 2, pero los contadores de la tienda se mantienen en 100
      expect(service.totalElements()).toBe(2);
      expect(service.tabCounts().all).toBe(100);
      expect(service.totalOrdersCount()).toBe(100);
      expect(service.confirmedCount()).toBe(20);
    });

    it('debe reconciliar la consulta y finalizar isLoading si el usuario escribe y vuelve al término aplicado antes de 300ms (P1 Regresión)', () => {
      vi.useFakeTimers();

      // 1. Búsqueda aplicada inicial con solicitud en vuelo
      service.appliedSearchQuery.set('1042');
      service.searchQuery.set('1042');
      service.loadOrders(0, 10, 'ALL', 'store-123', '1042').subscribe();
      const req1 = httpMock.expectOne((r) => r.params.get('search') === '1042');
      expect(service.isLoading()).toBe(true);

      // 2. Usuario escribe otro término antes de que responda req1
      service.setSearchQuery('10425');
      expect(service.searchQuery()).toBe('10425');
      expect(service.isLoading()).toBe(true);

      // Si req1 responde ahora, fue invalidada por activeListRequestId++
      req1.flush({
        ...mockPageResponse,
        content: [{ ...mockPageResponse.content[0], orderNumber: '#1042' }],
      });
      expect(service.orders().length).toBe(0);

      // 3. Usuario borra '5' y vuelve a '1042' antes de 300ms
      service.setSearchQuery('1042');
      expect(service.searchQuery()).toBe('1042');
      expect(service.isLoading()).toBe(true);

      // Avanzar el debounce
      vi.advanceTimersByTime(300);

      // Debe existir una nueva solicitud vigente para '1042'
      const req2 = httpMock.expectOne((r) => r.params.get('search') === '1042');
      req2.flush({
        ...mockPageResponse,
        totalElements: 1,
        content: [{ ...mockPageResponse.content[0], orderNumber: '#1042' }],
      });

      // El estado de carga debe finalizar correctamente
      expect(service.isLoading()).toBe(false);
      expect(service.orders().length).toBe(1);
      expect(service.orders()[0].orderNumber).toBe('#1042');
    });

    it('debe reconciliar la búsqueda remota y no finalizar isLoading prematuramente cuando un PATCH completa durante una búsqueda HTTP en vuelo (P1 Regresión)', () => {
      // 1. Mantener en memoria pedidos y metadatos de una página anterior
      service.orders.set([
        {
          id: 'ord-1039',
          orderNumber: '#PED-1039',
          customerName: 'Valeria Ramos',
          deliveryMode: 'DELIVERY',
          deliveryTimeLabel: '12:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50,
          items: [],
          createdAt: new Date().toISOString(),
        },
      ]);
      service.totalElements.set(25);
      service.totalPages.set(3);
      service.currentPage.set(0);

      // 2. Iniciar búsqueda HTTP para 'Carlos'
      service.appliedSearchQuery.set('Carlos');
      service.loadOrders(0, 10, 'ALL', 'store-123', 'Carlos').subscribe();
      const searchReq1 = httpMock.expectOne((r) => r.params.get('search') === 'Carlos');
      expect(service.isLoading()).toBe(true);

      // 3. Ejecutar PATCH en paralelo para ord-1039
      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`,
      );

      // 4. Completa el PATCH mientras searchReq1 está pendiente
      patchReq.flush({
        ...mockDetailResponse,
        id: 'ord-1039',
        status: 'IN_PREPARATION',
      });

      // La mutación actualiza ord-1039 en memoria, detecta búsqueda activa en vuelo,
      // NO finaliza isLoading prematuramente y despacha reconciliación remota válida
      expect(service.isLoading()).toBe(true);
      const searchReq2 = httpMock.expectOne((r) => r.params.get('search') === 'Carlos');

      // 5. Simular respuestas HTTP fuera de orden: searchReq1 responde con retraso
      searchReq1.flush({
        ...mockPageResponse,
        totalElements: 99,
        totalPages: 10,
        content: [{ ...mockPageResponse.content[0], id: 'ord-stale-search', customerName: 'Viejo Carlos' }],
      });
      // searchReq1 fue invalidada: no debe reemplazar pedidos ni metadatos, e isLoading sigue en true
      expect(service.orders().find((o) => o.id === 'ord-stale-search')).toBeUndefined();
      expect(service.isLoading()).toBe(true);

      // 6. searchReq2 (reconciliación fresca) responde con los resultados vigentes
      searchReq2.flush({
        ...mockPageResponse,
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-carlos-1',
            customerName: 'Carlos Gomez',
            status: 'IN_PREPARATION',
          },
        ],
      });

      // 7. Confirma que isLoading finaliza solo tras la consulta válida y metadatos son coherentes
      expect(service.isLoading()).toBe(false);
      expect(service.orders().length).toBe(1);
      expect(service.orders()[0].id).toBe('ord-carlos-1');
      expect(service.orders()[0].customerName).toBe('Carlos Gomez');
      expect(service.totalElements()).toBe(1);
      expect(service.totalPages()).toBe(1);
      expect(service.currentPage()).toBe(0);
    });

    it('debe reconciliar el listado remoto con search omitido y no finalizar isLoading prematuramente cuando un PATCH completa durante una carga HTTP sin búsqueda activa (P1 Regresión)', () => {
      // 1. Pestaña ALL y búsqueda vacía
      service.activeTab.set('ALL');
      service.appliedSearchQuery.set('');
      service.searchQuery.set('');

      // 2. Página con datos previamente cargados (ej. página 1 con 50 elementos)
      service.orders.set([
        {
          id: 'ord-prev',
          orderNumber: '#PED-PREV',
          customerName: 'Cliente Previo',
          deliveryMode: 'DELIVERY',
          deliveryTimeLabel: '12:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50,
          items: [],
          createdAt: new Date().toISOString(),
        },
      ]);
      service.totalElements.set(50);
      service.totalPages.set(5);
      service.currentPage.set(1);

      // 3. Nueva petición HTTP de listado en vuelo para página 1
      service.loadOrders(1, 10, 'ALL', 'store-123').subscribe();
      const listReq = httpMock.expectOne((r) => {
        return (
          r.url.endsWith('/orders') &&
          r.params.get('page') === '1' &&
          r.params.get('size') === '10' &&
          !r.params.has('search') &&
          !r.params.has('status')
        );
      });
      expect(service.isLoading()).toBe(true);

      // 4. PATCH de mutación completado antes de la respuesta
      service.updateOrderStatus('ord-prev', 'READY', 'store-123').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-prev/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        id: 'ord-prev',
        status: 'READY',
      });

      // La mutación actualiza en memoria, pero isLoading NO finaliza prematuramente:
      // se emite la reconciliación HTTP con search omitido
      expect(service.isLoading()).toBe(true);
      const reconReq = httpMock.expectOne((r) => {
        return (
          r.url.endsWith('/orders') &&
          r.params.get('page') === '1' &&
          r.params.get('size') === '10' &&
          !r.params.has('search') &&
          !r.params.has('status')
        );
      });

      // 5. Respuesta antigua retrasada responde y es descartada
      listReq.flush({
        ...mockPageResponse,
        page: 1,
        totalElements: 999,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-stale-page',
            status: 'CONFIRMED',
          },
        ],
      });
      expect(service.orders().find((o) => o.id === 'ord-stale-page')).toBeUndefined();
      expect(service.isLoading()).toBe(true);

      // 6. Reconciliación HTTP responde con datos válidos
      reconReq.flush({
        ...mockPageResponse,
        page: 1,
        size: 10,
        totalElements: 50,
        totalPages: 5,
        content: [
          {
            ...mockPageResponse.content[0],
            id: 'ord-prev',
            status: 'READY',
          },
        ],
      });

      // 7. Verificación final de orders, totalElements, totalPages, currentPage e isLoading
      expect(service.isLoading()).toBe(false);
      expect(service.orders().length).toBe(1);
      expect(service.orders()[0].id).toBe('ord-prev');
      expect(service.orders()[0].status).toBe('READY');
      expect(service.totalElements()).toBe(50);
      expect(service.totalPages()).toBe(5);
      expect(service.currentPage()).toBe(1);
    });

    it('debe impedir que un PATCH perteneciente a una tienda anterior modifique el listado de la tienda activa', () => {
      // 1. Tienda activa inicial es store-123 con un pedido en memoria
      service.orders.set([
        {
          id: 'ord-current-store',
          orderNumber: '#PED-100',
          customerName: 'Cliente Tienda Actual',
          deliveryMode: 'DELIVERY',
          deliveryTimeLabel: '12:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 50,
          items: [],
          createdAt: new Date().toISOString(),
        },
      ]);
      expect(service.orders().length).toBe(1);

      // 2. Iniciar un PATCH explícito para una tienda anterior 'store-old'
      service.updateOrderStatus('ord-old-store', 'READY', 'store-old').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-old/orders/ord-old-store/status`,
      );

      // 3. El PATCH responde pero la tienda activa sigue siendo store-123
      patchReq.flush({
        ...mockDetailResponse,
        id: 'ord-old-store',
        storeId: 'store-old',
        status: 'READY',
      });

      // 4. El listado de la tienda activa store-123 no debe modificarse
      expect(service.orders().length).toBe(1);
      expect(service.orders()[0].id).toBe('ord-current-store');
      expect(service.orders().find((o) => o.id === 'ord-old-store')).toBeUndefined();
      // No debe haberse disparado ninguna recarga HTTP hacia store-old ni store-123
      httpMock.expectNone((r) => r.url.includes('/orders'));
    });

    it('no debe emitir solicitudes con búsqueda desactualizada cuando un PATCH completa durante un debounce activo (P1 Regresión)', () => {
      vi.useFakeTimers();

      // 1. Usuario empieza a escribir 'Valeria' (debounce pendiente de 300ms)
      service.setSearchQuery('Valeria');
      expect(service.isLoading()).toBe(true);

      // 2. PATCH de mutación se completa durante el debounce
      service.orders.set([
        {
          id: 'ord-200',
          orderNumber: '#PED-200',
          customerName: 'Cliente Prueba',
          deliveryMode: 'PICKUP',
          deliveryTimeLabel: '14:00',
          isUrgent: false,
          status: 'CONFIRMED',
          totalAmount: 40,
          items: [],
          createdAt: new Date().toISOString(),
        },
      ]);
      service.updateOrderStatus('ord-200', 'READY', 'store-123').subscribe();
      const patchReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders/ord-200/status`,
      );
      patchReq.flush({
        ...mockDetailResponse,
        id: 'ord-200',
        status: 'READY',
      });

      // No debe haberse emitido ninguna solicitud de listado con el término anterior (vacío)
      httpMock.expectNone((r) => r.url.includes('/orders') && !r.url.includes('/counts'));

      // 3. Al completarse los 300ms de debounce, se dispara la búsqueda con 'Valeria'
      vi.advanceTimersByTime(300);
      const searchReq = httpMock.expectOne((r) => r.params.get('search') === 'Valeria');
      searchReq.flush(mockPageResponse);

      expect(service.isLoading()).toBe(false);
      expect(service.appliedSearchQuery()).toBe('Valeria');
    });

    it('debe descartar la respuesta si la tienda pasa a null mientras la petición estaba en vuelo (P2 Regresión)', () => {
      service.loadOrders(0, 10, 'ALL', 'store-123', '984552109').subscribe();
      const req = httpMock.expectOne((r) => r.url.includes('/store-123/orders'));

      // Simular logout o reseteo de tienda a null
      storeService.clearStore();
      service.clearOrders();

      // La petición responde después
      req.flush(mockPageResponse);

      expect(service.orders().length).toBe(0);
      expect(service.totalElements()).toBe(0);
      expect(service.isLoaded()).toBe(false);
      expect(service.isLoading()).toBe(false);
    });

    it('debe mantener la coherencia de filteredOrders, orders y totalElements al cambiar de pestaña con búsqueda activa (P2 Regresión)', () => {
      service.appliedSearchQuery.set('Carlos');
      service.activeTab.set('ALL');

      // Cambiar a pestaña READY
      service.setActiveTab('READY');
      expect(service.activeTab()).toBe('READY');
      expect(service.currentPage()).toBe(0);

      const req = httpMock.expectOne(
        (r) =>
          r.params.get('status') === 'READY' &&
          r.params.get('search') === 'Carlos' &&
          r.params.get('page') === '0',
      );

      req.flush({
        ...mockPageResponse,
        totalElements: 1,
        content: [
          {
            ...mockPageResponse.content[0],
            status: 'READY',
            customerName: 'Carlos Gomez',
          },
        ],
      });

      expect(service.totalElements()).toBe(1);
      expect(service.orders().length).toBe(1);
      expect(service.filteredOrders().length).toBe(1);
      expect(service.filteredOrders()[0].status).toBe('READY');
      expect(service.isFilterEmpty()).toBe(false);
      expect(service.isSearchEmpty()).toBe(false);
    });
  });

  describe('createMerchantOrder (POST)', () => {
    const mockCreateRequest: CreateMerchantOrderRequest = {
      deliveryMode: 'DELIVERY',
      customerName: 'Lucía Méndez',
      customerPhone: '987654321',
      paymentMethod: 'YAPE',
      serviceDate: '2026-11-20',
      serviceStartTime: '15:00:00',
      serviceEndTime: '15:30:00',
      items: [{ variantId: 'v-p1', quantity: 2, unitPriceAmount: 20 }],
    };

    const mockDetailResponse: MerchantOrderDetailResponse = {
      id: 'ord-created-1',
      orderNumber: '#ORD-00000001',
      storeId: 'store-123',
      customerUserId: 'usr-1',
      paymentId: '',
      capacityReservationId: '',
      status: 'CONFIRMED',
      deliveryMode: 'DELIVERY',
      serviceDate: '2026-11-20',
      serviceStartTime: '15:00:00',
      serviceEndTime: '15:30:00',
      customerName: 'Lucía Méndez',
      customerPhone: '987654321',
      customerEmail: null,
      deliveryAddress: null,
      deliveryReference: null,
      deliveryLatitude: null,
      deliveryLongitude: null,
      currency: 'PEN',
      productsSubtotalAmount: 40,
      discountAmount: 0,
      discountCode: null,
      deliveryFeeAmount: 0,
      includedTaxAmount: 0,
      totalAmount: 40,
      confirmedAt: '2026-11-20T15:00:00Z',
      updatedAt: '2026-11-20T15:00:00Z',
      items: [],
      statusHistory: [],
    };

    it('debe enviar POST a /merchants/stores/{storeId}/orders y refrescar listado y contadores al responder 201', () => {
      service.openCreateModal();
      expect(service.isCreateModalOpen()).toBe(true);

      const busEvents: any[] = [];
      invalidationBus.orderCreated$.subscribe((e) => busEvents.push(e));

      let createdResponse: MerchantOrderDetailResponse | null = null;
      service.createMerchantOrder('store-123', mockCreateRequest).subscribe((res) => {
        createdResponse = res;
      });

      const postReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders`,
      );
      expect(postReq.request.method).toBe('POST');
      expect(postReq.request.body).toEqual(mockCreateRequest);

      postReq.flush(mockDetailResponse);

      expect(createdResponse).toEqual(mockDetailResponse);
      expect(service.isCreateModalOpen()).toBe(false);
      expect(toastMock.success).toHaveBeenCalledWith(
        'Comanda #ORD-00000001 creada con éxito',
      );
      expect(busEvents.length).toBe(1);
      expect(busEvents[0]).toEqual({
        storeId: 'store-123',
        orderId: 'ord-created-1',
        orderNumber: '#ORD-00000001',
        origin: undefined,
        source: 'LOCAL_MERCHANT',
      });

      // Debe disparar refresco de órdenes y contadores
      const listReq = httpMock.expectOne((r) => r.url.endsWith('/orders') && r.method === 'GET');
      listReq.flush(mockPageResponse);

      const countsReq = httpMock.expectOne(
        (r) => r.url.endsWith('/orders/counts') && !r.params.has('date'),
      );
      countsReq.flush({ all: 1, confirmed: 1, inPreparation: 0, ready: 0, outForDelivery: 0, completed: 0, cancelled: 0 });

      const todayCountsReq = httpMock.expectOne(
        (r) => r.url.endsWith('/orders/counts') && r.params.has('date'),
      );
      todayCountsReq.flush({ all: 1, confirmed: 1, inPreparation: 0, ready: 0, outForDelivery: 0, completed: 0, cancelled: 0 });
    });

    it('debe manejar error al registrar comanda y mostrar toast', () => {
      let errorOccurred = false;
      service.createMerchantOrder('store-123', mockCreateRequest).subscribe({
        error: () => {
          errorOccurred = true;
        },
      });

      const postReq = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/store-123/orders`,
      );
      postReq.flush(
        { message: 'Stock insuficiente para la variante' },
        { status: 409, statusText: 'Conflict' },
      );

      expect(errorOccurred).toBe(true);
      expect(toastMock.error).toHaveBeenCalledWith('Stock insuficiente para la variante');
    });
  });

  describe('Estrategia Cache-First y Optimización de Consultas', () => {
    it('debe aplicar Cache-First en loadOrders evitando peticiones HTTP repetidas', () => {
      // 1. Primera consulta: debe realizar petición HTTP
      let firstResponse: any = null;
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe((res) => {
        firstResponse = res;
      });

      const req1 = httpMock.expectOne((r) => r.url.endsWith('/orders'));
      req1.flush(mockPageResponse);

      expect(firstResponse).toBeTruthy();
      expect(service.orders().length).toBe(1);
      expect(service.isLoaded()).toBe(true);

      // 2. Segunda consulta con mismos parámetros y forceRefresh = false (por defecto): debe devolver datos en memoria sin HTTP
      let secondResponse: any = null;
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe((res) => {
        secondResponse = res;
      });

      // No debe haber ninguna petición HTTP en vuelo
      httpMock.expectNone((r) => r.url.endsWith('/orders'));
      expect(secondResponse).toBeTruthy();
      expect(secondResponse.totalElements).toBe(mockPageResponse.totalElements);

      // 3. Tercera consulta con forceRefresh = true: debe forzar nueva petición HTTP
      let thirdResponse: any = null;
      service.loadOrders(0, 10, 'ALL', 'store-123', undefined, true).subscribe((res) => {
        thirdResponse = res;
      });

      const req3 = httpMock.expectOne((r) => r.url.endsWith('/orders'));
      req3.flush(mockPageResponse);
      expect(thirdResponse).toBeTruthy();
    });

    it('debe aplicar Cache-First en loadOrderCounts evitando peticiones HTTP repetidas', () => {
      // 1. Primera llamada: realiza HTTP
      service.loadOrderCounts('store-123').subscribe();
      const req1 = httpMock.expectOne((r) => r.url.endsWith('/orders/counts') && !r.params.has('date'));
      req1.flush({ all: 5, confirmed: 2, inPreparation: 1, ready: 1, outForDelivery: 0, completed: 1, cancelled: 0 });

      expect(service.orderCounts()?.all).toBe(5);

      // 2. Segunda llamada con force = false: usa caché en memoria
      service.loadOrderCounts('store-123').subscribe();
      httpMock.expectNone((r) => r.url.endsWith('/orders/counts'));

      // 3. Tercera llamada con force = true: fuerza HTTP
      service.loadOrderCounts('store-123', true).subscribe();
      const req3 = httpMock.expectOne((r) => r.url.endsWith('/orders/counts') && !r.params.has('date'));
      req3.flush({ all: 6, confirmed: 3, inPreparation: 1, ready: 1, outForDelivery: 0, completed: 1, cancelled: 0 });
      expect(service.orderCounts()?.all).toBe(6);
    });

    it('debe aplicar Cache-First en loadTodayOrderCounts evitando peticiones HTTP repetidas', () => {
      // 1. Primera llamada: realiza HTTP
      service.loadTodayOrderCounts('store-123').subscribe();
      const req1 = httpMock.expectOne((r) => r.url.endsWith('/orders/counts') && r.params.has('date'));
      req1.flush({ all: 3, confirmed: 1, inPreparation: 1, ready: 1, outForDelivery: 0, completed: 0, cancelled: 0 });

      expect(service.todayOrderCounts()?.all).toBe(3);

      // 2. Segunda llamada con force = false: usa caché en memoria
      service.loadTodayOrderCounts('store-123').subscribe();
      httpMock.expectNone((r) => r.url.endsWith('/orders/counts'));

      // 3. Tercera llamada con force = true: fuerza HTTP
      service.loadTodayOrderCounts('store-123', true).subscribe();
      const req3 = httpMock.expectOne((r) => r.url.endsWith('/orders/counts') && r.params.has('date'));
      req3.flush({ all: 4, confirmed: 2, inPreparation: 1, ready: 1, outForDelivery: 0, completed: 0, cancelled: 0 });
      expect(service.todayOrderCounts()?.all).toBe(4);
    });

    it('debe limpiar la memoria y la caché al llamar clearCache()', () => {
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
      const req = httpMock.expectOne((r) => r.url.endsWith('/orders'));
      req.flush(mockPageResponse);
      expect(service.isLoaded()).toBe(true);

      service.clearCache();

      expect(service.isLoaded()).toBe(false);
      expect(service.orders().length).toBe(0);
      expect(service.orderCounts()).toBeNull();

      // Tras clearCache, una nueva llamada debe requerir HTTP
      service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
      const newReq = httpMock.expectOne((r) => r.url.endsWith('/orders'));
      newReq.flush(mockPageResponse);
    });
  });
});
