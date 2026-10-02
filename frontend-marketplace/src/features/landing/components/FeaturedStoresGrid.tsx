import React from 'react';
import Link from 'next/link';
import { Star, Clock, Bike, ArrowRight, Sparkles } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';

interface StoreCardData {
  name: string;
  slug: string;
  tagline: string;
  category: string;
  rating: number;
  reviews: number;
  deliveryTime: string;
  deliveryFee: string;
  coverGradient: string;
  logoBg: string;
  badge: string;
  badgeVariant: 'jade' | 'terracotta' | 'amber';
  iconEmoji: string;
}

const FEATURED_STORES: StoreCardData[] = [
  {
    name: 'Panadería Don Pepe',
    slug: 'panaderia-don-pepe',
    tagline: 'Pan artesanal de masa madre, empanadas y café de especialidad horneado cada mañana.',
    category: 'Panadería & Pastelería',
    rating: 4.9,
    reviews: 128,
    deliveryTime: '25-40 min',
    deliveryFee: 'S/ 5.00',
    coverGradient: 'from-[#feae2c]/25 via-[#ea580c]/10 to-[#faf7f2]',
    logoBg: 'bg-[#ea580c]',
    badge: 'Cupos disponibles hoy',
    badgeVariant: 'jade',
    iconEmoji: '🥖',
  },
  {
    name: 'Dulce Amor Repostería',
    slug: 'dulce-amor',
    tagline: 'Tortas personalizadas, cheesecakes de frutos rojos y macarons para momentos inolvidables.',
    category: 'Repostería Creativa',
    rating: 4.95,
    reviews: 94,
    deliveryTime: 'Programado 24h',
    deliveryFee: 'S/ 7.50',
    coverGradient: 'from-pink-500/20 via-rose-500/10 to-[#faf7f2]',
    logoBg: 'bg-rose-600',
    badge: 'Hold 10m activo',
    badgeVariant: 'terracotta',
    iconEmoji: '🍰',
  },
  {
    name: 'Tokyo Dark Kitchen',
    slug: 'tokyo-dark-kitchen',
    tagline: 'Ramen artesanal, sushi rolls tempura y gyozas en slots exactos para máxima frescura.',
    category: 'Comida Japonesa',
    rating: 4.85,
    reviews: 210,
    deliveryTime: '30-45 min',
    deliveryFee: 'S/ 6.00',
    coverGradient: 'from-[#005141]/20 via-[#166a57]/10 to-[#faf7f2]',
    logoBg: 'bg-[#005141]',
    badge: 'Franjas de cena abiertas',
    badgeVariant: 'amber',
    iconEmoji: '🍱',
  },
];

export function FeaturedStoresGrid() {
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
            <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[#1c1917]">
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

        {/* Stores Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {FEATURED_STORES.map((store, idx) => (
            <MotionFade key={store.slug} delay={idx * 0.15}>
              <Link href={`/tienda/${store.slug}`} className="block h-full group">
                <Card
                  variant="interactive"
                  className="h-full flex flex-col justify-between overflow-hidden border-2 border-[#e7e0d6] group-hover:border-[#005141] transition-all duration-300"
                >
                  {/* Top Cover Banner */}
                  <div className={`h-32 bg-gradient-to-br ${store.coverGradient} p-5 flex items-start justify-between relative`}>
                    <Badge variant={store.badgeVariant} size="sm" className="bg-white shadow-xs">
                      {store.badge}
                    </Badge>
                    <div className="text-xs font-bold text-[#1c1917] flex items-center gap-1.5 bg-white/95 px-3 py-1 rounded-full shadow-xs border border-[#e7e0d6]">
                      <Star className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" />
                      {store.rating} <span className="text-[#a8a29e] text-[10px]">({store.reviews})</span>
                    </div>
                  </div>

                  {/* Body Content */}
                  <div className="p-6 pt-0 relative flex-1 flex flex-col justify-between">
                    {/* Floating Store Logo */}
                    <div className="flex items-end justify-between -mt-7 mb-4">
                      <div className={`w-14 h-14 rounded-2xl ${store.logoBg} text-white flex items-center justify-center shadow-md font-extrabold text-2xl border-4 border-white`}>
                        {store.iconEmoji}
                      </div>
                      <span className="text-xs font-bold text-[#57534e] bg-[#faf7f2] px-3 py-1 rounded-xl border border-[#e7e0d6]">
                        {store.category}
                      </span>
                    </div>

                    <div className="space-y-2">
                      <h3 className="text-xl font-bold text-[#1c1917] group-hover:text-[#005141] transition-colors">
                        {store.name}
                      </h3>
                      <p className="text-xs sm:text-sm text-[#57534e] line-clamp-2 leading-relaxed">
                        {store.tagline}
                      </p>
                    </div>

                    {/* Store Meta Info */}
                    <div className="mt-6 pt-4 border-t border-[#e7e0d6] flex items-center justify-between text-xs text-[#57534e]">
                      <div className="flex items-center gap-1.5 font-medium">
                        <Clock className="w-3.5 h-3.5 text-[#a8a29e]" />
                        <span>{store.deliveryTime}</span>
                      </div>
                      <div className="flex items-center gap-1.5 font-semibold text-[#1c1917]">
                        <Bike className="w-3.5 h-3.5 text-[#ea580c]" />
                        <span>Envío: {store.deliveryFee}</span>
                      </div>
                    </div>
                  </div>
                </Card>
              </Link>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}
