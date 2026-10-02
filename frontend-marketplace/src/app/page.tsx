import React from 'react';
import { HeroSection } from '@/features/landing/components/HeroSection';
import { TrustMetrics } from '@/features/landing/components/TrustMetrics';
import { LiveOrdersShowcase } from '@/features/landing/components/LiveOrdersShowcase';
import { ComparisonSection } from '@/features/landing/components/ComparisonSection';
import { CapacitySimulatorSection } from '@/features/landing/components/CapacitySimulatorSection';
import { BentoFeatures } from '@/features/landing/components/BentoFeatures';
import { FeaturedStoresGrid } from '@/features/landing/components/FeaturedStoresGrid';
import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection';
import { Footer } from '@/shared/components/layout/Footer';

export default function HomePage() {
  return (
    <div className="flex flex-col min-h-screen bg-[#faf7f2]">
      <HeroSection />
      <TrustMetrics />
      <LiveOrdersShowcase />
      <ComparisonSection />
      <CapacitySimulatorSection />
      <BentoFeatures />
      <FeaturedStoresGrid />
      <FinalCtaSection />
      <Footer />
    </div>
  );
}

