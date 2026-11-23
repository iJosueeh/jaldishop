import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminReconciliation } from './admin-reconciliation';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { signal } from '@angular/core';
import { of } from 'rxjs';

describe('AdminReconciliation', () => {
  let component: AdminReconciliation;
  let fixture: ComponentFixture<AdminReconciliation>;
  let adminServiceMock: any;
  let toastMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      analyticsSummary: signal({
        financial: {
          orphanPaymentsAmount: 500,
          orphanPaymentsCount: 2,
          orderLinkedPaymentsAmount: 15000,
          orderLinkedPaymentsCount: 45,
        },
      }),
      isLoadingAnalytics: signal(false),
      loadAnalyticsSummary: vi.fn().mockReturnValue(of({})),
      getReconciliationCandidates: vi.fn().mockReturnValue(of([
        {
          attemptId: '123e4567-e89b-12d3-a456-426614174000',
          paymentId: '223e4567-e89b-12d3-a456-426614174000',
          amount: 50.0,
          currency: 'PEN',
          storeName: 'Bodega Central',
          customerEmail: 'cliente@test.com',
          incidenceType: 'ORPHAN_APPROVED',
          providerPaymentId: 'MP-998877',
        },
      ])),
      investigatePaymentAttempt: vi.fn().mockReturnValue(
        of({ outcome: 'VERIFIED', reason: 'Coincidencia confirmada en Mercado Pago.' })
      ),
      reconcilePaymentAttempt: vi.fn().mockReturnValue(
        of({ orderId: 'ord-123', status: 'CONFIRMED', orderNumber: 1001 })
      ),
    };

    toastMock = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminReconciliation],
      providers: [
        { provide: AdminService, useValue: adminServiceMock },
        { provide: ToastService, useValue: toastMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReconciliation);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente y mostrar los indicadores de contingencia', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Pagos Huérfanos Aprobados');
    expect(compiled.textContent).toContain('500.00');
  });

  it('debe validar el formato del UUID antes de investigar', () => {
    component.onInputId('id-invalido');
    component.investigate();
    expect(component.inputError()).toContain('Formato inválido');
    expect(adminServiceMock.investigatePaymentAttempt).not.toHaveBeenCalled();
  });

  it('debe ejecutar la investigación ante un UUID válido y actualizar el historial', () => {
    const validUuid = '123e4567-e89b-12d3-a456-426614174000';
    component.onInputId(validUuid);
    component.investigate();

    expect(adminServiceMock.investigatePaymentAttempt).toHaveBeenCalledWith(validUuid);
    expect(component.investigationResult()?.outcome).toBe('VERIFIED');
    expect(component.sessionHistory().length).toBe(1);
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('debe ejecutar la conciliación forzada ante un UUID válido', () => {
    const validUuid = '123e4567-e89b-12d3-a456-426614174000';
    component.onInputId(validUuid);
    component.reconcile();

    expect(adminServiceMock.reconcilePaymentAttempt).toHaveBeenCalledWith(validUuid);
    expect(component.reconciliationResult()?.status).toBe('CONFIRMED');
    expect(toastMock.success).toHaveBeenCalled();
  });

  it('debe permitir seleccionar una incidencia desde los candidatos cargados y disparar investigación', () => {
    expect(component.candidates().length).toBe(1);
    const candidate = component.candidates()[0];

    component.selectCandidate(candidate);

    expect(component.selectedCandidate()).toEqual(candidate);
    expect(component.attemptIdInput()).toBe(candidate.attemptId);
    expect(adminServiceMock.investigatePaymentAttempt).toHaveBeenCalledWith(candidate.attemptId);
  });
});
