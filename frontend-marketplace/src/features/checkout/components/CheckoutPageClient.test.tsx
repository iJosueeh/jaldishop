import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CheckoutPageClient } from './CheckoutPageClient';
import { StorefrontCartProvider } from '@/features/storefront/cart/StorefrontCartProvider';
import { cartService } from '@/features/storefront/cart/services/cartService';
import { customerSessionService } from '@/features/storefront/cart/services/customerSessionService';
import { storeService } from '@/features/storefront/services/storeService';
import { capacityService } from '@/features/storefront/services/capacityService';
import { customerAddressService } from '@/features/customer/services/customerAddressService';
import { checkoutService } from '../services/checkoutService';

vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams('store=toddy'),
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/features/storefront/cart/services/cartService', () => ({
  cartService: { get: vi.fn(), add: vi.fn(), update: vi.fn(), remove: vi.fn(), clear: vi.fn() },
}));

vi.mock('@/features/storefront/cart/services/customerSessionService', () => ({
  customerSessionService: { get: vi.fn(), login: vi.fn(), logout: vi.fn() },
}));

vi.mock('@/features/storefront/services/storeService', () => ({
  storeService: { getStoreBySlugClient: vi.fn(), getPublicStoreSettingsClient: vi.fn() },
}));

vi.mock('@/features/storefront/services/capacityService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/storefront/services/capacityService')>();
  return {
    ...actual,
    capacityService: { getCapacitySlots: vi.fn() },
  };
});

vi.mock('@/features/customer/services/customerAddressService', () => ({
  customerAddressService: {
    getAddresses: vi.fn(),
    createAddress: vi.fn(),
    setDefaultAddress: vi.fn(),
  },
}));

vi.mock('@/features/customer/hooks/useCustomerProfile', () => ({
  useCustomerProfile: vi.fn(() => ({
    data: {
      id: 'user-1',
      email: 'juan.perez@example.com',
      firstName: 'Juan',
      lastName: 'Pérez',
      phone: '987654321',
      status: 'ACTIVE',
      roles: ['CUSTOMER'],
      createdAt: '2026-01-01T00:00:00Z',
    },
  })),
}));

vi.mock('../services/checkoutService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../services/checkoutService')>();
  return {
    ...actual,
    checkoutService: {
      ...actual.checkoutService,
      initiateCheckout: vi.fn(),
      createPaymentPreference: vi.fn(),
    },
  };
});

const mockStore = {
  id: 'store-1',
  name: 'Toddy',
  slug: 'toddy',
  status: 'ACTIVE' as const,
  deliveryEnabled: true,
  pickupEnabled: true,
  deliveryFeeAmount: 5,
};

const mockSettings = {
  cancellationPolicy: 'Flexible',
  refundPolicy: 'Completa',
  requireCustomerDni: true,
  allowOrderNotes: true,
  onlinePaymentsEnabled: true,
  cashOnDeliveryEnabled: false,
  acceptsPosOnDelivery: false,
  minDeliveryAmount: 20,
  estimatedPrepTimeMinutes: 30,
};

const mockCart = {
  id: 'cart-1',
  storeId: 'store-1',
  items: [
    {
      variantId: 'var-1',
      productId: 'prod-1',
      productName: 'Tacos',
      presentationName: 'Combo',
      quantity: 2,
      unitPriceAmount: 15,
      unitPriceCurrency: 'PEN',
      subtotalAmount: 30,
      available: true,
    },
  ],
  totalItems: 2,
  totalAmount: 30,
  currency: 'PEN',
};

const mockAddress = {
  id: 'addr-1',
  alias: 'Casa',
  recipientName: 'Juan Pérez',
  recipientPhone: '987654321',
  department: 'LIMA',
  province: 'LIMA',
  district: 'MIRAFLORES',
  ubigeoCode: '150122',
  streetAddress: 'Av. Larco 123',
  addressLine: 'Av. Larco 123',
  reference: 'Frente al parque',
  isDefault: true,
  latitude: -12.12,
  longitude: -77.03,
};

function renderCheckout() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <StorefrontCartProvider>
        <CheckoutPageClient />
      </StorefrontCartProvider>
    </QueryClientProvider>
  );
}

describe('CheckoutPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: true });
    vi.mocked(storeService.getStoreBySlugClient).mockResolvedValue(mockStore);
    vi.mocked(storeService.getPublicStoreSettingsClient).mockResolvedValue(mockSettings);
    vi.mocked(cartService.get).mockResolvedValue(mockCart);
    vi.mocked(customerAddressService.getAddresses).mockResolvedValue({
      total: 1,
      maxAllowed: 5,
      addresses: [mockAddress],
    });
    vi.mocked(capacityService.getCapacitySlots).mockResolvedValue({
      storeId: 'store-1',
      serviceDate: '2026-11-24',
      slots: [
        {
          storeId: 'store-1',
          serviceDate: '2026-11-24',
          startTime: '14:00:00',
          endTime: '15:00:00',
          effectiveCapacity: 5,
          availableCapacity: 3,
          reservedCapacity: 2,
          committedCapacity: 0,
          occupiedCapacity: 2,
        },
      ],
    });
  });

  it('blocks Mercado Pago checkout when minDeliveryAmount is not met', async () => {
    vi.mocked(cartService.get).mockResolvedValue({
      ...mockCart,
      totalAmount: 10,
      items: [{ ...mockCart.items[0], quantity: 1, unitPriceAmount: 10, subtotalAmount: 10 }],
    });
    renderCheckout();
    expect(await screen.findByText(/monto mínimo para delivery/i)).toBeInTheDocument();
    const payBtn = screen.getByRole('button', { name: /Reservar y pagar con Mercado Pago/i });
    expect(payBtn).toBeDisabled();
  });

  it('requires valid DNI when requireCustomerDni is true and redirects to valid Mercado Pago initPoint', async () => {
    const assignMock = vi.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { assign: assignMock, pathname: '/checkout', href: '' },
    });

    vi.mocked(checkoutService.initiateCheckout).mockResolvedValue({
      reservationId: 'res-123',
      reservationExpiresAt: new Date(Date.now() + 600000).toISOString(),
      storeId: 'store-1',
      storeName: 'Toddy',
      userId: 'user-1',
      fulfillmentType: 'DELIVERY',
      serviceDate: '2026-11-24',
      startTime: '14:00:00',
      endTime: '15:00:00',
      customerInfo: { customerName: 'Juan Pérez', customerPhone: '987654321', customerEmail: '' },
      pricing: {
        currency: 'PEN',
        productsSubtotalAmount: 30,
        discountAmount: 0,
        deliveryFeeAmount: 5,
        includedTaxAmount: 5.34,
        totalAmount: 35,
      },
      createdAt: new Date().toISOString(),
    });

    vi.mocked(checkoutService.createPaymentPreference).mockResolvedValue({
      paymentId: 'pay-123',
      preferenceId: 'pref-123',
      initPoint: 'https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-123',
      status: 'PENDING',
    });

    renderCheckout();

    // Select capacity slot
    const slotBtn = await screen.findByRole('button', { name: /14:00 - 15:00/i });
    fireEvent.click(slotBtn);

    // Pay button should be disabled until DNI is valid
    const payBtn = screen.getByRole('button', { name: /Reservar y pagar con Mercado Pago/i });
    expect(payBtn).toBeDisabled();

    // Enter valid 8-digit DNI
    const dniInput = screen.getByLabelText(/DNI \/ RUC/i);
    fireEvent.change(dniInput, { target: { value: '73026305' } });
    expect(payBtn).toBeEnabled();

    fireEvent.click(payBtn);

    await waitFor(() => expect(checkoutService.initiateCheckout).toHaveBeenCalledTimes(1));
    await waitFor(() =>
      expect(checkoutService.createPaymentPreference).toHaveBeenCalledWith(
        expect.objectContaining({ capacityReservationId: 'res-123', fulfillmentType: 'DELIVERY' })
      )
    );
    await waitFor(() =>
      expect(assignMock).toHaveBeenCalledWith(
        'https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-123'
      )
    );
  });

  it('autofills customer email from account profile and shows indicator', async () => {
    renderCheckout();
    const emailInput = (await screen.findByLabelText(/Correo electrónico/i)) as HTMLInputElement;
    await waitFor(() => expect(emailInput.value).toBe('juan.perez@example.com'));
    expect(screen.getByText('Completado desde tu cuenta')).toBeInTheDocument();
  });
});
