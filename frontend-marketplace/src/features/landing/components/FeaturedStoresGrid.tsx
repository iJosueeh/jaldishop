import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';
import { storeService } from '@/features/storefront/services/storeService';
import { PublicStore } from '@/features/storefront/types/storefront.types';
import { StorePeekSlider } from './StorePeekSlider';

interface FeaturedStoresGridProps {
  initialStores?: PublicStore[];
}

const CURATED_DEFAULT_STORES: PublicStore[] = [
  {
    id: 'store-01',
    name: 'Panadería Don Pepe',
    slug: 'panaderia-don-pepe',
    category: 'Panadería & Masa Madre',
    tagline: 'Fermentación natural de 24 horas y horneado artesanal al alba en Miraflores.',
    description: 'Fermentación natural de 24 horas y horneado artesanal al alba en Miraflores.',
    bannerUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=800&auto=format&fit=crop',
    iconEmoji: '🥐',
    logoBg: 'bg-[#005141]',
    rating: 4.9,
    reviewsCount: 148,
    preparationTimeMinutes: 20,
    deliveryFee: 5.0,
    deliveryEnabled: true,
    pickupEnabled: true,
    status: 'ACTIVE',
    badge: 'Más Pedido',
    badgeVariant: 'amber',
  },
  {
    id: 'store-02',
    name: 'Dulce Amor Repostería',
    slug: 'dulce-amor',
    category: 'Pastelería & Postres',
    tagline: 'Tortas artesanales de autor, alfajores de maicena y bocaditos para celebraciones.',
    description: 'Tortas artesanales de autor, alfajores de maicena y bocaditos para celebraciones.',
    bannerUrl: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=800&auto=format&fit=crop',
    iconEmoji: '🍰',
    logoBg: 'bg-[#ea580c]',
    rating: 4.8,
    reviewsCount: 94,
    preparationTimeMinutes: 30,
    deliveryFee: 6.0,
    deliveryEnabled: true,
    pickupEnabled: true,
    status: 'ACTIVE',
    badge: 'Horneado Hoy',
    badgeVariant: 'terracotta',
  },
  {
    id: 'store-03',
    name: 'Café Villa Rica Barista',
    slug: 'cafe-villa-rica',
    category: 'Cafetería de Especialidad',
    tagline: 'Granos de altura 100% arábica tostados semanalmente y cold brew embotellado.',
    description: 'Granos de altura 100% arábica tostados semanalmente y cold brew embotellado.',
    bannerUrl: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=800&auto=format&fit=crop',
    iconEmoji: '☕',
    logoBg: 'bg-stone-900',
    rating: 5.0,
    reviewsCount: 210,
    preparationTimeMinutes: 10,
    deliveryFee: 0,
    deliveryEnabled: true,
    pickupEnabled: true,
    status: 'ACTIVE',
    badge: '100% Arábica',
    badgeVariant: 'jade',
  },
  {
    id: 'store-04',
    name: 'La Focacceria Urbana',
    slug: 'la-focacceria',
    category: 'Pizzas & Masas Italianas',
    tagline: 'Focaccias crujientes con romero fresco, aceite de oliva virgen extra y mortadela.',
    description: 'Focaccias crujientes con romero fresco, aceite de oliva virgen extra y mortadela.',
    bannerUrl: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=800&auto=format&fit=crop',
    iconEmoji: '🍕',
    logoBg: 'bg-[#005141]',
    rating: 4.9,
    reviewsCount: 82,
    preparationTimeMinutes: 25,
    deliveryFee: 5.5,
    deliveryEnabled: true,
    pickupEnabled: true,
    status: 'ACTIVE',
    badge: 'Receta Romana',
    badgeVariant: 'amber',
  },
  {
    id: 'store-05',
    name: 'El Taller del Bagel',
    slug: 'el-taller-del-bagel',
    category: 'Desayunos & Brunches',
    tagline: 'Bagels hervidos estilo Montreal rellenos de salmón curado, queso crema y eneldo.',
    description: 'Bagels hervidos estilo Montreal rellenos de salmón curado, queso crema y eneldo.',
    bannerUrl: 'https://images.unsplash.com/photo-1585478259715-876a6a81ae08?q=80&w=800&auto=format&fit=crop',
    iconEmoji: '🥯',
    logoBg: 'bg-[#ea580c]',
    rating: 4.8,
    reviewsCount: 67,
    preparationTimeMinutes: 15,
    deliveryFee: 4.5,
    deliveryEnabled: true,
    pickupEnabled: true,
    status: 'ACTIVE',
    badge: 'Nuevo en Jaldi',
    badgeVariant: 'jade',
  },
];

export async function FeaturedStoresGrid({ initialStores }: FeaturedStoresGridProps) {
  const fetchedStores = initialStores ?? (await storeService.getFeaturedStores());
  // Si la API devuelve comercios, los mostramos; si viene vacío (ej. sin backend levantado), usamos los comercios curados con fallbacks
  const storesToDisplay = fetchedStores.length > 0 ? fetchedStores : CURATED_DEFAULT_STORES;

  return (
    <section id="tiendas-destacadas" className="py-20 sm:py-28 bg-[#faf7f2] relative overflow-hidden scroll-mt-20 sm:scroll-mt-24">
      {/* Background soft ambient accents */}
      <div className="absolute top-1/2 left-0 -translate-y-1/2 w-96 h-96 bg-[#feae2c]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-[#005141]/5 rounded-full blur-3xl pointer-events-none" />

      <Container size="lg">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 sm:mb-12 gap-4">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white border border-stone-200 shadow-xs text-xs font-bold text-[#005141] uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
              Explora Tiendas Verificadas
            </div>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-[#1c1917]">
              Comercios destacados en JaldiShop
            </h2>
            <p className="text-sm sm:text-base text-[#57534e] max-w-xl">
              Descubre panaderías, reposterías y creadores locales que sincronizan sus pedidos con máxima precisión y calidad artesanal.
            </p>
          </div>

          <Link href="/tienda/panaderia-don-pepe">
            <Button
              variant="outline"
              size="sm"
              className="bg-white border-stone-200 hover:bg-stone-50 text-[#1c1917] font-bold shadow-xs cursor-pointer rounded-2xl"
              rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              Ver todas las tiendas
            </Button>
          </Link>
        </div>

        {/* Peek Slider Carousel Component with Default Fallbacks & Interactive Controls */}
        <StorePeekSlider stores={storesToDisplay} />
      </Container>
    </section>
  );
}
