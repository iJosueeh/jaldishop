import React from 'react';
import Link from 'next/link';
import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { MessageCircle } from 'lucide-react';
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

export const revalidate = 300;

export async function generateMetadata({
  params,
}: StorefrontPageProps): Promise<Metadata> {
  try {
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
  } catch {
    return {
      title: 'Tienda | JaldiShop',
      description: 'Plataforma de gestión de pedidos en línea con JaldiShop.',
    };
  }
}

export default async function StorefrontPage({ params }: StorefrontPageProps) {
  const { slug } = await params;
  let store: import('@/features/storefront/types/storefront.types').PublicStore | null = null;
  let loadFailed = false;

  try {
    store = await storeService.getStoreBySlug(slug);
  } catch (error) {
    console.error(`[StorefrontPage] Error al obtener datos de la tienda '${slug}':`, error);
    loadFailed = true;
  }

  if (!store && !loadFailed) {
    notFound();
  }

  if (loadFailed || !store) {
    return (
      <div className="flex flex-col min-h-screen bg-[#fcfbfa]">
        <main className="flex-1 flex items-center justify-center py-20 px-6">
          <div className="max-w-md w-full text-center space-y-6 bg-white p-8 sm:p-10 rounded-[12px] border border-stone-200/90 shadow-xl">
            <div className="w-16 h-16 rounded-full bg-amber-50 border border-amber-200 text-[#ea580c] flex items-center justify-center mx-auto">
              <span className="w-3 h-3 rounded-full bg-[#ea580c] animate-ping" />
            </div>
            <div className="space-y-2">
              <h1 className="font-display text-2xl font-bold text-stone-900">
                No pudimos conectar con la tienda
              </h1>
              <p className="text-sm text-stone-600 leading-relaxed">
                Estamos teniendo una breve reconexión con el servidor backend. Esto no significa que la tienda no exista.
              </p>
            </div>
            <div className="pt-2 flex flex-col gap-3">
              <a
                href={`/tienda/${slug}`}
                className="w-full inline-flex items-center justify-center px-6 py-3 rounded-full bg-[#005141] hover:bg-[#00382d] text-white font-bold text-sm shadow-md transition-all"
              >
                Reintentar conexión
              </a>
              <Link
                href="/"
                className="text-xs font-semibold text-stone-500 hover:text-stone-800 transition-colors"
              >
                Volver al inicio de JaldiShop
              </Link>
            </div>
          </div>
        </main>
      </div>
    );
  }

  // Cargar productos activos del catálogo público de la tienda
  const products = await storeService.getStoreProducts(store.id);
  const contactNumber = store.whatsappNumber || store.phone;
  const whatsappUrl = contactNumber?.replace(/\D/g, '') ? `https://wa.me/${contactNumber.replace(/\D/g, '')}` : null;

  return (
    <div className="flex flex-col min-h-screen bg-[#fcfbfa]">
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

        {/* Sección de Consulta Directa con 12px radius & Pill Button */}
        <section aria-labelledby="contact-heading" className="flex flex-col gap-4 rounded-[12px] border border-stone-200/90 bg-white p-5 sm:flex-row sm:items-center sm:justify-between sm:p-7 shadow-xs">
          <div>
            <h2 id="contact-heading" className="font-display text-lg font-bold text-stone-900">¿Tienes una consulta para {store.name}?</h2>
            <p className="mt-1 text-sm text-stone-600">{whatsappUrl ? 'Consulta directamente con la tienda antes de elegir.' : 'La tienda aún no ha publicado un número de contacto.'}</p>
          </div>
          {whatsappUrl && (
            <a
              href={whatsappUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex shrink-0 items-center gap-2 rounded-full bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/90 px-6 py-3 text-sm font-bold text-[#005141] transition-all hover:-translate-y-0.5 active:scale-95 shadow-2xs"
            >
              <MessageCircle className="w-4 h-4 text-[#005141]" />
              <span>Contactar por WhatsApp</span>
            </a>
          )}
        </section>
      </Container>

      {/* Footer sobrio conforme a DESIGN.md */}
      <footer className="border-t border-stone-200/90 py-6 text-center text-xs text-stone-500 bg-white">
        <Link href="/" className="font-bold text-[#005141] hover:underline transition-colors">JaldiShop</Link>
        <span className="mx-2 text-stone-300">·</span>
        <Link href="/terminos" className="hover:text-stone-800 transition-colors">Términos</Link>
        <span className="mx-2 text-stone-300">·</span>
        <Link href="/privacidad" className="hover:text-stone-800 transition-colors">Privacidad</Link>
      </footer>
    </div>
  );
}
