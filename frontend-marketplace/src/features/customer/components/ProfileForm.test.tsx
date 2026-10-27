import React from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ProfileForm } from './ProfileForm';
import * as hooks from '../hooks/useCustomerProfile';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('ProfileForm Component', () => {
  const mockMutateAsync = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(hooks, 'useUpdateCustomerProfile').mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
    } as unknown as ReturnType<typeof hooks.useUpdateCustomerProfile>);
  });

  it('renders loading skeleton while fetching profile', () => {
    vi.spyOn(hooks, 'useCustomerProfile').mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof hooks.useCustomerProfile>);

    render(<ProfileForm />);
    expect(screen.queryByLabelText(/Nombres/i)).not.toBeInTheDocument();
  });

  it('renders customer profile information correctly', () => {
    vi.spyOn(hooks, 'useCustomerProfile').mockReturnValue({
      data: {
        id: '123',
        email: 'cliente@ejemplo.com',
        firstName: 'María',
        lastName: 'Rojas',
        phone: '+51987654321',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
        createdAt: '2026-01-15T00:00:00Z',
      },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof hooks.useCustomerProfile>);

    render(<ProfileForm />);
    expect(screen.getByDisplayValue('María')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Rojas')).toBeInTheDocument();
    expect(screen.getByDisplayValue('+51987654321')).toBeInTheDocument();
    expect(screen.getByText('cliente@ejemplo.com')).toBeInTheDocument();
  });

  it('validates required fields and shows inline errors without submitting', async () => {
    vi.spyOn(hooks, 'useCustomerProfile').mockReturnValue({
      data: {
        id: '123',
        email: 'cliente@ejemplo.com',
        firstName: 'María',
        lastName: 'Rojas',
        phone: '',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
        createdAt: '2026-01-15T00:00:00Z',
      },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof hooks.useCustomerProfile>);

    render(<ProfileForm />);

    const firstNameInput = screen.getByLabelText(/Nombres/i);
    fireEvent.change(firstNameInput, { target: { value: '' } });

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/Ingresa tus nombres/i)).toBeInTheDocument();
    });
    expect(mockMutateAsync).not.toHaveBeenCalled();
  });

  it('submits valid profile updates and calls mutation with normalized phone', async () => {
    vi.spyOn(hooks, 'useCustomerProfile').mockReturnValue({
      data: {
        id: '123',
        email: 'cliente@ejemplo.com',
        firstName: 'María',
        lastName: 'Rojas',
        phone: '',
        status: 'ACTIVE',
        roles: ['CUSTOMER'],
        createdAt: '2026-01-15T00:00:00Z',
      },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof hooks.useCustomerProfile>);

    mockMutateAsync.mockResolvedValue({
      id: '123',
      email: 'cliente@ejemplo.com',
      firstName: 'María',
      lastName: 'Rojas',
      phone: '987654321',
      status: 'ACTIVE',
      roles: ['CUSTOMER'],
      createdAt: '2026-01-15T00:00:00Z',
    });

    render(<ProfileForm />);

    const phoneInput = screen.getByLabelText(/Teléfono celular/i);
    fireEvent.change(phoneInput, { target: { value: '987 654 321' } });

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith({
        firstName: 'María',
        lastName: 'Rojas',
        phone: '987654321',
      });
    });
  });
});
