import { Suspense } from 'react';
import { Metadata } from 'next';
import { PaymentReturnView } from '@/features/checkout/components/PaymentReturnView';

export const metadata: Metadata = {
  title: 'Pago No Concretado | JaldiShop',
  description: 'Verificación del resultado de pago en JaldiShop.',
};

export default function CheckoutFailurePage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[80vh] bg-[#faf7f2] flex items-center justify-center p-6 text-[#57534e] text-sm">
          Cargando verificación de pago...
        </div>
      }
    >
      <PaymentReturnView expectedPath="failure" />
    </Suspense>
  );
}
