import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStoreDrawer } from './admin-store-drawer';
import { ToastService } from '../../../../../core/services/toast.service';
import { AdminService } from '../../../../../core/services/admin.service';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { of } from 'rxjs';

describe('AdminStoreDrawer', () => {
  let component: AdminStoreDrawer;
  let fixture: ComponentFixture<AdminStoreDrawer>;

  const mockToastService = {
    info: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  };

  const mockAdminService = {
    getStoreDetail: vi.fn().mockReturnValue(
      of({
        legalBusinessName: 'Panadería San José S.A.C.',
        taxIdType: 'RUC_20',
        taxIdNumber: '20123456789',
        taxRegime: 'MYPE',
        canIssueInvoices: true,
      })
    ),
    loadAnalyticsSummary: vi.fn().mockReturnValue(of(null)),
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
      providers: [
        { provide: ToastService, useValue: mockToastService },
        { provide: AdminService, useValue: mockAdminService },
      ],
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

  it('debe copiar el ID de la tienda y notificar', async () => {
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });

    component.copyId(mockStore.id);
    await Promise.resolve();

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(mockStore.id);
    expect(mockToastService.info).toHaveBeenCalledWith(
      'ID de tienda copiado al portapapeles.',
      'Copiado'
    );
  });

  it('debe cargar datos fiscales y de liquidación al inicializar', () => {
    expect(mockAdminService.getStoreDetail).toHaveBeenCalledWith(mockStore.id, true);
    expect(mockAdminService.loadAnalyticsSummary).toHaveBeenCalledWith(undefined, undefined, mockStore.id);
  });
});
