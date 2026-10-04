import React from 'react';
import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { storeService } from '@/features/storefront/services/storeService';
import { StorefrontHero } from '@/features/storefront/components/StorefrontHero';
import { StorefrontBadges } from '@/features/storefront/components/StorefrontBadges';
import { CatalogContainer } from '@/features/storefront/components/CatalogContainer';
import { Footer } from '@/shared/components/layout/Footer';
import { Container } from '@/shared/components/ui/Container';

interface StorefrontPageProps {
  params: Promise<{
    slug: string;
  }>;
}

export async function generateMetadata({
  params,
}: StorefrontPageProps): Promise<Metadata> {
  const { slug } = await params;
  const store = await storeService.getStoreBySlug(slug);

  if (!store) {
    return {
      title: 'Tienda no encontrada | JaldiShop',
      description: 'La tienda que buscas no existe o se encuentra inactiva.',
    };
  }

  return {
    title: `${store.name} | JaldiShop`,
    description:
      store.description ||
      `Ordena en línea en ${store.name}. Reserva tu cupo y recibe tus pedidos a tiempo con JaldiShop.`,
    openGraph: {
      title: `${store.name} — Pedidos Online`,
      description:
        store.description ||
        `Ordena en línea con control de capacidad en tiempo real en ${store.name}.`,
      type: 'website',
    },
  };
}

export default async function StorefrontPage({ params }: StorefrontPageProps) {
  const { slug } = await params;
  const store = await storeService.getStoreBySlug(slug);

  if (!store) {
    notFound();
  }

  return (
    <div className="flex flex-col min-h-screen bg-[#faf7f2]">
      {/* Store Header / Hero */}
      <StorefrontHero store={store} />

      {/* Main Container */}
      <Container size="lg" className="flex-1 py-8 space-y-8">
        {/* Badges & Operational Data */}
        <StorefrontBadges store={store} />

        {/* Modular Open/Closed Catalog Container */}
        <CatalogContainer
          storeSlug={store.slug}
          storeName={store.name}
        />
      </Container>

      {/* Footer */}
      <Footer />
    </div>
  );
}
