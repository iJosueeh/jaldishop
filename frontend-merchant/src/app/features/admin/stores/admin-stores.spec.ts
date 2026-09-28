import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStores } from './admin-stores';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStoreSummary } from '../../../core/models/admin.models';
import { of } from 'rxjs';

describe('AdminStores', () => {
  let component: AdminStores;
  let fixture: ComponentFixture<AdminStores>;
  let adminServiceMock: any;

  const mockStores: AdminStoreSummary[] = [
    {
      id: 'str-1',
      merchantUserId: 'usr-1',
      name: 'Dulce Capri',
      slug: 'dulce-capri',
      status: 'ACTIVE',
      deliveryEnabled: true,
      pickupEnabled: true,
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
      merchant: {
        id: 'usr-1',
        email: 'v@s.com',
        fullName: 'Valeria Ramos',
        status: 'ACTIVE',
      },
    },
  ];

  beforeEach(async () => {
    adminServiceMock = {
      stores: vi.fn().mockReturnValue(mockStores),
      filteredStores: vi.fn().mockReturnValue(mockStores),
      metrics: vi.fn().mockReturnValue({ totalStores: 1 }),
      isLoadingStores: vi.fn().mockReturnValue(false),
      storeSearchQuery: vi.fn().mockReturnValue(''),
      storeStatusFilter: vi.fn().mockReturnValue('ALL'),
      storeModalityFilter: vi.fn().mockReturnValue('ALL'),
      loadStores: vi.fn().mockReturnValue(of(mockStores)),
      setStoreSearchQuery: vi.fn(),
      setStoreStatusFilter: vi.fn(),
      setStoreModalityFilter: vi.fn(),
      suspendStore: vi.fn().mockReturnValue(of({ ...mockStores[0], status: 'SUSPENDED' })),
      activateStore: vi.fn().mockReturnValue(of({ ...mockStores[0], status: 'ACTIVE' })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminStores],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminStores);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe abrir y cerrar el modal de estado de tienda', () => {
    component.onOpenStatusModal(mockStores[0]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedStore()).toEqual(mockStores[0]);

    component.onCloseModal();
    expect(component.isModalOpen()).toBe(false);
    expect(component.selectedStore()).toBeNull();
  });

  it('debe abrir y cerrar la ficha 360 (drawer) de tienda', () => {
    component.onSelectStore(mockStores[0]);
    expect(component.isDrawerOpen()).toBe(true);
    expect(component.drawerStore()).toEqual(mockStores[0]);

    component.onCloseDrawer();
    expect(component.isDrawerOpen()).toBe(false);
    expect(component.drawerStore()).toBeNull();
  });

  it('debe abrir el modal de estado desde la ficha 360', () => {
    component.onSelectStore(mockStores[0]);
    component.onDrawerStatusChange(mockStores[0]);
    expect(component.isDrawerOpen()).toBe(false);
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedStore()).toEqual(mockStores[0]);
  });

  it('debe actualizar el filtro de modalidad y paginación', () => {
    component.onModalityChange('DELIVERY');
    expect(adminServiceMock.setStoreModalityFilter).toHaveBeenCalledWith('DELIVERY');
    expect(component.currentPage()).toBe(1);

    component.onPageChange(2);
    expect(component.currentPage()).toBe(2);
  });
});
