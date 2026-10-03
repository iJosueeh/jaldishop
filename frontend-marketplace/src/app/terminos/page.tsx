import React from 'react';
import { Metadata } from 'next';
import { LegalLayout } from '@/features/legal/components/LegalLayout';
import { TERMS_DOCUMENT } from '@/features/legal/constants/termsContent';
import { Footer } from '@/shared/components/layout/Footer';

export const metadata: Metadata = {
  title: 'Términos y Condiciones de Uso | JaldiShop Perú',
  description:
    'Condiciones contractuales para compras gastronómicas bajo pedido, control de capacidad, franjas horarias y pasarelas de pago seguras en JaldiShop.',
  openGraph: {
    title: 'Términos y Condiciones de Uso | JaldiShop Perú',
    description:
      'Condiciones contractuales para compras gastronómicas bajo pedido, control de capacidad y pagos en JaldiShop conforme a la Ley N° 29571.',
    type: 'article',
    locale: 'es_PE',
  },
  alternates: {
    canonical: '/terminos',
  },
};

export default function TerminosPage() {
  return (
    <>
      <LegalLayout document={TERMS_DOCUMENT} />
      <Footer />
    </>
  );
}
