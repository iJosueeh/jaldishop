import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import { RegisterForm } from '@/features/auth/components/RegisterForm';

export const metadata: Metadata = {
  title: 'Crear Cuenta | JaldiShop',
  description: 'Crea tu cuenta de cliente en JaldiShop y realiza pedidos en comercios locales sin sobreventa.',
};

export default function RegisterPage() {
  return (
    <main className="min-h-screen w-full flex flex-col bg-[#fbfcfb]">
      <Suspense
        fallback={
          <div className="w-full min-h-screen flex items-center justify-center text-stone-600 bg-[#fbfcfb] animate-pulse">
            Cargando registro...
          </div>
        }
      >
        <RegisterForm />
      </Suspense>
    </main>
  );
}
