import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AddressesView } from './AddressesView';
import { useCustomerAddresses } from '../hooks/useCustomerAddresses';

vi.mock('../hooks/useCustomerAddresses', () => ({
  useCustomerAddresses: vi.fn(),
}));

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
    reference: null,
    latitude: null,
    longitude: null,
    isDefault: true,
    createdAt: '2026-10-26T10:00:00Z',
    updatedAt: '2026-10-26T10:00:00Z',
  },
];

describe('AddressesView Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders empty state when customer has 0 addresses', () => {
    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: [],
      total: 0,
      canAddAddress: true,
      defaultAddress: null,
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

    render(<AddressesView />);

    expect(
      screen.getByText('Aún no tienes direcciones guardadas')
    ).toBeInTheDocument();
    expect(screen.getByText('0 de 5 direcciones')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /registrar primera dirección/i })
    ).toBeInTheDocument();
  });

  it('renders addresses list with counter and active addresses', () => {
    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: mockAddresses,
      total: 1,
      canAddAddress: true,
      defaultAddress: mockAddresses[0],
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

    render(<AddressesView />);

    expect(screen.getByText('1 de 5 direcciones')).toBeInTheDocument();
    expect(screen.getByText('Casa')).toBeInTheDocument();
    expect(screen.getByText('Av. Larco 1234')).toBeInTheDocument();
  });

  it('shows limit notice when 5 addresses are reached and disables adding', () => {
    const fiveAddresses = [1, 2, 3, 4, 5].map((i) => ({
      ...mockAddresses[0],
      id: `addr-${i}`,
      alias: `Dirección ${i}`,
    }));

    vi.mocked(useCustomerAddresses).mockReturnValue({
      addresses: fiveAddresses,
      total: 5,
      canAddAddress: false,
      defaultAddress: mockAddresses[0],
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

    render(<AddressesView />);

    expect(screen.getByText('5 de 5 direcciones')).toBeInTheDocument();
    expect(
      screen.getByText(/has alcanzado el límite máximo permitido de 5 direcciones/i)
    ).toBeInTheDocument();

    const addBtn = screen.getByRole('button', { name: /agregar dirección/i });
    expect(addBtn).toBeDisabled();
  });
});
