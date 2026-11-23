import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentReconciliationModal } from './payment-reconciliation-modal';
import { AdminService } from '../../../../../core/services/admin.service';
import { of } from 'rxjs';

describe('PaymentReconciliationModal', () => {
  let component: PaymentReconciliationModal;
  let fixture: ComponentFixture<PaymentReconciliationModal>;
  let adminServiceMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      investigatePaymentAttempt: vi.fn().mockReturnValue(
        of({
          attemptId: '123e4567-e89b-12d3-a456-426614174000',
          providerStatus: 'approved',
          status: 'SUCCESS_CONFIRMED',
          detail: 'Pago verificado',
        })
      ),
      reconcilePaymentAttempt: vi.fn().mockReturnValue(
        of({
          attemptId: '123e4567-e89b-12d3-a456-426614174000',
          status: 'SUCCESS_CONFIRMED',
          detail: 'Orden confirmada',
        })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [PaymentReconciliationModal],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentReconciliationModal);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe validar formato UUID antes de investigar', () => {
    component.attemptId.set('invalido');
    component.onInvestigate();
    expect(component.errorMessage()).toContain('UUID válido');
    expect(adminServiceMock.investigatePaymentAttempt).not.toHaveBeenCalled();
  });

  it('debe llamar a investigar cuando el UUID es válido', () => {
    const validUuid = '123e4567-e89b-12d3-a456-426614174000';
    component.attemptId.set(validUuid);
    component.onInvestigate();

    expect(adminServiceMock.investigatePaymentAttempt).toHaveBeenCalledWith(validUuid);
    expect(component.investigationResult()).toBeTruthy();
  });

  it('debe llamar a conciliar cuando se pulsa Forzar Conciliación', () => {
    const validUuid = '123e4567-e89b-12d3-a456-426614174000';
    component.attemptId.set(validUuid);
    component.onReconcile();

    expect(adminServiceMock.reconcilePaymentAttempt).toHaveBeenCalledWith(validUuid);
    expect(component.reconciliationResult()).toBeTruthy();
  });
});
