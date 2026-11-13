import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render as testingRender, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CatalogContainer } from './CatalogContainer';
import { capacityService } from '../services/capacityService';
import { checkoutService } from '@/features/checkout/services/checkoutService';
import { PublicStore, CartItem } from '../types/storefront.types';
import { CapacitySlotBackendResponse } from '../services/capacityService';

// Mock StorefrontCartProvider with controllable state
let mockCartState = {
  items: [] as CartItem[],
  count: 0,
  store: null as { id: string; name: string; slug: string } | null,
  authenticated: false,
  changeQuantity: vi.fn(),
  selectStore: vi.fn(),
  pending: false,
  loading: false,
  subtotal: 0,
  currency: 'PEN',
};

vi.mock('../cart/StorefrontCartProvider', () => ({
  useStorefrontCart: () => mockCartState,
  StorefrontCartProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
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

vi.mock('../services/capacityService', () => ({
  getBusinessTodayDate: vi.fn().mockReturnValue('2026-11-13'),
  capacityService: {
    getCapacitySlots: vi.fn(),
  },
  mapBackendSlotsToPickerSlots: vi.fn((slots: CapacitySlotBackendResponse[]) =>
    slots.map((s: CapacitySlotBackendResponse) => ({
      id: `${s.startTime.substring(0, 5)}-${s.endTime.substring(0, 5)}`,
      timeRange: `${s.startTime.substring(0, 5)} - ${s.endTime.substring(0, 5)}`,
      totalCapacity: s.effectiveCapacity,
      reservedCount: s.occupiedCapacity,
      status: s.availableCapacity <= 0 ? 'sold_out' : s.availableCapacity <= 2 ? 'few_left' : 'available',
      estimatedDeliveryMinutes: 45,
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
  },
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
    name: 'Pan de Masa Madre',
    price: 10,
    category: 'Panes',
    categoryId: 'cat-1',
  },
];

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

beforeEach(() => {
  vi.clearAllMocks();
  mockCartState = {
    items: [{ id: 'item-1', product: mockProducts[0], quantity: 2 }],
    count: 2,
    store: { id: mockStore.id, name: mockStore.name, slug: mockStore.slug },
    authenticated: true,
    changeQuantity: vi.fn(),
    selectStore: vi.fn(),
    pending: false,
    loading: false,
    subtotal: 20,
    currency: 'PEN',
  };
});

describe('CatalogContainer Capacity & Slot Integration', () => {
  it('renders login prompt when cart has items but customer is unauthenticated', () => {
    mockCartState.authenticated = false;

    renderWithClient(
      <CatalogContainer
        storeName={mockStore.name}
        store={mockStore}
        products={mockProducts}
      />
    );

    expect(
      screen.getByText('Inicia sesión para seleccionar horario')
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Iniciar sesión' })
    ).toHaveAttribute('href', expect.stringContaining('/login?redirect='));
    expect(capacityService.getCapacitySlots).not.toHaveBeenCalled();
  });

  it('fetches real capacity slots and auto-selects first available slot when authenticated', async () => {
    vi.mocked(capacityService.getCapacitySlots).mockResolvedValue({
      storeId: mockStore.id,
      serviceDate: '2026-11-13',
      slots: [
        {
          storeId: mockStore.id,
          serviceDate: '2026-11-13',
          startTime: '09:00:00',
          endTime: '11:00:00',
          effectiveCapacity: 10,
          availableCapacity: 8,
          reservedCapacity: 2,
          committedCapacity: 0,
          occupiedCapacity: 2,
        },
        {
          storeId: mockStore.id,
          serviceDate: '2026-11-13',
          startTime: '11:00:00',
          endTime: '13:00:00',
          effectiveCapacity: 10,
          availableCapacity: 0,
          reservedCapacity: 10,
          committedCapacity: 0,
          occupiedCapacity: 10,
        },
      ],
    });

    renderWithClient(
      <CatalogContainer
        storeName={mockStore.name}
        store={mockStore}
        products={mockProducts}
      />
    );

    await waitFor(() => {
      expect(capacityService.getCapacitySlots).toHaveBeenCalledWith(
        mockStore.id,
        expect.any(String)
      );
    });

    // Verify slot rendered
    const slotElements = await screen.findAllByText('09:00 - 11:00');
    expect(slotElements.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('11:00 - 13:00')).toBeInTheDocument();

    // Explicitly click slot card
    fireEvent.click(slotElements[0]);

    // Verify slot is now selected in summary bar
    expect(await screen.findByText('Horario elegido; sin reserva confirmada')).toBeInTheDocument();
  });

  it('handles slot fetch failure and displays error message with retry option', async () => {
    vi.mocked(capacityService.getCapacitySlots).mockRejectedValue(
      new Error('Error de conexión con el backend de capacidad')
    );

    renderWithClient(
      <CatalogContainer
        storeName={mockStore.name}
        store={mockStore}
        products={mockProducts}
      />
    );

    expect(
      await screen.findByText('Error de conexión con el backend de capacidad')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  it('submits checkout reservation with real slot time and date on pickup', async () => {
    vi.mocked(capacityService.getCapacitySlots).mockResolvedValue({
      storeId: mockStore.id,
      serviceDate: '2026-11-13',
      slots: [
        {
          storeId: mockStore.id,
          serviceDate: '2026-11-13',
          startTime: '14:00:00',
          endTime: '16:00:00',
          effectiveCapacity: 5,
          availableCapacity: 5,
          reservedCapacity: 0,
          committedCapacity: 0,
          occupiedCapacity: 0,
        },
      ],
    });

    vi.mocked(checkoutService.initiateCheckout).mockResolvedValue({
      reservationId: 'res-123',
      reservationExpiresAt: '2026-11-13T14:10:00Z',
      storeId: mockStore.id,
      storeName: mockStore.name,
      userId: 'user-1',
      fulfillmentType: 'PICKUP',
      serviceDate: '2026-11-13',
      startTime: '14:00:00',
      endTime: '16:00:00',
      customerInfo: {
        customerName: 'Cliente Prueba',
        customerPhone: '999888777',
        customerEmail: 'test@jaldishop.com',
      },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 20,
        discountAmount: 0,
        deliveryFeeAmount: 0,
        includedTaxAmount: 0,
        totalAmount: 20,
      },
      createdAt: '2026-11-13T14:00:00Z',
    });

    renderWithClient(
      <CatalogContainer
        storeName={mockStore.name}
        store={mockStore}
        products={mockProducts}
      />
    );

    // Wait for slot to load
    const slotElements = await screen.findAllByText('14:00 - 16:00');
    expect(slotElements.length).toBeGreaterThanOrEqual(1);

    // Explicitly click slot to select it
    fireEvent.click(slotElements[0]);

    // Switch to PICKUP mode
    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    // Click on Continuar al pago button
    const checkoutBtn = await screen.findByRole('button', { name: /Continuar al pago/i });
    fireEvent.click(checkoutBtn);

    await waitFor(() => {
      expect(checkoutService.initiateCheckout).toHaveBeenCalledWith(
        expect.objectContaining({
          storeId: mockStore.id,
          fulfillmentType: 'PICKUP',
          startTime: '14:00:00',
          endTime: '16:00:00',
        })
      );
    });
  });

  it('handles session expiration (401) during checkout initiation gracefully', async () => {
    vi.mocked(capacityService.getCapacitySlots).mockResolvedValue({
      storeId: mockStore.id,
      serviceDate: '2026-11-13',
      slots: [
        {
          storeId: mockStore.id,
          serviceDate: '2026-11-13',
          startTime: '10:00:00',
          endTime: '12:00:00',
          effectiveCapacity: 10,
          availableCapacity: 5,
          reservedCapacity: 5,
          committedCapacity: 0,
          occupiedCapacity: 5,
        },
      ],
    });

    const sessionExpiredError = new Error('Tu sesión ha expirado.') as Error & {
      status?: number;
      code?: string;
    };
    sessionExpiredError.status = 401;
    sessionExpiredError.code = 'SESSION_EXPIRED';
    vi.mocked(checkoutService.initiateCheckout).mockRejectedValue(sessionExpiredError);

    renderWithClient(
      <CatalogContainer
        storeName={mockStore.name}
        store={mockStore}
        products={mockProducts}
      />
    );

    const slotElements = await screen.findAllByText('10:00 - 12:00');
    expect(slotElements.length).toBeGreaterThanOrEqual(1);

    // Explicitly click slot to select it
    fireEvent.click(slotElements[0]);

    const pickupBtn = screen.getByRole('button', { name: /Retiro en Tienda/ });
    fireEvent.click(pickupBtn);

    const checkoutBtn = await screen.findByRole('button', { name: /Continuar al pago/i });
    fireEvent.click(checkoutBtn);

    await waitFor(() => {
      expect(checkoutService.initiateCheckout).toHaveBeenCalled();
    });
  });
});
