import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserRoleModal } from './user-role-modal';
import { AdminUserSummary } from '../../../../../core/models/admin.models';

describe('UserRoleModal', () => {
  let component: UserRoleModal;
  let fixture: ComponentFixture<UserRoleModal>;

  const mockUser: AdminUserSummary = {
    id: 'u1',
    email: 'carlos@jaldishop.com',
    firstName: 'Carlos',
    lastName: 'Vargas',
    fullName: 'Carlos Vargas',
    phone: '987654321',
    status: 'ACTIVE',
    roles: ['CUSTOMER'],
    createdAt: '2026-03-01T10:00:00Z',
    updatedAt: '2026-03-01T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserRoleModal],
    }).compileComponents();

    fixture = TestBed.createComponent(UserRoleModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('user', mockUser);
    fixture.componentRef.setInput('targetRole', 'ADMIN');
    fixture.componentRef.setInput('action', 'ADD');
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular los nuevos roles al agregar un rol', () => {
    expect(component.calculatedNewRoles()).toEqual(['CUSTOMER', 'ADMIN']);
  });

  it('debe calcular los nuevos roles al remover un rol', () => {
    const multiRoleUser: AdminUserSummary = {
      ...mockUser,
      roles: ['CUSTOMER', 'MERCHANT'],
    };
    fixture.componentRef.setInput('user', multiRoleUser);
    fixture.componentRef.setInput('targetRole', 'MERCHANT');
    fixture.componentRef.setInput('action', 'REMOVE');
    fixture.detectChanges();

    expect(component.calculatedNewRoles()).toEqual(['CUSTOMER']);
  });

  it('debe emitir confirm con los detalles y nuevos roles', () => {
    const spy = vi.spyOn(component.confirm, 'emit');
    component.onConfirm();
    expect(spy).toHaveBeenCalledWith({
      user: mockUser,
      targetRole: 'ADMIN',
      action: 'ADD',
      newRoles: ['CUSTOMER', 'ADMIN'],
    });
  });

  it('debe emitir cancel al invocar cancel.emit()', () => {
    const spy = vi.spyOn(component.cancel, 'emit');
    component.cancel.emit();
    expect(spy).toHaveBeenCalled();
  });
});
