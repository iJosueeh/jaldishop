import React from 'react';
import type { Metadata } from 'next';
import { HeroSection } from '@/features/landing/components/HeroSection';
import { TrustMetrics } from '@/features/landing/components/TrustMetrics';
import {
  CraftMarqueeSection,
  MarqueeItem,
} from '@/features/landing/components/CraftMarqueeSection';
import { HowItWorksSection } from '@/features/landing/components/HowItWorksSection';
import { ComparisonSection } from '@/features/landing/components/ComparisonSection';
import { BentoFeatures } from '@/features/landing/components/BentoFeatures';
import { FeaturedStoresGrid } from '@/features/landing/components/FeaturedStoresGrid';
import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection';
import { Footer } from '@/shared/components/layout/Footer';
import { storeService } from '@/features/storefront/services/storeService';
import { formatCurrency } from '@/shared/utils/formatters';

const title = 'JaldiShop | Catálogo digital y pedidos por WhatsApp';
const description =
  'Comparte tu catálogo digital por WhatsApp e Instagram y organiza los pedidos de tu negocio con cupos por día y horarios de entrega o recojo.';

export const metadata: Metadata = {
  title,
  description,
  openGraph: {
    title,
    description,
    type: 'website',
    locale: 'es_PE',
  },
  twitter: {
    card: 'summary',
    title,
    description,
  },
};

export const revalidate = 60;

export default async function HomePage() {
  const [featuredStores, marketplaceProducts] = await Promise.all([
    storeService.getFeaturedStores(6),
    storeService.getMarketplaceProducts(4, 6),
  ]);

  const marqueeItems: MarqueeItem[] = marketplaceProducts.map((product) => ({
    id: product.id,
    name: product.name,
    category: product.category,
    store: product.storeName,
    storeSlug: product.storeSlug,
    productSlug: product.slug,
    price: formatCurrency(product.price, product.priceCurrency || 'PEN'),
    badge: product.deliveryEnabled ? 'Delivery disponible' : 'Retiro en tienda',
    badgeVariant: product.deliveryEnabled ? 'jade' : 'amber',
    imageUrl: product.imageUrl ?? null,
  }));

  return (
    <div className="flex flex-col min-h-screen bg-[#02090a] text-white selection:bg-[#36f4a4] selection:text-[#02090a]">
      {/* 1. Hero Section */}
      <HeroSection />

      {/* 2. Key Trust Metrics & Multi-payment Pillars */}
      <TrustMetrics />

      {/* 2.5 Real Products Marquee */}
      <CraftMarqueeSection items={marqueeItems} />

      {/* 3. How JaldiShop Works: 3 Visual Moments */}
      <HowItWorksSection />

      {/* 4. Comparison Section */}
      <ComparisonSection />

      {/* 5. Technology Bento Grid */}
      <BentoFeatures />

      {/* 6. Featured Local Stores */}
      <FeaturedStoresGrid initialStores={featuredStores} />

      {/* 7. Final High-Conversion Call To Action */}
      <FinalCtaSection />

      {/* 8. Global Footer */}
      <Footer />
    </div>
  );
}
