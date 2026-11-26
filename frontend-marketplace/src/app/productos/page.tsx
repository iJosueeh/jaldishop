import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import { Container } from '@/shared/components/ui/Container';
import { Footer } from '@/shared/components/layout/Footer';
import { storeService } from '@/features/storefront/services/storeService';
import { ProductsCatalogView } from '@/features/storefront/components/ProductsCatalogView';

export const revalidate = 60;

export const metadata: Metadata = {
  title: 'Catálogo de Productos | JaldiShop',
  description:
    'Explora productos reales publicados por comercios locales en JaldiShop con horarios disponibles.',
};

function ProductsCatalogSkeleton() {
  return (
    <div
      aria-busy="true"
      aria-label="Cargando catálogo de productos"
      className="w-full lg:grid lg:grid-cols-[280px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)] lg:gap-8 items-start"
    >
      <div className="hidden lg:block h-[480px] rounded-2xl bg-white border border-[#e7e0d6] animate-pulse" />
      <div className="space-y-5 min-w-0">
        <div className="h-[76px] rounded-2xl bg-white border border-[#e7e0d6] animate-pulse" />
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div
              key={idx}
              className="h-[320px] rounded-2xl bg-white border border-[#e7e0d6] animate-pulse"
            />
          ))}
        </div>
      </div>
    </div>
  );
}

export default async function ProductosPage() {
  const products = await storeService.getMarketplaceProducts(12, 12);

  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col">
      <section className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] text-white pt-28 sm:pt-32 pb-10 border-b border-[#1e2c31]">
        <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12">
          <div className="space-y-2.5 max-w-2xl">
            <h1 className="font-display text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
              Productos de comercios locales
            </h1>
            <p className="text-sm sm:text-base text-[#99b3ad] leading-relaxed">
              Descubre productos disponibles hoy y reserva tu turno de entrega o retiro directamente en cada tienda.
            </p>
          </div>
        </Container>
      </section>

      <main className="flex-1 py-6 sm:py-10">
        <Container size="full" className="px-4 sm:px-6 lg:px-10 xl:px-12">
          <Suspense fallback={<ProductsCatalogSkeleton />}>
            <ProductsCatalogView products={products} />
          </Suspense>
        </Container>
      </main>

      <Footer />
    </div>
  );
}
