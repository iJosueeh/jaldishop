import React from 'react';
import type { Metadata } from 'next';
import { AccountSubNav } from '@/features/customer/components/AccountSubNav';
import { AddressesView } from '@/features/customer/components/AddressesView';

export const metadata: Metadata = {
  title: 'Mis Direcciones | Mi JaldiShop',
  description: 'Gestiona tus direcciones de entrega en todo el Perú para tus compras en JaldiShop.',
};

export default function CustomerAddressesPage() {
  return (
    <div className="space-y-6 w-full">
      <div className="pb-5 border-b border-stone-200/80">
        <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
          Datos de mi Cuenta
        </h2>
        <p className="text-xs sm:text-sm text-stone-600 mt-0.5 leading-relaxed">
          Mantén actualizada tu información de contacto y direcciones para coordinar la entrega de tus compras.
        </p>
      </div>

      <AccountSubNav />

      <AddressesView />
    </div>
  );
}
