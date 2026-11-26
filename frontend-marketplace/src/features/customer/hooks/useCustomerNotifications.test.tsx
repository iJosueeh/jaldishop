import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useCustomerNotifications,
  useUnreadNotificationsCount,
  UNREAD_NOTIFICATIONS_COUNT_KEY,
} from './useCustomerNotifications';
import { customerNotificationService } from '../services/customerNotificationService';

vi.mock('../services/customerNotificationService', () => ({
  customerNotificationService: {
    getUnreadCount: vi.fn(),
    listNotifications: vi.fn(),
    markAsRead: vi.fn(),
  },
}));

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useCustomerNotifications hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads unreadCount when enabled and respects disabled state', async () => {
    vi.mocked(customerNotificationService.getUnreadCount).mockResolvedValue({ unreadCount: 3 });
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    const { result } = renderHook(() => useUnreadNotificationsCount(true), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.unreadCount).toBe(3);
    });
    expect(customerNotificationService.getUnreadCount).toHaveBeenCalledTimes(1);
  });

  it('optimistically decrements unreadCount when marking a notification as read', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    queryClient.setQueryData(UNREAD_NOTIFICATIONS_COUNT_KEY, { unreadCount: 2 });

    vi.mocked(customerNotificationService.listNotifications).mockResolvedValue([
      {
        id: 'n-1',
        userId: 'u-1',
        type: 'ORDER_STATUS_CHANGED',
        title: 'Pedido confirmado',
        message: 'Tu pedido #ORD-1 fue confirmado',
        status: 'UNREAD',
        createdAt: '2026-11-25T10:00:00Z',
        readAt: null,
      },
    ]);
    vi.mocked(customerNotificationService.markAsRead).mockResolvedValue({
      id: 'n-1',
      userId: 'u-1',
      type: 'ORDER_STATUS_CHANGED',
      title: 'Pedido confirmado',
      message: 'Tu pedido #ORD-1 fue confirmado',
      status: 'READ',
      createdAt: '2026-11-25T10:00:00Z',
      readAt: '2026-11-25T10:05:00Z',
    });

    const { result } = renderHook(() => useCustomerNotifications(0, 20), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => {
      expect(result.current.notifications).toHaveLength(1);
    });

    await act(async () => {
      await result.current.markAsRead('n-1');
    });

    expect(customerNotificationService.markAsRead).toHaveBeenCalledWith('n-1');
    expect(queryClient.getQueryData(UNREAD_NOTIFICATIONS_COUNT_KEY)).toEqual({ unreadCount: 1 });
  });
});
