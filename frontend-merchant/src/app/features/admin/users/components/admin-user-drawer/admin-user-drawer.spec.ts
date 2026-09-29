import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUserDrawer } from './admin-user-drawer';
import { ToastService } from '../../../../../core/services/toast.service';
import { AdminUserSummary } from '../../../../../core/models/admin.models';

describe('AdminUserDrawer', () => {
  let component: AdminUserDrawer;
  let fixture: ComponentFixture<AdminUserDrawer>;

  const mockToastService = {
    info: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  };

  const mockUser: AdminUserSummary = {
    id: 'u1234567-89ab-cdef-0123-456789abcdef',
    email: 'carlos@jaldishop.com',
    firstName: 'Carlos',
    lastName: 'Vargas',
    fullName: 'Carlos Vargas',
    phone: '912345678',
    status: 'ACTIVE',
    roles: ['MERCHANT'],
    createdAt: '2026-03-01T10:00:00Z',
    updatedAt: '2026-03-01T10:00:00Z',
    storeId: 's1',
    storeName: 'Bodega Central',
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AdminUserDrawer],
      providers: [{ provide: ToastService, useValue: mockToastService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserDrawer);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('user', mockUser);
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir statusChange al accionar el botón de cambio de estado', () => {
    const spy = vi.spyOn(component.statusChange, 'emit');
    component.onToggleStatus();
    expect(spy).toHaveBeenCalledWith(mockUser);
  });

  it('debe activar dismiss y emitir closeDrawer con animación', () => {
    vi.useFakeTimers();
    const spy = vi.spyOn(component.closeDrawer, 'emit');
    component.dismiss();
    expect(component.isClosing()).toBe(true);

    vi.advanceTimersByTime(200);
    expect(spy).toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('debe enviar restablecimiento de contraseña mostrando un toast de éxito', () => {
    component.onSendPasswordReset();
    expect(mockToastService.success).toHaveBeenCalledWith(
      expect.stringContaining(mockUser.email),
      'Acción de Soporte'
    );
  });

  it('debe emitir roleToggle y roleChangeRequest al alternar rol de usuario', () => {
    const toggleSpy = vi.spyOn(component.roleToggle, 'emit');
    const requestSpy = vi.spyOn(component.roleChangeRequest, 'emit');

    component.onToggleRole('ADMIN');

    expect(toggleSpy).toHaveBeenCalledWith({ user: mockUser, role: 'ADMIN' });
    expect(requestSpy).toHaveBeenCalledWith({
      user: mockUser,
      targetRole: 'ADMIN',
      action: 'ADD',
    });
  });

  it('debe guardar nota interna de auditoría correctamente', () => {
    vi.useFakeTimers();
    component.adminNote.set('Cliente verificado por DNI');
    component.onSaveNote();
    expect(component.isNoteSaved()).toBe(true);
    expect(mockToastService.success).toHaveBeenCalledWith(
      'Nota interna de auditoría guardada.',
      'Nota Admin'
    );

    vi.advanceTimersByTime(2600);
    expect(component.isNoteSaved()).toBe(false);
    vi.useRealTimers();
  });
});

