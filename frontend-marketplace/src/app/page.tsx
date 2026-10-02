import React from 'react';
import dynamic from 'next/dynamic';
import { HeroSection } from '@/features/landing/components/HeroSection';
import { TrustMetrics } from '@/features/landing/components/TrustMetrics';
import { ComparisonSection } from '@/features/landing/components/ComparisonSection';
import { BentoFeatures } from '@/features/landing/components/BentoFeatures';
import { FeaturedStoresGrid } from '@/features/landing/components/FeaturedStoresGrid';
import { FinalCtaSection } from '@/features/landing/components/FinalCtaSection';
import { Footer } from '@/shared/components/layout/Footer';

const LiveOrdersShowcase = dynamic(
  () => import('@/features/landing/components/LiveOrdersShowcase').then((mod) => mod.LiveOrdersShowcase),
  { ssr: true }
);

const CapacitySimulatorSection = dynamic(
  () => import('@/features/landing/components/CapacitySimulatorSection').then((mod) => mod.CapacitySimulatorSection),
  { ssr: true }
);

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
