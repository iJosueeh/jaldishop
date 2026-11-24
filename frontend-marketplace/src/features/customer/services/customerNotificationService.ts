import { CustomerNotification, UnreadNotificationsCount } from '../types/customer.types';

export interface ListNotificationsParams {
  page?: number;
  size?: number;
}

export const customerNotificationService = {
  async listNotifications(params: ListNotificationsParams = {}): Promise<CustomerNotification[]> {
    const page = params.page ?? 0;
    const size = params.size ?? 20;
    const res = await fetch(`/api/customer/notifications?page=${page}&size=${size}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No pudimos cargar tus notificaciones.');
    }

    return res.json();
  },

  async getUnreadCount(): Promise<UnreadNotificationsCount> {
    const res = await fetch('/api/customer/notifications/unread-count', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No pudimos consultar el contador de notificaciones.');
    }

    return res.json();
  },

  async markAsRead(id: string): Promise<CustomerNotification> {
    const res = await fetch(`/api/customer/notifications/${encodeURIComponent(id)}/read`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No pudimos marcar la notificación como leída.');
    }

    return res.json();
  },
};
