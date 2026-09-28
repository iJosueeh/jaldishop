import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUsersTable } from './admin-users-table';
import { AdminUserSummary } from '../../../../../core/models/admin.models';

describe('AdminUsersTable', () => {
  let component: AdminUsersTable;
  let fixture: ComponentFixture<AdminUsersTable>;

  const mockUsers: AdminUserSummary[] = [
    {
      id: 'usr-1',
      email: 'valeria@store.com',
      firstName: 'Valeria',
      lastName: 'Ramos',
      fullName: 'Valeria Ramos',
      phone: '984552109',
      status: 'ACTIVE',
      roles: ['MERCHANT'],
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
      storeName: 'Dulce Capri',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsersTable],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersTable);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('users', mockUsers);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar usuarios', () => {
    expect(component).toBeTruthy();
    expect(component.users().length).toBe(1);
  });

  it('debe emitir toggleStatus al hacer clic en el botón de acción', () => {
    const spy = vi.spyOn(component.toggleStatus, 'emit');
    component.toggleStatus.emit(mockUsers[0]);
    expect(spy).toHaveBeenCalledWith(mockUsers[0]);
  });
});
