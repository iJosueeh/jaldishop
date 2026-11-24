import { beforeEach, describe, expect, it, vi } from 'vitest';
import { customerNotificationService } from './customerNotificationService';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('customerNotificationService', () => {
  it('lists notifications with pagination', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 'n-1',
            userId: 'u-1',
            type: 'ORDER_STATUS_CHANGED',
            title: 'Pedido listo',
            message: 'Tu pedido está listo para retiro.',
            status: 'UNREAD',
            createdAt: '2026-11-24T10:00:00Z',
            readAt: null,
          },
        ]),
        { status: 200 }
      )
    );

    const items = await customerNotificationService.listNotifications({ page: 0, size: 10 });
    expect(items).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/customer/notifications?page=0&size=10',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('fetches unread count', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ unreadCount: 5 }), { status: 200 }));
    const result = await customerNotificationService.getUnreadCount();
    expect(result.unreadCount).toBe(5);
  });

  it('marks notification as read via PATCH', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'n-1',
          status: 'READ',
          readAt: '2026-11-24T10:05:00Z',
        }),
        { status: 200 }
      )
    );

    const updated = await customerNotificationService.markAsRead('n-1');
    expect(updated.status).toBe('READ');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/customer/notifications/n-1/read',
      expect.objectContaining({ method: 'PATCH' })
    );
  });
});
