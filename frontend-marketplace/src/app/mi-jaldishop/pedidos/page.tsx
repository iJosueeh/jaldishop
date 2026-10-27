import React from 'react';
import type { Metadata } from 'next';
import { OrdersView } from './OrdersView';

export const metadata: Metadata = {
  title: 'Mis Pedidos | Mi JaldiShop',
  description: 'Revisa el estado de tus pedidos y franjas horarias acordadas con cada comercio.',
};

export default function CustomerOrdersPage() {
  return <OrdersView />;
}
