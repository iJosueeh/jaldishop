import { Suspense } from 'react';
import { Metadata } from 'next';
import { PaymentReturnView } from '@/features/checkout/components/PaymentReturnView';

export const metadata: Metadata = {
  title: 'Pago Pendiente | JaldiShop',
  description: 'Verificación del resultado de pago en JaldiShop.',
};

export default function CheckoutPendingPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[80vh] bg-[#02090a] flex items-center justify-center p-6 text-[#99b3ad] text-sm">
          Cargando verificación de pago...
        </div>
      }
    >
      <PaymentReturnView expectedPath="pending" />
    </Suspense>
  );
}
