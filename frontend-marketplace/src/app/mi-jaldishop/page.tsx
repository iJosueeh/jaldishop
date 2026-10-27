import React from 'react';
import type { Metadata } from 'next';
import { CustomerHomeView } from './CustomerHomeView';

export const metadata: Metadata = {
  title: 'Mi JaldiShop | Inicio del Comprador',
  description: 'Panel de resumen personal de tus pedidos y perfil en JaldiShop.',
};

export default function CustomerDashboardPage() {
  return <CustomerHomeView />;
}
