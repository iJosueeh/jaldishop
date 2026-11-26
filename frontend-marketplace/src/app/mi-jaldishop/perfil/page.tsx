import React from 'react';
import type { Metadata } from 'next';
import { ProfileForm } from '@/features/customer/components/ProfileForm';
import { AccountPageHeader } from '@/features/customer/components/AccountPageHeader';

export const metadata: Metadata = {
  title: 'Mi Cuenta y Seguridad | Mi JaldiShop',
  description: 'Actualiza tus datos personales y revisa la seguridad de tu cuenta en JaldiShop.',
};

export default function CustomerProfilePage() {
  return (
    <div className="space-y-6 w-full">
      <AccountPageHeader
        title="Mi Cuenta y Seguridad"
        description="Mantén actualizada tu información personal y de contacto para coordinar tus compras."
      />

      <ProfileForm />
    </div>
  );
}
