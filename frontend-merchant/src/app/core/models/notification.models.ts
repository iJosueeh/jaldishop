export type NotificationType = 'NEW_ORDER' | 'ORDER_STATUS_CHANGED' | 'LOW_STOCK' | 'SYSTEM';

export type NotificationStatus = 'UNREAD' | 'READ';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  status: NotificationStatus;
  createdAt: string;
  readAt: string | null;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
