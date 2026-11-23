import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InviteAdminModal } from './invite-admin-modal';
import { AdminService } from '../../../../../core/services/admin.service';
import { of } from 'rxjs';

describe('InviteAdminModal', () => {
  let component: InviteAdminModal;
  let fixture: ComponentFixture<InviteAdminModal>;
  let adminServiceMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      invitations: vi.fn().mockReturnValue([
        {
          id: 'inv-1',
          email: 'admin2@jaldishop.com',
          firstName: 'Rosa',
          lastName: 'Mendez',
          status: 'PENDING',
          expiresAt: '2026-11-25T10:00:00Z',
          createdAt: '2026-11-23T10:00:00Z',
        },
      ]),
      loadInvitations: vi.fn().mockReturnValue(of([])),
      inviteAdmin: vi.fn().mockReturnValue(
        of({
          id: 'inv-2',
          email: 'nuevo@jaldishop.com',
          firstName: 'Nuevo',
          lastName: 'Admin',
          status: 'PENDING',
        })
      ),
      revokeAdminInvitation: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [InviteAdminModal],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(InviteAdminModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe validar formulario antes de enviar', () => {
    expect(component.isFormValid()).toBe(false);

    component.email.set('invalido');
    component.firstName.set('C');
    component.lastName.set('M');
    expect(component.isFormValid()).toBe(false);

    component.email.set('admin@jaldishop.com');
    component.firstName.set('Carlos');
    component.lastName.set('Mendoza');
    expect(component.isFormValid()).toBe(true);
  });

  it('debe enviar invitación y cambiar a pestaña de lista', () => {
    component.email.set('admin@jaldishop.com');
    component.firstName.set('Carlos');
    component.lastName.set('Mendoza');
    component.onSubmit();

    expect(adminServiceMock.inviteAdmin).toHaveBeenCalledWith({
      email: 'admin@jaldishop.com',
      firstName: 'Carlos',
      lastName: 'Mendoza',
      phone: undefined,
    });
    expect(component.activeTab()).toBe('list');
  });

  it('debe revocar invitación al llamar onRevoke()', () => {
    component.onRevoke('inv-1');
    expect(adminServiceMock.revokeAdminInvitation).toHaveBeenCalledWith('inv-1');
  });
});
