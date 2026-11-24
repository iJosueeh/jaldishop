import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { storeService } from '@/features/storefront/services/storeService';
import { PublicStore } from '@/features/storefront/types/storefront.types';
import { StorePeekSlider } from './StorePeekSlider';
import { env } from '@/core/config/env';

interface FeaturedStoresGridProps {
  initialStores?: PublicStore[];
}

export async function FeaturedStoresGrid({ initialStores }: FeaturedStoresGridProps) {
  const stores = initialStores ?? (await storeService.getFeaturedStores());
  const hasStores = stores.length > 0;

  return (
    <section id="tiendas-destacadas" className="py-20 sm:py-28 bg-[#fbfcfb] relative overflow-hidden scroll-mt-20 sm:scroll-mt-24">
      <Container size="lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 sm:mb-12 gap-4">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-stone-100 border border-stone-200 shadow-xs text-xs font-semibold text-[#072720] uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
              Explora negocios locales
            </div>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-light tracking-tight text-[#1c1917]">
              Comercios destacados en JaldiShop
            </h2>
            <p className="text-sm sm:text-base text-stone-600 max-w-xl">
              Descubre panaderías, reposterías y negocios locales. Consulta sus productos y horarios para organizar tu próxima compra.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link href="/tiendas">
              <Button
                variant="outline"
                size="sm"
                className="bg-white border-stone-200 hover:bg-stone-50 text-[#02090a] font-medium shadow-xs cursor-pointer rounded-full px-5 py-2.5"
                rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
              >
                Ver todas las tiendas
              </Button>
            </Link>
            {hasStores && (
              <Link href={`/tienda/${stores[0].slug}`}>
                <Button
                  variant="primary"
                  size="sm"
                  className="font-medium shadow-xs cursor-pointer rounded-full px-5 py-2.5"
                  rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                >
                  Explorar una tienda
                </Button>
              </Link>
            )}
          </div>
        </div>

        {hasStores ? (
          <StorePeekSlider stores={stores} />
        ) : (
          <div
            role="status"
            className="rounded-2xl border border-stone-200/90 bg-white p-10 sm:p-14 text-center max-w-2xl mx-auto space-y-4 shadow-xs"
          >
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-200/70 text-[#005141] flex items-center justify-center mx-auto">
              <Store className="w-6 h-6" />
            </div>
            <h3 className="font-display text-xl font-bold text-stone-900">
              Aún no hay comercios destacados disponibles
            </h3>
            <p className="text-sm text-stone-600 max-w-md mx-auto leading-relaxed">
              Los negocios locales de tu comunidad aparecerán aquí en cuanto publiquen su catálogo y horarios de atención.
            </p>
            <div className="pt-2">
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 rounded-full bg-[#005141] hover:bg-[#072720] text-white text-xs sm:text-sm font-semibold px-5 py-2.5 transition-colors"
              >
                <span>¿Tienes un comercio? Regístralo aquí</span>
                <ArrowRight className="w-4 h-4" />
              </a>
            </div>
          </div>
        )}
      </Container>
    </section>
  );
}
