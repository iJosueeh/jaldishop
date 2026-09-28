import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStoreDrawer } from './admin-store-drawer';
import { ToastService } from '../../../../../core/services/toast.service';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';

describe('AdminStoreDrawer', () => {
  let component: AdminStoreDrawer;
  let fixture: ComponentFixture<AdminStoreDrawer>;

  const mockToastService = {
    info: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  };

  const mockStore: AdminStoreSummary = {
    id: 's1234567-89ab-cdef-0123-456789abcdef',
    merchantUserId: 'u1',
    name: 'Panaderia San Jose',
    slug: 'panaderia-san-jose',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    contactPhone: '999888777',
    createdAt: '2026-03-01T10:00:00Z',
    updatedAt: '2026-03-01T10:00:00Z',
    merchant: {
      id: 'u1',
      email: 'maria@test.com',
      fullName: 'Maria Lopez',
      phone: '987654321',
      status: 'ACTIVE',
    },
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AdminStoreDrawer],
      providers: [{ provide: ToastService, useValue: mockToastService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminStoreDrawer);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('store', mockStore);
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir statusChange al accionar el botón de estado', () => {
    const spy = vi.spyOn(component.statusChange, 'emit');
    component.onToggleStatus();
    expect(spy).toHaveBeenCalledWith(mockStore);
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

  it('debe abrir la tienda pública en una nueva pestaña', () => {
    const windowSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
    component.openPublicStore(mockStore.slug);
    expect(windowSpy).toHaveBeenCalledWith('/store/panaderia-san-jose', '_blank');
  });

  it('debe emitir modalityChange y notificar al alternar modalidad', () => {
    const spy = vi.spyOn(component.modalityChange, 'emit');
    component.onToggleModality('pickup');
    expect(spy).toHaveBeenCalledWith({ store: mockStore, type: 'pickup' });
    expect(mockToastService.info).toHaveBeenCalledWith(
      expect.stringContaining('Pickup'),
      'Configuración de Tienda'
    );
  });

  it('debe guardar nota interna de auditoría de tienda correctamente', () => {
    vi.useFakeTimers();
    component.adminNote.set('Tienda inspeccionada y aprobada');
    component.onSaveNote();
    expect(component.isNoteSaved()).toBe(true);
    expect(mockToastService.success).toHaveBeenCalledWith(
      'Nota de auditoría de tienda guardada.',
      'Nota Admin'
    );

    vi.advanceTimersByTime(2600);
    expect(component.isNoteSaved()).toBe(false);
    vi.useRealTimers();
  });
});

