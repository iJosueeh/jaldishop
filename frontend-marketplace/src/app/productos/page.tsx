import React from 'react';
import type { Metadata } from 'next';
import { Package } from 'lucide-react';
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

export default async function ProductosPage() {
  const products = await storeService.getMarketplaceProducts(12, 12);

  return (
    <div className="min-h-screen bg-[#faf7f2] flex flex-col">
      <section className="bg-gradient-to-r from-[#02090a] via-[#072720] to-[#061a1c] text-white pt-28 sm:pt-32 pb-10 border-b border-[#1e2c31]">
        <Container size="lg">
          <div className="space-y-3 max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider">
              <Package className="w-3.5 h-3.5" />
              <span>Catálogo del Marketplace</span>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
              Productos de comercios locales
            </h1>
            <p className="text-sm sm:text-base text-[#99b3ad] leading-relaxed">
              Descubre productos disponibles hoy y reserva tu turno de entrega o retiro directamente en cada tienda.
            </p>
          </div>
        </Container>
      </section>

      <main className="flex-1 py-8 sm:py-12">
        <Container size="lg">
          <ProductsCatalogView products={products} />
        </Container>
      </main>

      <Footer />
    </div>
  );
}
