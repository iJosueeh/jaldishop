import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import { LoginForm } from '@/features/auth/components/LoginForm';
import { storeService } from '@/features/storefront/services/storeService';

export const revalidate = 60;

export const metadata: Metadata = {
  title: 'Iniciar Sesión | JaldiShop',
  description: 'Inicia sesión como cliente en JaldiShop para realizar y gestionar tus pedidos.',
};

export default async function LoginPage() {
  const featuredStores = await storeService.getFeaturedStores(1);
  const featuredStore = featuredStores[0] ?? null;

  return (
    <main className="min-h-screen w-full flex flex-col bg-[#fbfcfb]">
      <Suspense
        fallback={
          <div className="w-full min-h-screen flex items-center justify-center text-stone-600 bg-[#fbfcfb] animate-pulse">
            Cargando acceso...
          </div>
        }
      >
        <LoginForm featuredStore={featuredStore} />
      </Suspense>
    </main>
  );
}
