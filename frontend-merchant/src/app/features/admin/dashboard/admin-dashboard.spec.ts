import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminDashboard } from './admin-dashboard';
import { AdminService } from '../../../core/services/admin.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('AdminDashboard', () => {
  let component: AdminDashboard;
  let fixture: ComponentFixture<AdminDashboard>;
  let adminServiceMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      users: vi.fn().mockReturnValue([
        { id: 'usr-1', fullName: 'Valeria Ramos', email: 'v@s.com', roles: ['MERCHANT'], status: 'ACTIVE' },
      ]),
      stores: vi.fn().mockReturnValue([
        { id: 'str-1', name: 'Dulce Capri', slug: 'dulce-capri', status: 'ACTIVE' },
      ]),
      metrics: vi.fn().mockReturnValue({
        totalUsers: 1,
        totalStores: 1,
        activeStores: 1,
      }),
      isLoadingUsers: vi.fn().mockReturnValue(false),
      isLoadingStores: vi.fn().mockReturnValue(false),
      loadUsers: vi.fn().mockReturnValue(of([])),
      loadStores: vi.fn().mockReturnValue(of([])),
      suspendUser: vi.fn().mockReturnValue(of({})),
      activateUser: vi.fn().mockReturnValue(of({})),
      suspendStore: vi.fn().mockReturnValue(of({})),
      activateStore: vi.fn().mockReturnValue(of({})),
      setStoreStatusFilter: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        provideRouter([{ path: 'admin/stores', component: AdminDashboard }]),
        { provide: AdminService, useValue: adminServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente y cargar datos al inicializar', () => {
    expect(component).toBeTruthy();
    expect(adminServiceMock.loadUsers).toHaveBeenCalled();
    expect(adminServiceMock.loadStores).toHaveBeenCalled();
  });

  it('debe refrescar datos con forceRefresh al llamar refreshData()', () => {
    component.refreshData();
    expect(adminServiceMock.loadUsers).toHaveBeenCalledWith(true);
    expect(adminServiceMock.loadStores).toHaveBeenCalledWith(true);
  });

  it('debe abrir y cerrar la ficha 360 de usuario', () => {
    const mockUser: any = { id: 'usr-1', fullName: 'Valeria Ramos', status: 'ACTIVE' };
    component.onSelectUser(mockUser);
    expect(component.isUserDrawerOpen()).toBe(true);
    expect(component.drawerUser()).toEqual(mockUser);

    component.onCloseUserDrawer();
    expect(component.isUserDrawerOpen()).toBe(false);
    expect(component.drawerUser()).toBeNull();
  });

  it('debe abrir y cerrar la ficha 360 de tienda', () => {
    const mockStore: any = { id: 'str-1', name: 'Dulce Capri', status: 'ACTIVE' };
    component.onSelectStore(mockStore);
    expect(component.isStoreDrawerOpen()).toBe(true);
    expect(component.drawerStore()).toEqual(mockStore);

    component.onCloseStoreDrawer();
    expect(component.isStoreDrawerOpen()).toBe(false);
    expect(component.drawerStore()).toBeNull();
  });

  it('debe filtrar tiendas suspendidas y navegar al invocar onFilterSuspendedStores()', () => {
    component.onFilterSuspendedStores();
    expect(adminServiceMock.setStoreStatusFilter).toHaveBeenCalledWith('SUSPENDED');
  });
});
