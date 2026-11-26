import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { PaymentsView } from './PaymentsView';

const mockUseCustomerPaymentsPaged = vi.fn();

vi.mock('../hooks/useCustomerPaymentsPaged', () => ({
  useCustomerPaymentsPaged: (params: unknown) => mockUseCustomerPaymentsPaged(params),
}));

vi.mock('../hooks/useDebouncedValue', () => ({
  useDebouncedValue: <T,>(value: T) => value,
}));

beforeEach(() => {
  mockUseCustomerPaymentsPaged.mockReset();
});

describe('PaymentsView Component', () => {
  it('renders empty state when customer has no payments', () => {
    mockUseCustomerPaymentsPaged.mockReturnValue({
      items: [],
      page: 0,
      size: 8,
      totalElements: 0,
      totalPages: 0,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<PaymentsView />);

    expect(screen.getByRole('heading', { level: 1, name: 'Mis Pagos' })).toBeInTheDocument();
    expect(screen.getByText('Aún no registras pagos')).toBeInTheDocument();
  });

  it('renders backend-enriched storeName, hides NOT_REQUIRED refund, and sends filter/search params', () => {
    mockUseCustomerPaymentsPaged.mockReturnValue({
      items: [
        {
          paymentId: 'pay-1',
          orderNumber: 'ORD-20261125-0001',
          orderStatus: 'CONFIRMED',
          paymentStatus: 'APPROVED',
          refundStatus: 'NOT_REQUIRED',
          amount: 120.5,
          currency: 'PEN',
          createdAt: '2026-11-25T10:00:00Z',
          approvedAt: '2026-11-25T10:01:00Z',
          refundedAt: null,
          storeName: 'Pastelería Dulce Deleite',
          totalItems: 2,
        },
        {
          paymentId: 'pay-2',
          orderNumber: null,
          orderStatus: null,
          paymentStatus: 'APPROVED',
          refundStatus: 'PENDING',
          amount: 22.9,
          currency: 'PEN',
          createdAt: '2026-11-24T10:00:00Z',
          approvedAt: '2026-11-24T10:01:00Z',
          refundedAt: null,
          storeName: null,
          totalItems: null,
        },
      ],
      page: 0,
      size: 8,
      totalElements: 2,
      totalPages: 1,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<PaymentsView />);

    expect(screen.getAllByText('Pedido #ORD-20261125-0001').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Pastelería Dulce Deleite').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Sin orden generada · Reembolso en trámite').length).toBeGreaterThanOrEqual(1);
    expect(screen.queryByText('NOT_REQUIRED')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Con pedido' }));
    expect(mockUseCustomerPaymentsPaged).toHaveBeenLastCalledWith({
      page: 0,
      size: 8,
      filter: 'WITH_ORDER',
      q: '',
    });

    fireEvent.change(screen.getByLabelText('Buscar pagos'), {
      target: { value: 'Dulce' },
    });
    expect(mockUseCustomerPaymentsPaged).toHaveBeenLastCalledWith({
      page: 0,
      size: 8,
      filter: 'WITH_ORDER',
      q: 'Dulce',
    });
  });

  it('requests next page from backend when clicking Siguiente', () => {
    mockUseCustomerPaymentsPaged.mockReturnValue({
      items: [
        {
          paymentId: 'pay-1',
          orderNumber: 'JAL-2026-000001',
          orderStatus: 'CONFIRMED',
          paymentStatus: 'APPROVED',
          refundStatus: 'NOT_REQUIRED',
          amount: 25,
          currency: 'PEN',
          createdAt: '2026-11-25T10:00:00Z',
          approvedAt: '2026-11-25T10:01:00Z',
          refundedAt: null,
          storeName: 'Pastelería Dulce Deleite',
          totalItems: 1,
        },
      ],
      page: 0,
      size: 8,
      totalElements: 12,
      totalPages: 2,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    render(<PaymentsView />);

    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Siguiente/i }));

    expect(mockUseCustomerPaymentsPaged).toHaveBeenLastCalledWith({
      page: 1,
      size: 8,
      filter: 'ALL',
      q: '',
    });
  });
});
