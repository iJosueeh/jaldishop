import { Suspense } from 'react';
import { Metadata } from 'next';
import { CheckoutPageClient } from '@/features/checkout/components/CheckoutPageClient';

export const metadata: Metadata = {
  title: 'Checkout | JaldiShop',
  description: 'Selecciona tu dirección, turno de entrega y paga de forma segura con Mercado Pago.',
};

export default function CheckoutPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#faf7f2] flex items-center justify-center p-6 text-[#57534e] text-sm">
          Cargando checkout...
        </div>
      }
    >
      <CheckoutPageClient />
    </Suspense>
  );
}
