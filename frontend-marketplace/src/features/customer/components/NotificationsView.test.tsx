import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { NotificationsView } from './NotificationsView';

const mockMarkAsRead = vi.fn();
const mockReload = vi.fn();
const mockUseCustomerNotifications = vi.fn();

vi.mock('../hooks/useCustomerNotifications', () => ({
  useCustomerNotifications: (page: number, size: number) =>
    mockUseCustomerNotifications(page, size),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('NotificationsView Component', () => {
  it('renders notifications, filters unread, and marks notification as read', () => {
    mockUseCustomerNotifications.mockReturnValue({
      notifications: [
        {
          id: 'n-1',
          userId: 'u-1',
          type: 'ORDER_STATUS_CHANGED',
          title: 'Pedido confirmado',
          message: 'Tu pedido #ORD-101 fue confirmado.',
          status: 'UNREAD',
          createdAt: '2026-11-24T10:00:00Z',
          readAt: null,
        },
        {
          id: 'n-2',
          userId: 'u-1',
          type: 'SYSTEM',
          title: 'Bienvenido',
          message: 'Tu cuenta está activa.',
          status: 'READ',
          createdAt: '2026-11-20T10:00:00Z',
          readAt: '2026-11-20T12:00:00Z',
        },
      ],
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      markAsRead: mockMarkAsRead,
      isMarkingAsRead: false,
    });

    render(<NotificationsView />);

    expect(screen.getByText('Pedido confirmado')).toBeInTheDocument();
    expect(screen.getByText('Bienvenido')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /No leídas/i }));
    expect(screen.getByText('Pedido confirmado')).toBeInTheDocument();
    expect(screen.queryByText('Bienvenido')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Marcar como leída/i }));
    expect(mockMarkAsRead).toHaveBeenCalledWith('n-1');
  });

  it('renders empty state when there are no notifications', () => {
    mockUseCustomerNotifications.mockReturnValue({
      notifications: [],
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      markAsRead: mockMarkAsRead,
      isMarkingAsRead: false,
    });

    render(<NotificationsView />);
    expect(screen.getByText(/Aún no tienes notificaciones/i)).toBeInTheDocument();
  });
});
