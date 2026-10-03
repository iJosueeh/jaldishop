import React from 'react';
import { Metadata } from 'next';
import { LegalLayout } from '@/features/legal/components/LegalLayout';
import { PRIVACY_DOCUMENT } from '@/features/legal/constants/privacyContent';
import { Footer } from '@/shared/components/layout/Footer';

export const metadata: Metadata = {
  title: 'Política de Privacidad y Datos Personales | JaldiShop Perú',
  description:
    'Protección estricta de datos personales, ejercicio de derechos ARCO y confidencialidad conforme a la Ley N° 29733 de la República del Perú en JaldiShop.',
  openGraph: {
    title: 'Política de Privacidad y Datos Personales | JaldiShop Perú',
    description:
      'Garantía de confidencialidad, ejercicio de derechos ARCO y seguridad en el tratamiento de datos en JaldiShop conforme a la Ley N° 29733.',
    type: 'article',
    locale: 'es_PE',
  },
  alternates: {
    canonical: '/privacidad',
  },
};

export default function PrivacidadPage() {
  return (
    <>
      <LegalLayout document={PRIVACY_DOCUMENT} />
      <Footer />
    </>
  );
}
