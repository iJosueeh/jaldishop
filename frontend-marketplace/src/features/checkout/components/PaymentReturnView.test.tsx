import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import React from 'react';
import {
  PaymentReturnView,
  resolveCorrelationId,
  isValidCorrelation,
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
  });

  // Scenario 1: Pago aprobado con pedido creado
  it('1. Handles approved payment with created order: displays order details, link to order, and clears correlation', async () => {
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

    // Storage should be cleared on confirmation
    expect(sessionStorage.getItem('jaldishop_pending_payment')).toBeNull();
  });

  // Scenario 2: Pago aprobado sin pedido todavía
  it('2. Handles approved payment without order: informs customer without promising order creation', async () => {
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

    expect(screen.getByText(/pago aprobado comercialmente en proceso/i)).toBeInTheDocument();
    expect(screen.getByText('PEN 85.50')).toBeInTheDocument();

    // Storage should NOT be cleared while order is not created
    expect(sessionStorage.getItem('jaldishop_pending_payment')).not.toBeNull();
  });

  // Scenario 3: Pago pendiente
  it('3. Handles pending payment: displays pending verification message and allows manual recheck', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

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

    // Manual recheck button
    const retryBtn = screen.getByRole('button', { name: /reconsultar estado/i });
    fireEvent.click(retryBtn);

    expect(pollSpy).toHaveBeenCalledTimes(2);
  });

  // Scenario 4: Pago rechazado
  it('4. Handles rejected/failed payment: displays failure without generating new payments automatically', async () => {
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

  // Scenario 5: Incidencia de confirmación
  it('5. Handles commercial confirmation incidence: displays friendly message and refund status', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      incidenceCode: 'RESERVATION_EXPIRED',
      incidenceMessage: 'La reserva de horario ha vencido. Se gestionará la devolución correspondiente.',
      orderCreated: false,
      orderNumber: null,
      refundStatus: 'PENDING',
    });

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText(/la reserva de horario ha vencido/i)).toBeInTheDocument();
    expect(screen.getByText('Devolución en Trámite')).toBeInTheDocument();
  });

  // Scenario 6: Devolución pendiente
  it('6. Distinguishes pending refund status', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      hasIncidence: true,
      refundStatus: 'PENDING',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-incidence')).toBeInTheDocument();
    });

    expect(screen.getByText('Devolución en Trámite')).toBeInTheDocument();
  });

  // Scenario 7: Devolución completada
  it('7. Distinguishes completed refund status', async () => {
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

    expect(screen.getByText('Devolución Completada')).toBeInTheDocument();
  });

  // Scenario 8: Sesión expirada (401)
  it('8. Handles customer session expiration: prompts login redirect', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockRejectedValue(
      new PaymentServiceError('Sesión expirada', 'SESSION_EXPIRED', 401)
    );

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-session-expired')).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /iniciar sesión/i })).toHaveAttribute('href', '/login');
  });

  // Scenario 9: Correlación ausente o inválida
  it('9. Handles missing or invalid correlation safely', async () => {
    // No storage and invalid searchParam
    mockSearchParams.set('external_reference', 'invalid-uuid-format');

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-correlation-error')).toBeInTheDocument();
    });

    expect(screen.getByText(/no encontramos los datos del pago/i)).toBeInTheDocument();
  });

  // Scenario 10: Respuesta tardía / timeout de red
  it('10. Handles network timeout / poll cancellation without fabricating a FAILED state', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockRejectedValue(
      new PaymentServiceError('Timeout de verificación', 'POLL_TIMEOUT', 408)
    );

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-unknown-error')).toBeInTheDocument();
    });

    expect(screen.getByText(/no pudimos comprobar el estado de inmediato/i)).toBeInTheDocument();
    // Re-verify it does not say "Pago no aprobado"
    expect(screen.queryByTestId('state-failed')).toBeNull();
  });

  // Scenario 11: Regreso repetido a la misma URL (idempotencia en UI)
  it('11. Repeated return to same URL retrieves confirmed order deterministically without duplicate side-effects', async () => {
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

  // Scenario 12: Pago perteneciente a otro cliente (403 Forbidden)
  it('12. Handles 403 Forbidden safely without disclosing technical details', async () => {
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

  // Scenario 13: Discrepancia entre external_reference y sessionStorage
  it('13. Stops verification and displays security warning on correlation discrepancy', async () => {
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

  // Scenario 14: external_reference de tipo Orders API (pa1_attemptId) sin sessionStorage
  it('14. Safely rejects pa1_attemptId when no local correlation is found without confusing it with paymentId', async () => {
    mockSearchParams.set('external_reference', `pa1_${OTHER_PAYMENT_ID}`);

    const pollSpy = vi.spyOn(checkoutService, 'pollPaymentStatus');

    render(<PaymentReturnView />);

    await waitFor(() => {
      expect(screen.getByTestId('state-correlation-error')).toBeInTheDocument();
    });

    expect(screen.getByText(/no fue posible recuperar la sesión de pago/i)).toBeInTheDocument();
    expect(pollSpy).not.toHaveBeenCalled();
  });

  // Scenario 15: Retorno nominal /failure pero con orden confirmada en backend
  it('15. Ignores nominal /failure path when backend confirms orderCreated=true', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue(baseStatusResponse);

    render(<PaymentReturnView expectedPath="failure" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-order-confirmed')).toBeInTheDocument();
    });

    expect(screen.getByText('#ORD-2026-99999')).toBeInTheDocument();
    expect(screen.queryByTestId('state-failed')).toBeNull();
  });

  // Scenario 16: Retorno nominal /success pero con pago PENDING en backend
  it('16. Ignores nominal /success path when backend reports paymentStatus=PENDING', async () => {
    mockSearchParams.set('external_reference', VALID_PAYMENT_ID);

    vi.spyOn(checkoutService, 'pollPaymentStatus').mockResolvedValue({
      ...baseStatusResponse,
      paymentStatus: 'PENDING',
      orderCreated: false,
      orderNumber: null,
    });

    render(<PaymentReturnView expectedPath="success" />);

    await waitFor(() => {
      expect(screen.getByTestId('state-pending')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('state-order-confirmed')).toBeNull();
  });
});
