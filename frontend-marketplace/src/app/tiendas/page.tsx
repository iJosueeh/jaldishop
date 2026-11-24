import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import { Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Footer } from '@/shared/components/layout/Footer';
import { storeService } from '@/features/storefront/services/storeService';
import { StoresCatalogView } from '@/features/storefront/components/StoresCatalogView';

export const revalidate = 60;

export const metadata: Metadata = {
  title: 'Explorar Tiendas | JaldiShop',
  description:
    'Directorio público de comercios locales en JaldiShop. Filtra por rubro, búsqueda o modalidad de entrega.',
};

export default async function TiendasPage() {
  const [stores, categories] = await Promise.all([
    storeService.getAllPublicStores('', 50),
    storeService.getCategories(),
  ]);

  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col">
      <section className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] text-white pt-28 sm:pt-32 pb-10 border-b border-[#1e2c31]">
        <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12">
          <div className="space-y-3 max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider">
              <Store className="w-3.5 h-3.5" />
              <span>Directorio de Comercios</span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
              Explora tiendas locales en JaldiShop
            </h1>
            <p className="text-sm sm:text-base text-[#99b3ad] leading-relaxed">
              Encuentra panaderías, pastelerías, cafeterías y negocios con horarios reales de entrega o retiro.
            </p>
          </div>
        </Container>
      </section>

      <main className="flex-1 py-6 sm:py-10">
        <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12">
          <Suspense
            fallback={
              <div className="h-64 rounded-2xl bg-white border border-stone-200 animate-pulse" />
            }
          >
            <StoresCatalogView stores={stores} categories={categories} />
          </Suspense>
        </Container>
      </main>

      <Footer />
    </div>
  );
}
