import React from 'react';
import Link from 'next/link';
import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { storeService } from '@/features/storefront/services/storeService';
import { StorefrontHero } from '@/features/storefront/components/StorefrontHero';
import { CatalogContainer } from '@/features/storefront/components/CatalogContainer';
import { StoreLocationMap } from '@/features/storefront/components/map';
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

  // Cargar productos activos del catálogo público de la tienda
  const products = await storeService.getStoreProducts(store.id);
  const contactNumber = store.whatsappNumber || store.phone;
  const whatsappUrl = contactNumber?.replace(/\D/g, '') ? `https://wa.me/${contactNumber.replace(/\D/g, '')}` : null;

  return (
    <div className="flex flex-col min-h-screen bg-[#faf7f2]">
      {/* Store Header / Hero Modular */}
      <StorefrontHero store={store} />

      {/* Main Container */}
      <Container size="lg" className="flex-1 py-8 space-y-8">
        {/* Modular Open/Closed Catalog Container con productos reales */}
        <CatalogContainer
          storeSlug={store.slug}
          storeName={store.name}
          products={products}
          store={store}
        />

        {/* Mapa Interactivo del Local con Coordenadas OpenStreetMap Leaflet */}
        <StoreLocationMap store={store} />
        <section aria-labelledby="contact-heading" className="flex flex-col gap-3 rounded-3xl border border-stone-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between sm:p-7">
          <div>
            <h2 id="contact-heading" className="font-display text-lg font-bold text-stone-900">¿Tienes una consulta para {store.name}?</h2>
            <p className="mt-1 text-sm text-stone-600">{whatsappUrl ? 'Consulta directamente con la tienda antes de elegir.' : 'La tienda aún no ha publicado un número de contacto.'}</p>
          </div>
          {whatsappUrl && <a href={whatsappUrl} target="_blank" rel="noopener noreferrer" className="shrink-0 rounded-2xl bg-emerald-50 px-5 py-3 text-sm font-semibold text-[#005141] transition hover:bg-emerald-100">Contactar por WhatsApp</a>}
        </section>
      </Container>

      {/* Footer */}
      <footer className="border-t border-stone-200 py-6 text-center text-sm text-stone-500">
        <Link href="/" className="font-semibold text-[#005141]">JaldiShop</Link>
        <span className="mx-2">·</span>
        <Link href="/terminos" className="hover:underline">Términos</Link>
        <span className="mx-2">·</span>
        <Link href="/privacidad" className="hover:underline">Privacidad</Link>
      </footer>
    </div>
  );
}
