import React from 'react';
import Link from 'next/link';
import { Star, Clock, Bike, ArrowRight, Sparkles, Store } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';
import { EmptyState } from '@/shared/components/ui/EmptyState';
import { storeService } from '@/features/storefront/services/storeService';
import { PublicStore } from '@/features/storefront/types/storefront.types';

interface FeaturedStoresGridProps {
  initialStores?: PublicStore[];
}

export async function FeaturedStoresGrid({ initialStores }: FeaturedStoresGridProps) {
  const stores = initialStores ?? (await storeService.getFeaturedStores());

  return (
    <section id="tiendas-destacadas" className="py-24 bg-[#faf7f2] relative overflow-hidden">
      <Container size="lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-14 gap-4">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 text-xs font-bold text-[#005141] uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
              Explora Tiendas Verificadas
            </div>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-[#1c1917]">
              Comercios destacados en JaldiShop
            </h2>
            <p className="text-sm sm:text-base text-[#57534e] max-w-xl">
              Descubre negocios locales que sincronizan sus pedidos con precisión y calidad artesanal.
            </p>
          </div>

          <Link href="/tienda/panaderia-don-pepe">
            <Button variant="outline" size="sm" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
              Ver todas las tiendas
            </Button>
          </Link>
        </div>

        {/* Dynamic Stores Grid / Empty State Handling (@empty pattern) */}
        {stores.length === 0 ? (
          <EmptyState
            variant="card"
            icon={<Store className="w-8 h-8 text-[#005141]" />}
            title="Aún no hay comercios disponibles"
            description="Actualmente no encontramos tiendas activas en esta sección. Vuelve pronto para descubrir nuevos comercios locales."
            action={
              <Link href="/">
                <Button variant="outline" size="sm">
                  Recargar página
                </Button>
              </Link>
            }
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {stores.map((store, idx) => (
              <MotionFade key={store.slug} delay={idx * 0.15}>
                <Link href={`/tienda/${store.slug}`} className="block h-full group">
                  <Card
                    variant="interactive"
                    className="h-full flex flex-col justify-between overflow-hidden shadow-md group-hover:shadow-xl rounded-3xl border-0 transition-all duration-300 bg-white"
                  >
                    {/* Top Cover Banner */}
                    <div
                      className={`h-32 bg-gradient-to-br ${
                        store.coverGradient || 'from-[#feae2c]/25 via-[#ea580c]/10 to-[#faf7f2]'
                      } p-5 flex items-start justify-between relative`}
                    >
                      {store.badge && (
                        <Badge variant={store.badgeVariant || 'jade'} size="sm" className="bg-white shadow-xs">
                          {store.badge}
                        </Badge>
                      )}
                      <div className="text-xs font-bold text-[#1c1917] flex items-center gap-1.5 bg-white px-3 py-1 rounded-full shadow-xs ml-auto">
                        <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                        {store.rating || 4.9}{' '}
                        <span className="text-[#a8a29e] text-[10px]">({store.reviewsCount || 100})</span>
                      </div>
                    </div>

                    {/* Body Content */}
                    <div className="p-6 pt-0 relative flex-1 flex flex-col justify-between">
                      {/* Floating Store Logo */}
                      <div className="flex items-end justify-between -mt-7 mb-4">
                        <div
                          className={`w-14 h-14 rounded-2xl ${
                            store.logoBg || 'bg-[#ea580c]'
                          } text-white flex items-center justify-center shadow-md font-extrabold text-2xl`}
                        >
                          {store.iconEmoji || '🏪'}
                        </div>
                        {store.category && (
                          <span className="text-xs font-bold text-[#57534e] bg-[#faf7f2] px-3 py-1 rounded-xl">
                            {store.category}
                          </span>
                        )}
                      </div>

                      <div className="space-y-2">
                        <h3 className="text-xl font-bold text-[#1c1917] group-hover:text-[#005141] transition-colors">
                          {store.name}
                        </h3>
                        <p className="text-xs sm:text-sm text-[#57534e] line-clamp-2 leading-relaxed">
                          {store.tagline || store.description}
                        </p>
                      </div>

                      {/* Store Meta Info */}
                      <div className="mt-6 pt-4 border-t border-stone-100 flex items-center justify-between text-xs text-[#57534e]">
                        <div className="flex items-center gap-1.5 font-medium">
                          <Clock className="w-3.5 h-3.5 text-[#a8a29e]" />
                          <span>{store.preparationTimeMinutes ? `${store.preparationTimeMinutes} min` : '25-40 min'}</span>
                        </div>
                        <div className="flex items-center gap-1.5 font-semibold text-[#1c1917]">
                          <Bike className="w-3.5 h-3.5 text-[#ea580c]" />
                          <span>
                            {store.deliveryFee !== undefined
                              ? `Envío: S/ ${store.deliveryFee.toFixed(2)}`
                              : 'Envío disponible'}
                          </span>
                        </div>
                      </div>
                    </div>
                  </Card>
                </Link>
              </MotionFade>
            ))}
          </div>
        )}
      </Container>
    </section>
  );
}
