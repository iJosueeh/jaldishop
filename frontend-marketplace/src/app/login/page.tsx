import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import { LoginForm } from '@/features/auth/components/LoginForm';

export const metadata: Metadata = {
  title: 'Iniciar Sesión | JaldiShop',
  description: 'Inicia sesión como cliente en JaldiShop para realizar y gestionar tus pedidos.',
};

export default function LoginPage() {
  return (
    <main className="min-h-screen w-full flex flex-col bg-[#fbfcfb]">
      <Suspense
        fallback={
          <div className="w-full min-h-screen flex items-center justify-center text-stone-600 bg-[#fbfcfb] animate-pulse">
            Cargando acceso...
          </div>
        }
      >
        <LoginForm />
      </Suspense>
    </main>
  );
}
