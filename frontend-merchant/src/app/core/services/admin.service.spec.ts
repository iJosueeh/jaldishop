import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { ToastService } from './toast.service';
import { InvalidationBusService } from './invalidation-bus.service';
import { environment } from '../../../environments/environment';
import {
  AdminAnalyticsSummary,
  AdminAuditLogPage,
  AdminInvitation,
  AdminStoreDetail,
  AdminStoreSummary,
  AdminUserSummary,
  OrderInvestigationResult,
  OrderReconciliationResult,
} from '../models/admin.models';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;
  let toastMock: any;

  const mockUsers: AdminUserSummary[] = [
    {
      id: 'usr-1',
      email: 'admin@jaldishop.com',
      firstName: 'Admin',
      lastName: 'Platform',
      fullName: 'Admin Platform',
      status: 'ACTIVE',
      roles: ['ADMIN'],
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
    },
    {
      id: 'usr-2',
      email: 'merchant@store.com',
      firstName: 'Valeria',
      lastName: 'Ramos',
      fullName: 'Valeria Ramos',
      phone: '984552109',
      status: 'ACTIVE',
      roles: ['MERCHANT'],
      createdAt: '2026-09-02T10:00:00Z',
      updatedAt: '2026-09-02T10:00:00Z',
      storeId: 'str-1',
      storeName: 'Dulce Capri',
    },
    {
      id: 'usr-3',
      email: 'customer@gmail.com',
      firstName: 'Carlos',
      lastName: 'Gomez',
      fullName: 'Carlos Gomez',
      status: 'SUSPENDED',
      roles: ['CUSTOMER'],
      createdAt: '2026-09-03T10:00:00Z',
      updatedAt: '2026-09-03T10:00:00Z',
    },
  ];

  const mockStores: AdminStoreSummary[] = [
    {
      id: 'str-1',
      merchantUserId: 'usr-2',
      name: 'Dulce Capri',
      slug: 'dulce-capri',
      contactPhone: '984552109',
      status: 'ACTIVE',
      deliveryEnabled: true,
      pickupEnabled: true,
      createdAt: '2026-09-02T10:00:00Z',
      updatedAt: '2026-09-02T10:00:00Z',
      merchant: {
        id: 'usr-2',
        email: 'merchant@store.com',
        fullName: 'Valeria Ramos',
        status: 'ACTIVE',
      },
    },
    {
      id: 'str-2',
      merchantUserId: 'usr-4',
      name: 'Pastelería Fina',
      slug: 'pasteleria-fina',
      status: 'SUSPENDED',
      deliveryEnabled: false,
      pickupEnabled: true,
      createdAt: '2026-09-04T10:00:00Z',
      updatedAt: '2026-09-04T10:00:00Z',
      merchant: {
        id: 'usr-4',
        email: 'fina@store.com',
        fullName: 'Luis Fina',
        status: 'ACTIVE',
      },
    },
  ];

  beforeEach(() => {
    TestBed.resetTestingModule();

    toastMock = {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AdminService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toastMock },
        InvalidationBusService,
      ],
    });

    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente e inicializar métricas en 0', () => {
    expect(service).toBeTruthy();
    expect(service.users().length).toBe(0);
    expect(service.stores().length).toBe(0);
    expect(service.metrics().totalUsers).toBe(0);
    expect(service.metrics().totalStores).toBe(0);
  });

  it('debe cargar usuarios desde la API y computar métricas', () => {
    service.loadUsers().subscribe((data) => {
      expect(data.length).toBe(3);
      expect(service.users().length).toBe(3);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);

    expect(service.metrics().totalUsers).toBe(3);
    expect(service.metrics().totalAdmins).toBe(1);
    expect(service.metrics().totalMerchants).toBe(1);
    expect(service.metrics().totalCustomers).toBe(1);
    expect(service.metrics().activeUsers).toBe(2);
    expect(service.metrics().suspendedUsers).toBe(1);
  });

  it('debe cargar tiendas desde la API y computar métricas', () => {
    service.loadStores().subscribe((data) => {
      expect(data.length).toBe(2);
      expect(service.stores().length).toBe(2);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/stores`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStores);

    expect(service.metrics().totalStores).toBe(2);
    expect(service.metrics().activeStores).toBe(1);
    expect(service.metrics().suspendedStores).toBe(1);
  });

  it('debe retornar datos en caché si ya están cargados y no están stale (Cache-First)', () => {
    service.users.set(mockUsers);
    (service as any).isUsersLoaded.set(true);
    (service as any).lastUsersSync = Date.now();

    service.loadUsers().subscribe((data) => {
      expect(data.length).toBe(3);
    });

    httpMock.expectNone(`${environment.apiUrl}/admin/users`);
  });

  it('debe suspender usuario optimistamente y confirmar con la API', () => {
    service.users.set(mockUsers);
    const updatedUser: AdminUserSummary = { ...mockUsers[0], status: 'SUSPENDED' };

    service.suspendUser('usr-1').subscribe((res) => {
      expect(res.status).toBe('SUSPENDED');
      expect(toastMock.warning).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/usr-1/suspend`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedUser);

    const target = service.users().find((u) => u.id === 'usr-1');
    expect(target?.status).toBe('SUSPENDED');
  });

  it('debe revertir optimistamente (rollback) si la suspensión de usuario falla con 409', () => {
    service.users.set(mockUsers);

    service.suspendUser('usr-1').subscribe({
      error: () => {
        expect(toastMock.error).toHaveBeenCalledWith('No puedes suspender tu propia cuenta de administrador.');
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/usr-1/suspend`);
    req.flush({ message: 'No puedes suspenderte' }, { status: 409, statusText: 'Conflict' });

    // Snapshot debe haberse restaurado a ACTIVE
    const target = service.users().find((u) => u.id === 'usr-1');
    expect(target?.status).toBe('ACTIVE');
  });

  it('debe activar usuario y actualizar la lista', () => {
    service.users.set(mockUsers);
    const updatedUser: AdminUserSummary = { ...mockUsers[2], status: 'ACTIVE' };

    service.activateUser('usr-3').subscribe((res) => {
      expect(res.status).toBe('ACTIVE');
      expect(toastMock.success).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/usr-3/activate`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedUser);

    const target = service.users().find((u) => u.id === 'usr-3');
    expect(target?.status).toBe('ACTIVE');
  });

  it('debe actualizar roles de usuario mediante PATCH', () => {
    service.users.set(mockUsers);
    const updatedUser: AdminUserSummary = {
      ...mockUsers[2],
      roles: ['CUSTOMER', 'ADMIN'],
    };

    service.updateUserRoles('usr-3', ['CUSTOMER', 'ADMIN']).subscribe((res) => {
      expect(res.roles).toEqual(['CUSTOMER', 'ADMIN']);
      expect(toastMock.success).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/usr-3/roles`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ roles: ['CUSTOMER', 'ADMIN'] });
    req.flush(updatedUser);

    const target = service.users().find((u) => u.id === 'usr-3');
    expect(target?.roles).toEqual(['CUSTOMER', 'ADMIN']);
  });

  it('debe suspender y reactivar tienda con rollback en caso de fallo', () => {
    service.stores.set(mockStores);

    service.suspendStore('str-1').subscribe({
      error: () => {
        expect(toastMock.error).toHaveBeenCalledWith('Error al suspender la tienda.');
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/stores/str-1/suspend`);
    req.flush({}, { status: 500, statusText: 'Server Error' });

    // Debe haberse restaurado a ACTIVE
    expect(service.stores().find((s) => s.id === 'str-1')?.status).toBe('ACTIVE');
  });

  it('debe cargar resumen de analíticas globales desde AdminAnalyticsController', () => {
    const mockSummary: AdminAnalyticsSummary = {
      period: { from: '2026-10-01', to: '2026-10-31' },
      commercial: {
        historicalConfirmedOrdersCount: 150,
        historicalConfirmedGmv: 12500.5,
        activeOrdersCount: 8,
        activeCommercialVolume: 620.0,
        completedOrdersCount: 140,
        productsGrossSales: 11900.0,
        totalDiscounts: 200.0,
        totalDeliveryFees: 800.5,
        cancelledOrdersCount: 2,
        cancelledOrdersVolume: 120.0,
        averageTicket: 89.28,
      },
      financial: {
        grossInflowAmount: 12500.5,
        approvedPaymentsCount: 150,
        settledRefundsAmount: 120.0,
        settledRefundsCount: 2,
        netFinancialInflow: 12380.5,
        orderLinkedPaymentsAmount: 12500.5,
        orderLinkedPaymentsCount: 150,
        orphanPaymentsAmount: 0,
        orphanPaymentsCount: 0,
        disclosure: 'Fondos de pasarela',
      },
      storeRankings: [],
    };

    service.loadAnalyticsSummary('2026-10-01', '2026-10-31').subscribe((data) => {
      expect(data.commercial.historicalConfirmedGmv).toBe(12500.5);
      expect(service.analyticsSummary()).toEqual(mockSummary);
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/admin/analytics/summary?from=2026-10-01&to=2026-10-31`
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('debe cargar pistas de auditoría desde AdminAuditLogController', () => {
    const mockAuditPage: AdminAuditLogPage = {
      content: [
        {
          id: 'log-1',
          actorId: 'usr-1',
          actorEmail: 'admin@jaldishop.com',
          action: 'USER_SUSPENDED',
          targetType: 'USER',
          targetId: 'usr-3',
          status: 'SUCCESS',
          ipAddress: '190.237.10.5',
          createdAt: '2026-10-25T14:30:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    service.loadAuditLogs().subscribe((page) => {
      expect(page.content.length).toBe(1);
      expect(service.auditLogs().length).toBe(1);
      expect(service.totalAuditLogs()).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/audit-logs?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAuditPage);
  });

  it('debe gestionar invitaciones de administradores (invitar y revocar)', () => {
    const newInvitation: AdminInvitation = {
      id: 'inv-1',
      email: 'newadmin@jaldishop.com',
      firstName: 'Marcos',
      lastName: 'Vargas',
      status: 'PENDING',
      expiresAt: '2026-11-25T10:00:00Z',
      createdAt: '2026-11-23T10:00:00Z',
    };

    service
      .inviteAdmin({
        email: 'newadmin@jaldishop.com',
        firstName: 'Marcos',
        lastName: 'Vargas',
      })
      .subscribe((inv) => {
        expect(inv.email).toBe('newadmin@jaldishop.com');
        expect(service.invitations().length).toBe(1);
        expect(toastMock.success).toHaveBeenCalled();
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/administrators/invitations`);
    expect(req.request.method).toBe('POST');
    req.flush(newInvitation);

    // Revocar invitación
    service.revokeAdminInvitation('inv-1').subscribe();
    const revokeReq = httpMock.expectOne(
      `${environment.apiUrl}/admin/administrators/invitations/inv-1/revoke`
    );
    expect(revokeReq.request.method).toBe('POST');
    revokeReq.flush(null);

    expect(service.invitations()[0].status).toBe('REVOKED');
  });

  it('debe investigar y conciliar pagos desfasados mediante AdminOrderInvestigationController', () => {
    const investigationResult: OrderInvestigationResult = {
      attemptId: 'att-123',
      providerStatus: 'approved',
      status: 'APPROVED_COMMERCIAL_PENDING',
      detail: 'Pago aprobado en Mercado Pago; orden pendiente',
    };

    service.investigatePaymentAttempt('att-123').subscribe((res) => {
      expect(res.status).toBe('APPROVED_COMMERCIAL_PENDING');
    });

    const invReq = httpMock.expectOne(
      `${environment.apiUrl}/admin/payments/attempts/att-123/investigate-order`
    );
    expect(invReq.request.method).toBe('POST');
    invReq.flush(investigationResult);

    const reconciliationResult: OrderReconciliationResult = {
      attemptId: 'att-123',
      status: 'SUCCESS_CONFIRMED',
      detail: 'Orden confirmada: #ORD-9988',
    };

    service.reconcilePaymentAttempt('att-123').subscribe((res) => {
      expect(res.status).toBe('SUCCESS_CONFIRMED');
      expect(toastMock.success).toHaveBeenCalled();
    });

    const recReq = httpMock.expectOne(
      `${environment.apiUrl}/admin/payments/attempts/att-123/reconcile-order`
    );
    expect(recReq.request.method).toBe('POST');
    recReq.flush(reconciliationResult);

    // La invalidación reactiva dispara re-fetch de stores y analytics
    const storeReq = httpMock.expectOne(`${environment.apiUrl}/admin/stores`);
    storeReq.flush([]);
    const analyticsReq = httpMock.expectOne(`${environment.apiUrl}/admin/analytics/summary`);
    analyticsReq.flush({});
  });

  it('debe cachear detalles de tienda con getStoreDetail()', () => {
    const mockDetail: AdminStoreDetail = {
      ...mockStores[0],
      legalBusinessName: 'Dulce Capri S.A.C.',
      taxIdType: 'RUC_20',
      taxIdNumber: '20601234567',
      taxRegime: 'MYPE',
      canIssueInvoices: true,
    };

    service.getStoreDetail('str-1').subscribe((detail) => {
      expect(detail.legalBusinessName).toBe('Dulce Capri S.A.C.');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/stores/str-1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetail);

    // Segunda lectura debe salir de memoria sin llamada HTTP
    service.getStoreDetail('str-1').subscribe((detail) => {
      expect(detail.taxRegime).toBe('MYPE');
    });
    httpMock.expectNone(`${environment.apiUrl}/admin/stores/str-1`);
  });
});
