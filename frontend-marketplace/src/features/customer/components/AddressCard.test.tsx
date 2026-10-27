import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AddressCard } from './AddressCard';
import { CustomerAddress } from '../types/customer.types';

const mockAddress: CustomerAddress = {
  id: 'addr-1',
  alias: 'Casa',
  recipientName: 'Carlos Benítez',
  recipientPhone: '987654321',
  department: 'LIMA',
  province: 'LIMA',
  district: 'MIRAFLORES',
  ubigeoCode: '150122',
  addressLine: 'Av. Larco 1234, Dpto 402',
  reference: 'Frente al parque',
  latitude: null,
  longitude: null,
  isDefault: true,
  createdAt: '2026-10-26T10:00:00Z',
  updatedAt: '2026-10-26T10:00:00Z',
};

describe('AddressCard Component', () => {
  it('renders address details, recipient, and default badge', () => {
    render(
      <AddressCard
        address={mockAddress}
        onEdit={vi.fn()}
        onDelete={vi.fn()}
        onSetDefault={vi.fn()}
      />
    );

    expect(screen.getByText('Casa')).toBeInTheDocument();
    expect(screen.getByText('Predeterminada')).toBeInTheDocument();
    expect(screen.getByText('Av. Larco 1234, Dpto 402')).toBeInTheDocument();
    expect(screen.getByText('MIRAFLORES, LIMA')).toBeInTheDocument();
    expect(screen.getByText('Frente al parque')).toBeInTheDocument();
    expect(screen.getByText('Carlos Benítez')).toBeInTheDocument();
    expect(screen.getByText('987654321')).toBeInTheDocument();
  });

  it('shows set default button for non-default address and triggers callback', () => {
    const handleSetDefault = vi.fn();
    const nonDefaultAddress = { ...mockAddress, isDefault: false };

    render(
      <AddressCard
        address={nonDefaultAddress}
        onEdit={vi.fn()}
        onDelete={vi.fn()}
        onSetDefault={handleSetDefault}
      />
    );

    expect(screen.queryByText('Predeterminada')).not.toBeInTheDocument();
    const setDefaultBtn = screen.getByRole('button', {
      name: /establecer como predeterminada/i,
    });
    expect(setDefaultBtn).toBeInTheDocument();

    fireEvent.click(setDefaultBtn);
    expect(handleSetDefault).toHaveBeenCalledWith('addr-1');
  });

  it('triggers edit and delete callbacks', () => {
    const handleEdit = vi.fn();
    const handleDelete = vi.fn();

    render(
      <AddressCard
        address={mockAddress}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onSetDefault={vi.fn()}
      />
    );

    const editBtn = screen.getByRole('button', { name: /editar dirección/i });
    fireEvent.click(editBtn);
    expect(handleEdit).toHaveBeenCalledWith(mockAddress);

    const deleteBtn = screen.getByRole('button', { name: /eliminar dirección/i });
    fireEvent.click(deleteBtn);
    expect(handleDelete).toHaveBeenCalledWith(mockAddress);
  });
});
