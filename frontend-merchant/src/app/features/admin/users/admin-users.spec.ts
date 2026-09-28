import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUsers } from './admin-users';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { of } from 'rxjs';

describe('AdminUsers', () => {
  let component: AdminUsers;
  let fixture: ComponentFixture<AdminUsers>;
  let adminServiceMock: any;

  const mockUsers: AdminUserSummary[] = [
    {
      id: 'usr-1',
      email: 'valeria@store.com',
      firstName: 'Valeria',
      lastName: 'Ramos',
      fullName: 'Valeria Ramos',
      status: 'ACTIVE',
      roles: ['MERCHANT'],
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    adminServiceMock = {
      users: vi.fn().mockReturnValue(mockUsers),
      filteredUsers: vi.fn().mockReturnValue(mockUsers),
      metrics: vi.fn().mockReturnValue({ totalUsers: 1 }),
      isLoadingUsers: vi.fn().mockReturnValue(false),
      userSearchQuery: vi.fn().mockReturnValue(''),
      userRoleFilter: vi.fn().mockReturnValue('ALL'),
      userStatusFilter: vi.fn().mockReturnValue('ALL'),
      loadUsers: vi.fn().mockReturnValue(of(mockUsers)),
      setUserSearchQuery: vi.fn(),
      setUserRoleFilter: vi.fn(),
      setUserStatusFilter: vi.fn(),
      suspendUser: vi.fn().mockReturnValue(of({ ...mockUsers[0], status: 'SUSPENDED' })),
      activateUser: vi.fn().mockReturnValue(of({ ...mockUsers[0], status: 'ACTIVE' })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminUsers],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsers);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe abrir y cerrar el modal de estado', () => {
    component.onOpenStatusModal(mockUsers[0]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedUser()).toEqual(mockUsers[0]);

    component.onCloseModal();
    expect(component.isModalOpen()).toBe(false);
    expect(component.selectedUser()).toBeNull();
  });

  it('debe ejecutar suspendUser al confirmar usuario activo', () => {
    component.onConfirmStatusChange(mockUsers[0]);
    expect(adminServiceMock.suspendUser).toHaveBeenCalledWith('usr-1');
  });

  it('debe ejecutar activateUser al confirmar usuario suspendido', () => {
    const suspendedUser: AdminUserSummary = { ...mockUsers[0], status: 'SUSPENDED' };
    component.onConfirmStatusChange(suspendedUser);
    expect(adminServiceMock.activateUser).toHaveBeenCalledWith('usr-1');
  });

  it('debe abrir y cerrar la ficha 360 de usuario', () => {
    component.onOpenUserDrawer(mockUsers[0]);
    expect(component.isDrawerOpen()).toBe(true);
    expect(component.selectedUser()).toEqual(mockUsers[0]);

    component.onCloseUserDrawer();
    expect(component.isDrawerOpen()).toBe(false);
    expect(component.selectedUser()).toBeNull();
  });

  it('debe actualizar la paginación de usuarios', () => {
    component.onSearchChange('Valeria');
    expect(adminServiceMock.setUserSearchQuery).toHaveBeenCalledWith('Valeria');
    expect(component.currentPage()).toBe(1);

    component.onRoleChange('MERCHANT');
    expect(adminServiceMock.setUserRoleFilter).toHaveBeenCalledWith('MERCHANT');
    expect(component.currentPage()).toBe(1);

    component.onStatusChange('ACTIVE');
    expect(adminServiceMock.setUserStatusFilter).toHaveBeenCalledWith('ACTIVE');
    expect(component.currentPage()).toBe(1);

    component.onPageChange(3);
    expect(component.currentPage()).toBe(3);
  });
});
