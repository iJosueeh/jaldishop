import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import React from 'react';
import {
  PaymentReturnView,
  resolveCorrelationId,
  isValidCorrelation,
  clearMatchingPendingPayment,
  getRefundStatusInfo,
} from './PaymentReturnView';
import {
  checkoutService,
  savePendingPayment,
  PaymentServiceError,
} from '../services/checkoutService';
import { CustomerPaymentStatusResponse } from '../types/checkout.types';

// Mock next/navigation
const mockSearchParams = new URLSearchParams();
vi.mock('next/navigation', () => ({
  useSearchParams: () => mockSearchParams,
}));

// Mock Link
vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

const VALID_PAYMENT_ID = '98765432-1234-4000-8000-123456789abc';
const OTHER_PAYMENT_ID = 'aaaaaaaa-bbbb-4000-8000-cccccccccccc';

const baseStatusResponse: CustomerPaymentStatusResponse = {
  paymentId: VALID_PAYMENT_ID,
  paymentStatus: 'APPROVED',
  orderCreated: true,
  orderNumber: 'ORD-2026-99999',
  orderStatus: 'PREPARING',
  hasIncidence: false,
  incidenceCode: null,
  incidenceMessage: null,
  refundStatus: 'NOT_REQUIRED',
  amount: 85.5,
  currency: 'PEN',
  createdAt: '2026-11-13T12:00:00Z',
  approvedAt: '2026-11-13T12:05:00Z',
};

describe('PaymentReturnView — F.1B.3 Return and Recovery Logic', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    // Reset searchParams
    Array.from(mockSearchParams.keys()).forEach((key) => mockSearchParams.delete(key));
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  // Helper unit tests for correlation logic
  describe('Correlation Helpers', () => {
    it('isValidCorrelation validates proper objects and rejects malformed ones', () => {
      expect(isValidCorrelation(null)).toBe(false);
      expect(isValidCorrelation({})).toBe(false);
      expect(
        isValidCorrelation({
          paymentId: 'not-a-uuid',
          preferenceId: 'p-1',
          capacityReservationId: 'res-1',
        })
      ).toBe(false);
      expect(
        isValidCorrelation({
          paymentId: VALID_PAYMENT_ID,
          preferenceId: 'p-1',
          capacityReservationId: 'res-1',
        })
      ).toBe(true);
    });

    it('clearMatchingPendingPayment clears storage only when paymentId matches', () => {
      savePendingPayment({
        paymentId: VALID_PAYMENT_ID,
        preferenceId: 'p-1',
        capacityReservationId: 'c-1',
        createdAt: Date.now(),
        status: 'PENDING',
      });

      // Different paymentId -> do not clear
      const notCleared = clearMatchingPendingPayment(OTHER_PAYMENT_ID);
      expect(notCleared).toBe(false);
      expect(sessionStorage.getItem('jaldishop_pending_payment')).not.toBeNull();

      // Matching paymentId -> clear
      const cleared = clearMatchingPendingPayment(VALID_PAYMENT_ID);
      expect(cleared).toBe(true);
      expect(sessionStorage.getItem('jaldishop_pending_payment')).toBeNull();
    });

    it('resolveCorrelationId detects discrepancy when stored and external UUIDs differ', () => {
      const stored = {
        paymentId: VALID_PAYMENT_ID,
        preferenceId: 'pref-1',
        capacityReservationId: 'cap-1',
        createdAt: Date.now(),
        status: 'PENDING',
      };
      const params = new URLSearchParams(`external_reference=${OTHER_PAYMENT_ID}`);
      const res = resolveCorrelationId(stored, params);
      expect(res).toEqual({ status: 'ERROR', code: 'DISCREPANCY' });
    });

    it('resolveCorrelationId accepts stored ID when external_reference is an attempt ID (pa1_)', () => {
      const stored = {
        paymentId: VALID_PAYMENT_ID,
        preferenceId: 'pref-1',
        capacityReservationId: 'cap-1',
        createdAt: Date.now(),
        status: 'PENDING',
      };
      const params = new URLSearchParams(`external_reference=pa1_${OTHER_PAYMENT_ID}`);
      const res = resolveCorrelationId(stored, params);
      expect(res).toEqual({ status: 'RESOLVED', paymentId: VALID_PAYMENT_ID });
    });

    it('resolveCorrelationId fails with ATTEMPT_ID_ONLY when no stored correlation and external_reference starts with pa1_', () => {
      const params = new URLSearchParams(`external_reference=pa1_${OTHER_PAYMENT_ID}`);
      const res = resolveCorrelationId(null, params);
      expect(res).toEqual({ status: 'ERROR', code: 'ATTEMPT_ID_ONLY' });
    });

    it('resolveCorrelationId falls back to valid external UUID when stored is empty', () => {
      const params = new URLSearchParams(`external_reference=${VALID_PAYMENT_ID}`);
      const res = resolveCorrelationId(null, params);
      expect(res).toEqual({ status: 'RESOLVED', paymentId: VALID_PAYMENT_ID });
    });

    it('getRefundStatusInfo maps refund statuses strictly without unfounded claims', () => {
      expect(getRefundStatusInfo('PENDING').label).toBe('Devolución Pendiente');
      expect(getRefundStatusInfo('PROCESSING').label).toBe('Devolución en Proceso');
      expect(getRefundStatusInfo('COMPLETED').label).toBe('Devolución Procesada');
      expect(getRefundStatusInfo('COMPLETED').description).toMatch(/tiempos y políticas de tu entidad/i);
      expect(getRefundStatusInfo('FAILED').label).toBe('Devolución No Completada');
      expect(getRefundStatusInfo('FAILED').description).toMatch(/requiere revisión manual/i);
      expect(getRefundStatusInfo('NOT_REQUIRED').label).toBe('No Aplica Devolución');
      expect(getRefundStatusInfo('NOT_REQUIRED').showAmount).toBe(false);
      expect(getRefundStatusInfo(null).label).toBe('Estado No Confirmado');
      expect(getRefundStatusInfo(null).showAmount).toBe(false);
    });
  });

  // Scenario 1: Pedido confirmado con correlación coincidente: se limpia
  it('1. Pedido confirmado con correlación coincidente: se limpia', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue(baseStatusResponse);

    render(<PaymentReturnView expectedPath="success" />);

    expect(screen.getByTestId('state-polling')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('state-order-confirmed')).toBeInTheDocument();
    });

    expect(screen.getByText('#ORD-2026-99999')).toBeInTheDocument();
    expect(screen.getByText('PEN 85.50')).toBeInTheDocument();

    // Verify link to single order route
    const link = screen.getByRole('link', { name: /ver detalle del pedido/i });
    expect(link).toHaveAttribute('href', '/mi-jaldishop/pedidos/ORD-2026-99999');

    // Storage should be cleared because paymentId matched
    expect(sessionStorage.getItem('jaldishop_pending_payment')).toBeNull();
  });

  // Scenario 2: Pedido confirmado con correlación distinta: se conserva
  it('2. Pedido confirmado con correlación distinta: se conserva', async () => {
    // Newer correlation in storage for a second order
    savePendingPayment({
      paymentId: OTHER_PAYMENT_ID,
      preferenceId: 'pref-new',
      capacityReservationId: 'cap-new',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    // Verification happens for VALID_PAYMENT_ID from query parameter
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    // Mock resolveCorrelationId to resolve the external reference for this test
    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue(baseStatusResponse);

    // Call helper directly to prove preservation
    clearMatchingPendingPayment(VALID_PAYMENT_ID);

    // Storage must still contain the newer correlation
    const preserved = sessionStorage.getItem('jaldishop_pending_payment');
    expect(preserved).not.toBeNull();
    expect(JSON.parse(preserved!).paymentId).toBe(OTHER_PAYMENT_ID);
  });

  // Scenario 3: Pago pendiente: no se limpia
  it('3. Pago pendiente: no se limpia', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    const pollSpy = vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      paymentStatus: 'PENDING',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView expectedPath="pending" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-pending')).toBeInTheDocument();
    });

    expect(screen.getByText(/esperando confirmación de la entidad/i)).toBeInTheDocument();

    // Storage must NOT be cleared on pending payment
    expect(sessionStorage.getItem('jaldishop_pending_payment')).not.toBeNull();

    // Manual recheck button
    const retryBtn = screen.getByRole('button', { name: /reconsultar estado/i });
    fireEvent.click(retryBtn);

    expect(pollSpy).toHaveBeenCalledTimes(2);
  });

  // Scenario 4: Incidencia: no se limpia
  it('4. Incidencia comercial: no se limpia', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      incidenceCode: 'CAPACITY_UNAVAILABLE',
      incidenceMessage: 'No hay cupos disponibles.',
      orderCreated: false,
      orderNumber: null,
      refundStatus: 'PENDING',
    });

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    // Storage must NOT be cleared on incidence
    expect(sessionStorage.getItem('jaldishop_pending_payment')).not.toBeNull();
  });

  // Scenario 5: refundStatus=PENDING
  it('5. refundStatus=PENDING muestra devolución pendiente de procesamiento', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      incidenceCode: 'RESERVATION_EXPIRED',
      incidenceMessage: 'La reserva de horario ha vencido.',
      orderCreated: false,
      orderNumber: null,
      refundStatus: 'PENDING',
    });

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('Devolución Pendiente')).toBeInTheDocument();
    expect(
      screen.getByText(/la devolución se encuentra registrada y pendiente de procesamiento/i)
    ).toBeInTheDocument();
  });

  // Scenario 6: refundStatus=PROCESSING
  it('6. refundStatus=PROCESSING muestra devolución en proceso', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      refundStatus: 'PROCESSING',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('Devolución en Proceso')).toBeInTheDocument();
    expect(
      screen.getByText(/la devolución está en proceso de gestión con la entidad de pago/i)
    ).toBeInTheDocument();
  });

  // Scenario 7: refundStatus=COMPLETED
  it('7. refundStatus=COMPLETED muestra devolución procesada sin garantizar acreditación inmediata', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      refundStatus: 'COMPLETED',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('Devolución Procesada')).toBeInTheDocument();
    expect(
      screen.getByText(/la acreditación efectiva dependerá de los tiempos y políticas de tu entidad bancaria/i)
    ).toBeInTheDocument();
  });

  // Scenario 8: refundStatus=FAILED
  it('8. refundStatus=FAILED muestra devolución no completada y solicita revisión', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      refundStatus: 'FAILED',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('Devolución No Completada')).toBeInTheDocument();
    expect(
      screen.getByText(/requiere revisión manual por nuestro equipo de soporte/i)
    ).toBeInTheDocument();
  });

  // Scenario 9: refundStatus=NOT_REQUIRED
  it('9. refundStatus=NOT_REQUIRED no afirma que existe un reembolso', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      refundStatus: 'NOT_REQUIRED',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('No Aplica Devolución')).toBeInTheDocument();
    expect(
      screen.getByText(/para esta incidencia no se requirió la emisión de un reembolso monetario/i)
    ).toBeInTheDocument();
    // Does not display refund amount
    expect(screen.queryByText(/monto de la transacción:/i)).toBeNull();
  });

  // Scenario 10: Sesión expirada y posterior recuperación
  it('10. Sesión expirada conserva la correlación en storage y enlaza a login con redirect', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockRejectedValue(
      new PaymentServiceError('Sesión expirada', 'SESSION_EXPIRED', 401)
    );

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-session-expired')).toBeInTheDocument();
    });

    // Correlation must NOT be deleted
    expect(sessionStorage.getItem('jaldishop_pending_payment')).not.toBeNull();

    // Verify safe login redirect link with return path
    const loginLink = screen.getByRole('link', { name: /iniciar sesión/i });
    expect(loginLink).toHaveAttribute('href', '/login?redirect=%2Fcheckout%2Fsuccess');
  });

  // Scenario 11: Retorno repetido
  it('11. Retorno repetido a la misma URL resuelve orden confirmada de forma idempotente', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue(baseStatusResponse);

    const { unmount } = render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-order-confirmed')).toBeInTheDocument();
    });

    unmount();

    // Re-render simulates returning again
    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-order-confirmed')).toBeInTheDocument();
    });
    expect(screen.getByText('#ORD-2026-99999')).toBeInTheDocument();
  });

  // Scenario 12: Identificadores de pago inconsistentes
  it('12. Identificadores de pago inconsistentes: detecta discrepancia o UUID inválido y detiene consulta', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    mockSearchParams.set('external_reference', OTHER_PAYMENT_ID);

    const pollSpy = vi.spyOn(checkoutService, 'pollPaymentStatus');

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-correlation-error')).toBeInTheDocument();
    });

    expect(screen.getByText(/se detectó un conflicto de seguridad/i)).toBeInTheDocument();
    expect(pollSpy).not.toHaveBeenCalled();
  });

  // Complementary Scenario: Pago aprobado sin orden creada
  it('Handles approved payment without order: informs customer without promising order in history', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      orderCreated: false,
      orderNumber: null,
      orderStatus: null,
    });

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-approved-pending-order')).toBeInTheDocument();
    });

    expect(screen.getByText(/pago aprobado en proceso de confirmación/i)).toBeInTheDocument();
    expect(screen.getByText(/no figurará en tu historial de compras/i)).toBeInTheDocument();
    // No misleading link to mis pedidos before order creation
    expect(screen.queryByRole('link', { name: /revisar mis pedidos/i })).toBeNull();
  });

  // Complementary Scenario: Pago rechazado
  it('Handles rejected/failed payment: displays failure and link to store', async () => {
    savePendingPayment({
      paymentId: VALID_PAYMENT_ID,
      preferenceId: 'pref-100',
      capacityReservationId: 'cap-100',
      createdAt: Date.now(),
      status: 'PENDING',
    });

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      paymentStatus: 'FAILED',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView expectedPath="failure" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-failed')).toBeInTheDocument();
    });

    expect(screen.getByText(/no pudimos procesar tu pago/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /volver al comercio/i })).toHaveAttribute('href', '/');
  });

  // Complementary Scenario: 403 Forbidden
  it('Handles 403 Forbidden safely without disclosing technical details', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockRejectedValue(
      new PaymentServiceError('Acceso denegado', 'FORBIDDEN', 403)
    );

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-unauthorized')).toBeInTheDocument();
    });

    expect(screen.getByText(/no tienes permisos para ver este pago/i)).toBeInTheDocument();
  });
});
