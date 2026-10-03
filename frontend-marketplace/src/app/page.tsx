import React from 'react';
import { HeroSection } from '@/features/landing/components/HeroSection';
import { TrustMetrics } from '@/features/landing/components/TrustMetrics';
import { CraftMarqueeSection } from '@/features/landing/components/CraftMarqueeSection';
import { HowItWorksSection } from '@/features/landing/components/HowItWorksSection';
import { ComparisonSection } from '@/features/landing/components/ComparisonSection';
import { BentoFeatures } from '@/features/landing/components/BentoFeatures';
import { FeaturedStoresGrid } from '@/features/landing/components/FeaturedStoresGrid';
import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection';
import { Footer } from '@/shared/components/layout/Footer';

export default function HomePage() {
  return (
    <div className="flex flex-col min-h-screen bg-[#faf7f2]">
      {/* 1. Hero Section (Intact) */}
      <HeroSection />

      {/* 2. Key Trust Metrics & Multi-payment Pillars */}
      <TrustMetrics />

      {/* 2.5 Continuous Artisanal Craft Marquee (Smooth Infinite Loop with Hover Pause) */}
      <CraftMarqueeSection />

      {/* 3. How JaldiShop Works: 3 Visual Moments (Warm Craft + Modern HUD) */}
      <HowItWorksSection />

      {/* 4. Comparison Section: Chat Chaos vs. JaldiShop Order Layer */}
      <ComparisonSection />

      {/* 5. Technology Bento Grid (Capacity Engine, 10m Hold, Mercado Pago/Yape) */}
      <BentoFeatures />

      {/* 6. Featured Local Stores with Artisanal Food Photography */}
      <FeaturedStoresGrid />

      {/* 7. Final High-Conversion Call To Action */}
      <FinalCtaSection />

      {/* 8. Global Footer */}
      <Footer />
    </div>
  );
}
