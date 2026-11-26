import { Suspense } from 'react';
import { Metadata } from 'next';
import { PaymentReturnView } from '@/features/checkout/components/PaymentReturnView';
import { Footer } from '@/shared/components/layout/Footer';

export const metadata: Metadata = {
  title: 'Retorno de Pago | JaldiShop',
  description: 'Verificación del resultado de pago en JaldiShop.',
};

export default function CheckoutSuccessPage() {
  return (
    <div className="min-h-screen flex flex-col bg-[#faf7f2]">
      <div className="flex-1">
        <Suspense
          fallback={
            <div className="min-h-[80vh] bg-[#faf7f2] flex items-center justify-center p-6 text-[#57534e] text-sm">
              Cargando verificación de pago...
            </div>
          }
        >
          <PaymentReturnView expectedPath="success" />
        </Suspense>
      </div>
      <Footer />
    </div>
  );
}
