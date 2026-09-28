import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { ToastService } from './toast.service';
import { environment } from '../../../environments/environment';
import { AdminStoreSummary, AdminUserSummary } from '../models/admin.models';

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
    toastMock = {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AdminService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toastMock },
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

  it('debe filtrar usuarios por rol, estado y búsqueda de texto', () => {
    service.users.set(mockUsers);

    // Filtro por Rol
    service.setUserRoleFilter('MERCHANT');
    expect(service.filteredUsers().length).toBe(1);
    expect(service.filteredUsers()[0].email).toBe('merchant@store.com');

    // Filtro por Búsqueda
    service.setUserRoleFilter('ALL');
    service.setUserSearchQuery('Carlos');
    expect(service.filteredUsers().length).toBe(1);
    expect(service.filteredUsers()[0].firstName).toBe('Carlos');

    // Filtro por Estado
    service.setUserSearchQuery('');
    service.setUserStatusFilter('SUSPENDED');
    expect(service.filteredUsers().length).toBe(1);
    expect(service.filteredUsers()[0].status).toBe('SUSPENDED');
  });

  it('debe filtrar tiendas por estado, modalidad y búsqueda de texto', () => {
    service.stores.set(mockStores);

    service.setStoreSearchQuery('Capri');
    expect(service.filteredStores().length).toBe(1);
    expect(service.filteredStores()[0].name).toBe('Dulce Capri');

    service.setStoreSearchQuery('');
    service.setStoreStatusFilter('SUSPENDED');
    expect(service.filteredStores().length).toBe(1);
    expect(service.filteredStores()[0].status).toBe('SUSPENDED');

    service.setStoreStatusFilter('ALL');
    service.setStoreModalityFilter('DELIVERY');
    expect(service.filteredStores().length).toBe(1);
    expect(service.filteredStores()[0].deliveryEnabled).toBe(true);
  });

  it('debe filtrar comerciantes por búsqueda y estado', () => {
    service.users.set(mockUsers);

    expect(service.merchants().length).toBe(1);
    expect(service.merchants()[0].fullName).toBe('Valeria Ramos');

    service.setMerchantSearchQuery('Valeria');
    expect(service.filteredMerchants().length).toBe(1);

    service.setMerchantSearchQuery('Inexistente');
    expect(service.filteredMerchants().length).toBe(0);

    service.setMerchantSearchQuery('');
    service.setMerchantStatusFilter('SUSPENDED');
    expect(service.filteredMerchants().length).toBe(0);

    service.setMerchantStatusFilter('ACTIVE');
    expect(service.filteredMerchants().length).toBe(1);
  });

  it('debe suspender un usuario y actualizar la lista', () => {
    service.users.set(mockUsers);
    const updatedUser = { ...mockUsers[1], status: 'SUSPENDED' as const };

    service.suspendUser('usr-2').subscribe((res) => {
      expect(res.status).toBe('SUSPENDED');
      expect(toastMock.warning).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/users/usr-2/suspend`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedUser);

    const target = service.users().find((u) => u.id === 'usr-2');
    expect(target?.status).toBe('SUSPENDED');
  });

  it('debe activar un usuario y actualizar la lista', () => {
    service.users.set(mockUsers);
    const updatedUser = { ...mockUsers[2], status: 'ACTIVE' as const };

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

  it('debe actualizar los roles de un usuario y actualizar la lista', () => {
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

  it('debe suspender una tienda y actualizar la lista', () => {
    service.stores.set(mockStores);
    const updatedStore = { ...mockStores[0], status: 'SUSPENDED' as const };

    service.suspendStore('str-1').subscribe((res) => {
      expect(res.status).toBe('SUSPENDED');
      expect(toastMock.warning).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/stores/str-1/suspend`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedStore);

    const target = service.stores().find((s) => s.id === 'str-1');
    expect(target?.status).toBe('SUSPENDED');
  });

  it('debe activar una tienda y actualizar la lista', () => {
    service.stores.set(mockStores);
    const updatedStore = { ...mockStores[1], status: 'ACTIVE' as const };

    service.activateStore('str-2').subscribe((res) => {
      expect(res.status).toBe('ACTIVE');
      expect(toastMock.success).toHaveBeenCalled();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/stores/str-2/activate`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedStore);

    const target = service.stores().find((s) => s.id === 'str-2');
    expect(target?.status).toBe('ACTIVE');
  });

  it('debe limpiar caché con clearCache()', () => {
    service.users.set(mockUsers);
    service.stores.set(mockStores);

    service.clearCache();

    expect(service.users().length).toBe(0);
    expect(service.stores().length).toBe(0);
  });
});
