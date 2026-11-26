import React from 'react';
import type { Metadata } from 'next';
import { AddressesView } from '@/features/customer/components/AddressesView';

export const metadata: Metadata = {
  title: 'Mis Direcciones | Mi JaldiShop',
  description: 'Gestiona tus direcciones de entrega en todo el Perú para tus compras en JaldiShop.',
};

export default function CustomerAddressesPage() {
  return <AddressesView />;
}
