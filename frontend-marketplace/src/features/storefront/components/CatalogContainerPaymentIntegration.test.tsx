import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render as testingRender, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CatalogContainer, CartItem } from './CatalogContainer';
import { PublicStore } from '../types/storefront.types';
import { capacityService } from '../services/capacityService';
import { checkoutService, PaymentServiceError } from '@/features/checkout/services/checkoutService';
import { toast } from 'sonner';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock('@/features/customer/hooks/useCustomerAddresses', () => ({
  useCustomerAddresses: vi.fn().mockReturnValue({
    addresses: [],
    total: 0,
    defaultAddress: null,
    canAddAddress: true,
    isLoading: false,
    createAddress: vi.fn(),
  }),
}));

const mockStore: PublicStore = {
  id: '018f2d5a-1b4e-7b7e-8c3e-4b6e5d7a8b9c',
  name: 'Panadería Central',
  slug: 'panaderia-central',
  status: 'ACTIVE',
  deliveryEnabled: true,
  pickupEnabled: true,
  deliveryFeeAmount: 5,
};

const mockProducts = [
  {
    id: 'prod-1',
    name: 'Pastel de Choclo',
    price: 25,
    category: 'platos',
    imageBg: 'bg-amber-100',
    iconText: '🌽',
  },
];

let mockCartState = {
  items: [] as CartItem[],
  count: 0,
  store: null as { id: string; name: string; slug: string } | null,
  authenticated: true,
  changeQuantity: vi.fn(),
  selectStore: vi.fn(),
  pending: false,
  loading: false,
  subtotal: 25,
  currency: 'PEN',
};

vi.mock('../cart/StorefrontCartProvider', () => ({
  useStorefrontCart: () => mockCartState,
  StorefrontCartProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock('../services/capacityService', () => ({
  getBusinessTodayDate: vi.fn().mockReturnValue('2026-11-13'),
  capacityService: {
    getCapacitySlots: vi.fn(),
  },
  mapBackendSlotsToPickerSlots: vi.fn((slots) =>
    slots.map((s: { slotId: string; startTime: string; endTime: string; serviceDate: string; availableCapacity: number }) => ({
      id: s.slotId,
      timeRange: `${s.startTime.slice(0, 5)} - ${s.endTime.slice(0, 5)}`,
      totalCapacity: 10,
      reservedCount: 10 - s.availableCapacity,
      status: s.availableCapacity <= 0 ? 'sold_out' : 'available',
      startTime: s.startTime,
      endTime: s.endTime,
      serviceDate: s.serviceDate,
      availableCapacity: s.availableCapacity,
    }))
  ),
}));

vi.mock('@/features/checkout/services/checkoutService', () => ({
  checkoutService: {
    initiateCheckout: vi.fn(),
    createPaymentPreference: vi.fn(),
  },
  PaymentServiceError: class extends Error {
    code?: string;
    status?: number;
    constructor(msg: string, code?: string, status?: number) {
      super(msg);
      this.code = code;
      this.status = status;
    }
  },
}));

function renderWithClient(ui: React.ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return testingRender(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
}

describe('CatalogContainer Payment & Mercado Pago Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCartState = {
      items: [{ id: 'item-1', product: mockProducts[0], quantity: 1 }],
      count: 1,
      store: { id: mockStore.id, name: mockStore.name, slug: mockStore.slug },
      authenticated: true,
      changeQuantity: vi.fn(),
      selectStore: vi.fn(),
      pending: false,
      loading: false,
      subtotal: 25,
      currency: 'PEN',
    };

    vi.mocked(capacityService.getCapacitySlots).mockResolvedValue({
      storeId: mockStore.id,
      serviceDate: '2026-11-13',
      slots: [
        {
          slotId: '018f2d5a-slot-001',
          startTime: '10:00:00',
          endTime: '11:00:00',
          serviceDate: '2026-11-13',
          totalCapacity: 10,
          reservedCapacity: 2,
          availableCapacity: 8,
          status: 'AVAILABLE',
        },
      ],
    });
  });

  it('initiates Mercado Pago payment preference and redirects to initPoint', async () => {
    const assignMock = vi.fn();
    delete (window as unknown as { location: unknown }).location;
    window.location = { assign: assignMock, pathname: '/tienda/panaderia-central' } as unknown as Location;

    vi.mocked(checkoutService.initiateCheckout).mockResolvedValueOnce({
      reservationId: 'res-uuid-12345',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '10:00:00',
      endTime: '11:00:00',
      customerInfo: {
        customerName: 'Rosa Flores',
        customerPhone: '987654321',
        customerEmail: 'rosa@example.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 25,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 25,
      },
      createdAt: new Date().toISOString(),
    });

    vi.mocked(checkoutService.createPaymentPreference).mockResolvedValueOnce({
      paymentId: 'pay-uuid-999',
      preferenceId: 'pref_mp_12345',
      initPoint: 'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref_mp_12345',
      status: 'PENDING',
    });

    renderWithClient(
      <CatalogContainer
        store={mockStore}
        storeName={mockStore.name}
        products={mockProducts}
        categories={[{ id: 'platos', name: 'Platos', slug: 'platos', itemCount: 1 }]}
      />
    );

    // Wait for slot to load and click it
    const slotElements = await screen.findAllByText('10:00 - 11:00');
    expect(slotElements.length).toBeGreaterThanOrEqual(1);
    fireEvent.click(slotElements[0]);

    // Switch to Retiro en Tienda
    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    // Wait for checkout button to be enabled
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Continuar al pago/i })).toBeInTheDocument();
    });

    // Click checkout
    const checkoutBtn = screen.getByRole('button', { name: /Continuar al pago/i });
    fireEvent.click(checkoutBtn);

    // Modal opens
    await waitFor(() => {
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });

    // Click Pagar con Mercado Pago
    const payBtn = screen.getByText('Pagar con Mercado Pago');
    fireEvent.click(payBtn);

    // Verify preference was created with reservation ID
    await waitFor(() => {
      expect(checkoutService.createPaymentPreference).toHaveBeenCalledWith(
        expect.objectContaining({
          capacityReservationId: 'res-uuid-12345',
          fulfillmentType: 'PICKUP',
        })
      );
      expect(assignMock).toHaveBeenCalledWith(
        'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref_mp_12345'
      );
      expect(toast.success).toHaveBeenCalledWith(
        'Redirigiendo a la pasarela segura de Mercado Pago...'
      );
    });
  });

  it('handles reservation expiration (409 Conflict) and closes modal', async () => {
    vi.mocked(checkoutService.initiateCheckout).mockResolvedValueOnce({
      reservationId: 'res-expired-99',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '10:00:00',
      endTime: '11:00:00',
      customerInfo: {
        customerName: 'Rosa Flores',
        customerPhone: '987654321',
        customerEmail: 'rosa@example.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 25,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 25,
      },
      createdAt: new Date().toISOString(),
    });

    const conflictErr = new PaymentServiceError(
      'La reserva de capacidad se encuentra expirada.',
      'RESERVATION_EXPIRED',
      409
    );
    vi.mocked(checkoutService.createPaymentPreference).mockRejectedValueOnce(conflictErr);

    renderWithClient(
      <CatalogContainer
        store={mockStore}
        storeName={mockStore.name}
        products={mockProducts}
        categories={[{ id: 'platos', name: 'Platos', slug: 'platos', itemCount: 1 }]}
      />
    );

    const slotElements = await screen.findAllByText('10:00 - 11:00');
    fireEvent.click(slotElements[0]);

    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Continuar al pago/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Continuar al pago/i }));

    await waitFor(() => {
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Pagar con Mercado Pago'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'La reserva de capacidad se encuentra expirada.'
      );
      // Modal should be closed
      expect(screen.queryByText('Pagar con Mercado Pago')).not.toBeInTheDocument();
    });
  });

  it('handles rate limiting (429) gracefully without closing modal', async () => {
    vi.mocked(checkoutService.initiateCheckout).mockResolvedValueOnce({
      reservationId: 'res-rate-limit',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '10:00:00',
      endTime: '11:00:00',
      customerInfo: {
        customerName: 'Rosa Flores',
        customerPhone: '987654321',
        customerEmail: 'rosa@example.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 25,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 25,
      },
      createdAt: new Date().toISOString(),
    });

    const rateLimitErr = new PaymentServiceError(
      'Demasiadas solicitudes de pago. Intenta más tarde.',
      'RATE_LIMIT_EXCEEDED',
      429
    );
    vi.mocked(checkoutService.createPaymentPreference).mockRejectedValueOnce(rateLimitErr);

    renderWithClient(
      <CatalogContainer
        store={mockStore}
        storeName={mockStore.name}
        products={mockProducts}
        categories={[{ id: 'platos', name: 'Platos', slug: 'platos', itemCount: 1 }]}
      />
    );

    const slotElements = await screen.findAllByText('10:00 - 11:00');
    fireEvent.click(slotElements[0]);

    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Continuar al pago/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Continuar al pago/i }));

    await waitFor(() => {
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Pagar con Mercado Pago'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Demasiadas solicitudes de pago. Por favor espera un momento antes de intentar nuevamente.'
      );
      // Modal remains open so user can retry later
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });
  });

  it('handles PAYMENT_IN_PROGRESS (409 Conflict) without closing modal or discarding reservation', async () => {
    vi.mocked(checkoutService.initiateCheckout).mockResolvedValueOnce({
      reservationId: 'res-in-progress-409',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '10:00:00',
      endTime: '11:00:00',
      customerInfo: {
        customerName: 'Rosa Flores',
        customerPhone: '987654321',
        customerEmail: 'rosa@example.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 25,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 25,
      },
      createdAt: new Date().toISOString(),
    });

    const inProgressErr = new PaymentServiceError(
      'Existe un intento de pago en proceso para esta reserva.',
      'PAYMENT_IN_PROGRESS',
      409
    );
    vi.mocked(checkoutService.createPaymentPreference).mockRejectedValueOnce(inProgressErr);

    renderWithClient(
      <CatalogContainer
        store={mockStore}
        storeName={mockStore.name}
        products={mockProducts}
        categories={[{ id: 'platos', name: 'Platos', slug: 'platos', itemCount: 1 }]}
      />
    );

    const slotElements = await screen.findAllByText('10:00 - 11:00');
    fireEvent.click(slotElements[0]);

    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Continuar al pago/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Continuar al pago/i }));

    await waitFor(() => {
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Pagar con Mercado Pago'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Existe un intento de pago en proceso para esta reserva.'
      );
      // Regression: Modal MUST remain open and reservation kept
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });
  });

  it('handles generic 409 Conflict without closing modal or misclassifying as expired reservation', async () => {
    vi.mocked(checkoutService.initiateCheckout).mockResolvedValueOnce({
      reservationId: 'res-generic-409',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '10:00:00',
      endTime: '11:00:00',
      customerInfo: {
        customerName: 'Rosa Flores',
        customerPhone: '987654321',
        customerEmail: 'rosa@example.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 25,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 25,
      },
      createdAt: new Date().toISOString(),
    });

    const otherConflictErr = new PaymentServiceError(
      'Conflicto de concurrencia al procesar el pago.',
      'CONCURRENCY_CONFLICT',
      409
    );
    vi.mocked(checkoutService.createPaymentPreference).mockRejectedValueOnce(otherConflictErr);

    renderWithClient(
      <CatalogContainer
        store={mockStore}
        storeName={mockStore.name}
        products={mockProducts}
        categories={[{ id: 'platos', name: 'Platos', slug: 'platos', itemCount: 1 }]}
      />
    );

    const slotElements = await screen.findAllByText('10:00 - 11:00');
    fireEvent.click(slotElements[0]);

    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Continuar al pago/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Continuar al pago/i }));

    await waitFor(() => {
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Pagar con Mercado Pago'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        'Conflicto de concurrencia al procesar el pago.'
      );
      // Regression: Modal MUST remain open and reservation kept
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();
    });
  });
});
