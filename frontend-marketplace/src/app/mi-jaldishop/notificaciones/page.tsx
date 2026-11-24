import React from 'react';
import type { Metadata } from 'next';
import { NotificationsView } from '@/features/customer/components/NotificationsView';

export const metadata: Metadata = {
  title: 'Notificaciones | Mi JaldiShop',
  description: 'Centro de notificaciones y avisos de tus pedidos en JaldiShop.',
};

export default function CustomerNotificationsPage() {
  return <NotificationsView />;
}
