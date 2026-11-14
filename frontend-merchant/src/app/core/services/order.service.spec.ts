import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { ToastService } from './toast.service';
import { StoreService } from './store.service';
import { StompService } from './stomp.service';
import {
  MerchantOrderDetailResponse,
  MerchantOrderPageResponse,
  MerchantOrder,
} from '../models/order.models';
import { environment } from '../../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;
  let storeService: StoreService;
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
    httpMock.verify();
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

    const req = httpMock.expectOne((r) =>
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

    const req = httpMock.expectOne((r) =>
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
    const ordersReq = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-resolved/orders`,
    );
    expect(ordersReq.request.method).toBe('GET');
    ordersReq.flush(mockPageResponse);

    expect(service.orders().length).toBe(1);
  });

  it('debe descartar respuestas obsoletas del listado si ocurre otra petición en paralelo (race condition)', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
    service.loadOrders(1, 10, 'READY', 'store-123').subscribe();

    const req1 = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
      r.params.get('page') === '0',
    );
    const req2 = httpMock.expectOne((r) =>
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

    const req = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-123/orders`,
    );
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    expect(service.error()).toBe('No tienes permisos para ver los pedidos de esta tienda');
    expect(toastMock.error).toHaveBeenCalledWith('No tienes permisos para ver los pedidos de esta tienda.');
    expect(service.isLoading()).toBe(false);
  });

  it('debe manejar error 500 del servidor', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();

    const req = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-123/orders`,
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

    const reqA = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/store-123/orders/ord-A`);
    const reqB = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/store-123/orders/ord-B`);

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

    const req = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
      r.params.get('page') === '0' &&
      r.params.get('status') === 'READY',
    );
    req.flush(mockPageResponse);
  });

  it('debe cambiar de página con setPage y ejecutar GET con la página indicada', () => {
    service.setPage(3);

    const req = httpMock.expectOne((r) =>
      r.url === `${environment.apiUrl}/merchants/stores/store-123/orders` &&
      r.params.get('page') === '3',
    );
    req.flush({ ...mockPageResponse, page: 3 });
    expect(service.currentPage()).toBe(3);
  });

  it('debe realizar búsqueda local en la página actual sin modificar totalElements', () => {
    service.loadOrders(0, 10, 'ALL', 'store-123').subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/orders'));
    req.flush({
      ...mockPageResponse,
      totalElements: 50,
      content: [
        mockPageResponse.content[0],
        {
          ...mockPageResponse.content[0],
          id: 'ord-2',
          orderNumber: '#PED-2000',
          customerName: 'Carlos Gomez',
        },
      ],
    });

    expect(service.orders().length).toBe(2);
    expect(service.totalElements()).toBe(50);

    // Búsqueda local por "Carlos"
    service.setSearchQuery('Carlos');
    expect(service.filteredOrders().length).toBe(1);
    expect(service.filteredOrders()[0].customerName).toBe('Carlos Gomez');
    // totalElements no debe ser afectado por la búsqueda local
    expect(service.totalElements()).toBe(50);
    expect(service.isSearchEmpty()).toBe(false);

    // Búsqueda sin coincidencias en esta página
    service.setSearchQuery('Inexistente');
    expect(service.filteredOrders().length).toBe(0);
    expect(service.isSearchEmpty()).toBe(true);
    expect(service.totalElements()).toBe(50);
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

    it('debe enviar PATCH con URL y body { status } y actualizar estado solo tras HTTP 200', () => {
      expect(service.orders()[0].status).toBe('CONFIRMED');

      service.updateOrderStatus('ord-1039', 'IN_PREPARATION', 'store-123').subscribe();

      // No debe haber mutación optimista antes de la respuesta HTTP
      expect(service.orders()[0].status).toBe('CONFIRMED');
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
      expect(toastMock.info).toHaveBeenCalledWith('Estado del pedido sincronizado con el servidor.');
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
      const reloadReq = httpMock.expectOne((r) =>
        r.url.includes('/orders') &&
        r.params.get('status') === 'CONFIRMED',
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
      httpMock.expectNone(`${environment.apiUrl}/merchants/stores/store-123/orders/ord-1039/status`);
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

      // El pedido en vuelo no debe haber sido pisoteado
      expect(service.orders()[0].status).toBe('CONFIRMED');

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

      const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/store-123/orders/ord-123/status`);
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
      expect(toastMock.info).toHaveBeenCalledWith('La creación manual de pedidos estará disponible próximamente.');
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

    it('debe disparar reconciliación REST cuando StompService emite evento reconnected$', () => {
      const stompService = TestBed.inject(StompService);

      // Simular reconexión tras caída de socket
      (stompService as any).reconnectedSubject.next();

      const req = httpMock.expectOne((r) => r.url.includes('/orders'));
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });
  });
});
