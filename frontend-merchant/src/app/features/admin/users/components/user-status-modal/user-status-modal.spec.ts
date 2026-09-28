import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserStatusModal } from './user-status-modal';
import { AdminUserSummary } from '../../../../../core/models/admin.models';

describe('UserStatusModal', () => {
  let component: UserStatusModal;
  let fixture: ComponentFixture<UserStatusModal>;

  const mockUser: AdminUserSummary = {
    id: 'usr-1',
    email: 'valeria@store.com',
    firstName: 'Valeria',
    lastName: 'Ramos',
    fullName: 'Valeria Ramos',
    status: 'ACTIVE',
    roles: ['MERCHANT'],
    createdAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-01T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserStatusModal],
    }).compileComponents();

    fixture = TestBed.createComponent(UserStatusModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('user', mockUser);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y mostrar el nombre del usuario', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(true);
    expect(component.user()?.fullName).toBe('Valeria Ramos');
  });

  it('debe emitir confirm con el usuario al confirmar', () => {
    const spy = vi.spyOn(component.confirm, 'emit');
    component.confirm.emit(mockUser);
    expect(spy).toHaveBeenCalledWith(mockUser);
  });

  it('debe emitir cancel al cancelar', () => {
    const spy = vi.spyOn(component.cancel, 'emit');
    component.cancel.emit();
    expect(spy).toHaveBeenCalled();
  });
});
