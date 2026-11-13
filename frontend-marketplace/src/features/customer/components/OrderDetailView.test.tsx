import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OrderDetailView } from './OrderDetailView';
import { useCustomerOrderDetail } from '../hooks/useCustomerOrderDetail';
import { CustomerOrderDetail } from '../types/customer.types';

vi.mock('../hooks/useCustomerOrderDetail', () => ({
  useCustomerOrderDetail: vi.fn(),
}));

const baseMockOrder: CustomerOrderDetail = {
  id: 'order-uuid-1',
  orderNumber: 'ORD-20261113-0001',
  store: {
    id: 'store-1',
    name: 'Bodega San Martín',
    slug: 'bodega-san-martin',
    contactPhone: '999888777',
    whatsappNumber: '999888777',
    address: 'Av. Brasil 456',
    addressReference: 'Frente al parque',
    logoUrl: null,
  },
  status: 'CONFIRMED',
  deliveryMode: 'DELIVERY',
  serviceDate: '2026-11-14',
  serviceStartTime: '10:00:00',
  serviceEndTime: '11:00:00',
  customerName: 'Juan Pérez',
  customerPhone: '987654321',
  customerEmail: 'juan@example.com',
  deliveryAddress: 'Calle Las Flores 123',
  deliveryReference: 'Dpto 402',
  deliveryLatitude: -12.05,
  deliveryLongitude: -77.03,
  currency: 'PEN',
  productsSubtotalAmount: 40.0,
  discountAmount: 0,
  discountCode: null,
  deliveryFeeAmount: 5.0,
  includedTaxAmount: 6.86,
  totalAmount: 45.0,
  confirmedAt: '2026-11-13T10:00:00Z',
  updatedAt: '2026-11-13T10:00:00Z',
  items: [
    {
      id: 'item-1',
      variantId: null,
      productName: 'Pan Ciabatta',
      variantName: null,
      quantity: 10,
      unitPriceAmount: 4.0,
      subtotalAmount: 40.0,
    },
  ],
  statusHistory: [
    {
      status: 'CONFIRMED',
      reason: 'Pago acreditado y orden confirmada',
      changedAt: '2026-11-13T10:00:00Z',
    },
  ],
};

describe('OrderDetailView Component', () => {
  const mockCancelOrder = vi.fn();
  const mockResetCancelError = vi.fn();
  const mockReload = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('pedido CONFIRMED: shows "Cancelar pedido" button and opens confirmation modal', () => {
    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: baseMockOrder,
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: null,
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    const cancelBtn = screen.getByRole('button', { name: /cancelar pedido/i });
    expect(cancelBtn).toBeInTheDocument();

    fireEvent.click(cancelBtn);

    expect(mockResetCancelError).toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('¿Deseas cancelar este pedido?')).toBeInTheDocument();
  });

  it('pedido IN_PREPARATION: does NOT show "Cancelar pedido" action', () => {
    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: {
        ...baseMockOrder,
        status: 'IN_PREPARATION',
      },
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: null,
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    expect(screen.queryByRole('button', { name: /cancelar pedido/i })).not.toBeInTheDocument();
    expect(screen.getAllByText('En preparación').length).toBeGreaterThan(0);
  });

  it('pedido READY, OUT_FOR_DELIVERY, COMPLETED: does NOT show "Cancelar pedido" action', () => {
    for (const status of ['READY', 'OUT_FOR_DELIVERY', 'COMPLETED'] as const) {
      vi.mocked(useCustomerOrderDetail).mockReturnValue({
        order: {
          ...baseMockOrder,
          status,
        },
        isLoading: false,
        isFetching: false,
        error: null,
        reload: mockReload,
        cancelOrder: mockCancelOrder,
        isCancelling: false,
        cancelError: null,
        resetCancelError: mockResetCancelError,
      });

      const { unmount } = render(<OrderDetailView orderNumber="ORD-20261113-0001" />);
      expect(screen.queryByRole('button', { name: /cancelar pedido/i })).not.toBeInTheDocument();
      unmount();
    }
  });

  it('pedido CANCELLED: does NOT offer cancellation and shows cancelled stepper details', () => {
    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: {
        ...baseMockOrder,
        status: 'CANCELLED',
        statusHistory: [
          ...baseMockOrder.statusHistory,
          {
            status: 'CANCELLED',
            reason: 'Cancelado por solicitud del cliente.',
            changedAt: '2026-11-13T10:15:00Z',
          },
        ],
      },
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: null,
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    expect(screen.queryByRole('button', { name: /cancelar pedido/i })).not.toBeInTheDocument();
    expect(screen.getByText('Pedido cancelado')).toBeInTheDocument();
    expect(screen.getByText('Cancelado por solicitud del cliente.')).toBeInTheDocument();
  });

  it('cancelación exitosa: calls cancelOrder with optional reason and closes modal', async () => {
    mockCancelOrder.mockResolvedValue({
      ...baseMockOrder,
      status: 'CANCELLED',
    });

    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: baseMockOrder,
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: null,
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    // Open modal
    fireEvent.click(screen.getByRole('button', { name: /cancelar pedido/i }));

    // Type reason
    const textarea = screen.getByLabelText(/motivo de la cancelación/i);
    fireEvent.change(textarea, { target: { value: 'Pedido por duplicado' } });

    // Submit
    fireEvent.click(screen.getByRole('button', { name: /confirmar cancelación/i }));

    await waitFor(() => {
      expect(mockCancelOrder).toHaveBeenCalledWith('Pedido por duplicado');
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('error de regla de negocio: shows backend message in modal without closing', async () => {
    mockCancelOrder.mockRejectedValue(
      new Error('El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.')
    );

    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: baseMockOrder,
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: 'El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.',
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    // Open modal
    fireEvent.click(screen.getByRole('button', { name: /cancelar pedido/i }));

    // Modal renders with error banner
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(
      screen.getByText('El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.')
    ).toBeInTheDocument();
  });

  it('error de red o pedido ajeno: renders error state with retry and navigation', () => {
    vi.mocked(useCustomerOrderDetail).mockReturnValue({
      order: null,
      isLoading: false,
      isFetching: false,
      error: 'No tienes permisos para visualizar un pedido ajeno.',
      reload: mockReload,
      cancelOrder: mockCancelOrder,
      isCancelling: false,
      cancelError: null,
      resetCancelError: mockResetCancelError,
    });

    render(<OrderDetailView orderNumber="ORD-20261113-0001" />);

    expect(screen.getByText('No pudimos cargar este pedido')).toBeInTheDocument();
    expect(
      screen.getByText('No tienes permisos para visualizar un pedido ajeno.')
    ).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /reintentar/i });
    fireEvent.click(retryBtn);
    expect(mockReload).toHaveBeenCalledTimes(1);
  });
});
