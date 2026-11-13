import { Suspense } from 'react';
import { Metadata } from 'next';
import { PaymentReturnView } from '@/features/checkout/components/PaymentReturnView';

export const metadata: Metadata = {
  title: 'Retorno de Pago | JaldiShop',
  description: 'Verificación del resultado de pago en JaldiShop.',
};

export default function CheckoutSuccessPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[70vh] flex items-center justify-center p-6 text-stone-500 text-sm">
          Cargando verificación de pago...
        </div>
      }
    >
      <PaymentReturnView expectedPath="success" />
    </Suspense>
  );
}
