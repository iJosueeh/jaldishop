import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DeliveryAddressSelector } from './DeliveryAddressSelector';
import { useCustomerAddresses } from '@/features/customer/hooks/useCustomerAddresses';
import { PublicStore } from '@/features/storefront/types/storefront.types';

vi.mock('@/features/customer/hooks/useCustomerAddresses', () => ({
  useCustomerAddresses: vi.fn(),
}));

const mockStore: PublicStore = {
  id: 'store-1',
  name: 'Panadería Don Pepe',
  slug: 'panaderia-don-pepe',
  description: 'Panadería tradicional',
  status: 'ACTIVE',
  deliveryEnabled: true,
  pickupEnabled: true,
  deliveryFeeAmount: 5.0,
  taxRate: 0.18,
  currency: 'PEN',
  phone: '987654321',
  whatsappNumber: '987654321',
  address: 'Av. Larco 100',
  addressReference: null,
  latitude: -12.12,
  longitude: -77.02,
  logoUrl: null,
  bannerUrl: null,
};

const mockAddresses = [
  {
    id: 'addr-1',
    alias: 'Casa',
    recipientName: 'Carlos Benítez',
    recipientPhone: '987654321',
    department: 'LIMA',
    province: 'LIMA',
    district: 'MIRAFLORES',
    ubigeoCode: '150122',
    addressLine: 'Av. Larco 1234',
    reference: 'Frente al parque',
    latitude: null,
    longitude: null,
    isDefault: true,
    createdAt: '2026-10-26T10:00:00Z',
    updatedAt: '2026-10-26T10:00:00Z',
  },
  {
    id: 'addr-2',
    alias: 'Oficina',
    recipientName: 'Carlos Benítez',
    recipientPhone: '987654321',
    department: 'LIMA',
    province: 'LIMA',
    district: 'SAN ISIDRO',
    ubigeoCode: '150131',
    addressLine: 'Av. Rivera Navarrete 450',
    reference: null,
    latitude: null,
    longitude: null,
    isDefault: false,
    createdAt: '2026-10-26T10:00:00Z',
    updatedAt: '2026-10-26T10:00:00Z',
  },
];

describe('DeliveryAddressSelector Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders saved addresses and preselects the default address', async () => {
    const handleChange = vi.fn();

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: mockAddresses,
      total: 2,
      defaultAddress: mockAddresses[0],
      canAddAddress: true,
      isLoading: false,
      isFetching: false,
      error: null,
      createAddress: vi.fn(),
      isCreating: false,
      updateAddress: vi.fn(),
      isUpdating: false,
      setDefaultAddress: vi.fn(),
      isSettingDefault: false,
      deleteAddress: vi.fn(),
      isDeleting: false,
      reload: vi.fn(),
    });

    render(
      <DeliveryAddressSelector
        store={mockStore}
        value={null}
        onChange={handleChange}
      />
    );

    expect(screen.getByText('Casa')).toBeInTheDocument();
    expect(screen.getByText('Oficina')).toBeInTheDocument();
    expect(screen.getByText('Predeterminada')).toBeInTheDocument();

    await waitFor(() => {
      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          mode: 'saved',
          selectedAddressId: 'addr-1',
          recipientName: 'Carlos Benítez',
          addressLine: 'Av. Larco 1234',
        }),
        true
      );
    });
  });

  it('allows switching to another saved address', async () => {
    const handleChange = vi.fn();

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: mockAddresses,
      total: 2,
      defaultAddress: mockAddresses[0],
      canAddAddress: true,
      isLoading: false,
      isFetching: false,
      error: null,
      createAddress: vi.fn(),
      isCreating: false,
      updateAddress: vi.fn(),
      isUpdating: false,
      setDefaultAddress: vi.fn(),
      isSettingDefault: false,
      deleteAddress: vi.fn(),
      isDeleting: false,
      reload: vi.fn(),
    });

    render(
      <DeliveryAddressSelector
        store={mockStore}
        value={null}
        onChange={handleChange}
      />
    );

    const oficinaRadio = screen.getByRole('radio', { name: /oficina/i });
    fireEvent.click(oficinaRadio);

    await waitFor(() => {
      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          selectedAddressId: 'addr-2',
          addressLine: 'Av. Rivera Navarrete 450',
        }),
        true
      );
    });
  });

  it('allows switching to manual mode and validates required fields', async () => {
    const handleChange = vi.fn();

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: mockAddresses,
      total: 2,
      defaultAddress: mockAddresses[0],
      canAddAddress: true,
      isLoading: false,
      isFetching: false,
      error: null,
      createAddress: vi.fn(),
      isCreating: false,
      updateAddress: vi.fn(),
      isUpdating: false,
      setDefaultAddress: vi.fn(),
      isSettingDefault: false,
      deleteAddress: vi.fn(),
      isDeleting: false,
      reload: vi.fn(),
    });

    render(
      <DeliveryAddressSelector
        store={mockStore}
        value={null}
        onChange={handleChange}
      />
    );

    const manualModeBtn = screen.getByRole('button', { name: /usar otra dirección/i });
    fireEvent.click(manualModeBtn);

    expect(screen.getByLabelText(/nombre del destinatario/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/teléfono celular/i)).toBeInTheDocument();

    // Fill in required fields
    fireEvent.change(screen.getByLabelText(/nombre del destinatario/i), {
      target: { value: 'Lucía Morales' },
    });
    fireEvent.change(screen.getByLabelText(/teléfono celular/i), {
      target: { value: '987111222' },
    });
    fireEvent.change(screen.getByLabelText(/departamento/i), {
      target: { value: 'LIMA' },
    });
    fireEvent.change(screen.getByLabelText(/provincia/i), {
      target: { value: 'LIMA' },
    });
    fireEvent.change(screen.getByLabelText(/distrito/i), {
      target: { value: 'SAN BORJA' },
    });
    fireEvent.change(screen.getByLabelText(/dirección exacta/i), {
      target: { value: 'Av. Aviación 2500' },
    });

    await waitFor(() => {
      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          mode: 'manual',
          recipientName: 'Lucía Morales',
          recipientPhone: '987111222',
          district: 'SAN BORJA',
          addressLine: 'Av. Aviación 2500',
        }),
        true
      );
    });
  });

  it('informs customer when store does not support delivery without forcing pickup', async () => {
    const handleChange = vi.fn();
    const storeWithoutDelivery: PublicStore = {
      ...mockStore,
      deliveryEnabled: false,
    };

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: mockAddresses,
      total: 2,
      defaultAddress: mockAddresses[0],
      canAddAddress: true,
      isLoading: false,
      isFetching: false,
      error: null,
      createAddress: vi.fn(),
      isCreating: false,
      updateAddress: vi.fn(),
      isUpdating: false,
      setDefaultAddress: vi.fn(),
      isSettingDefault: false,
      deleteAddress: vi.fn(),
      isDeleting: false,
      reload: vi.fn(),
    });

    render(
      <DeliveryAddressSelector
        store={storeWithoutDelivery}
        value={null}
        onChange={handleChange}
      />
    );

    expect(
      screen.getByText(/no realiza envíos a domicilio en este momento/i)
    ).toBeInTheDocument();
    expect(handleChange).toHaveBeenCalledWith(null, false);
  });

  it('disables saving checkbox when maximum of 5 addresses is reached', () => {
    const fiveAddresses = [1, 2, 3, 4, 5].map((i) => ({
      ...mockAddresses[0],
      id: `addr-${i}`,
      alias: `Dirección ${i}`,
    }));

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: fiveAddresses,
      total: 5,
      defaultAddress: fiveAddresses[0],
      canAddAddress: false,
      isLoading: false,
      isFetching: false,
      error: null,
      createAddress: vi.fn(),
      isCreating: false,
      updateAddress: vi.fn(),
      isUpdating: false,
      setDefaultAddress: vi.fn(),
      isSettingDefault: false,
      deleteAddress: vi.fn(),
      isDeleting: false,
      reload: vi.fn(),
    });

    render(
      <DeliveryAddressSelector
        store={mockStore}
        value={null}
        onChange={vi.fn()}
      />
    );

    const manualModeBtn = screen.getByRole('button', { name: /usar otra dirección/i });
    fireEvent.click(manualModeBtn);

    const saveCheckbox = screen.getByRole('checkbox', {
      name: /guardar esta dirección en mis direcciones/i,
    });
    expect(saveCheckbox).toBeDisabled();
    expect(
      screen.getByText(/has alcanzado el límite de 5 direcciones/i)
    ).toBeInTheDocument();
  });
});
