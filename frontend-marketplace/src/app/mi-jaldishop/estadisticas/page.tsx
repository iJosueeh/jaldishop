import React from 'react';
import type { Metadata } from 'next';
import { AnalyticsView } from '@/features/customer/components/AnalyticsView';

export const metadata: Metadata = {
  title: 'Mis Estadísticas | Mi JaldiShop',
  description: 'Consulta el resumen de tus compras, productos favoritos y exporta tu reporte en PDF.',
};

export default function CustomerAnalyticsPage() {
  return <AnalyticsView />;
}
