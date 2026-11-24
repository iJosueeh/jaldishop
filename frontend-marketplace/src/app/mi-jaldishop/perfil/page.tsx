import React from 'react';
import type { Metadata } from 'next';
import { ProfileForm } from '@/features/customer/components/ProfileForm';

export const metadata: Metadata = {
  title: 'Mi Cuenta y Seguridad | Mi JaldiShop',
  description: 'Actualiza tus datos personales y revisa la seguridad de tu cuenta en JaldiShop.',
};

export default function CustomerProfilePage() {
  return (
    <div className="space-y-6 w-full">
      <div className="pb-5 border-b border-stone-200/80">
        <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
          Mi Cuenta y Seguridad
        </h2>
        <p className="text-xs sm:text-sm text-stone-600 mt-0.5 leading-relaxed">
          Mantén actualizada tu información personal y de contacto para coordinar tus compras.
        </p>
      </div>

      <ProfileForm />
    </div>
  );
}
