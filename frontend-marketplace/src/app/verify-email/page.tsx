import type { Metadata } from 'next';
import { Suspense } from 'react';
import { VerifyEmailView } from '@/features/auth/components/VerifyEmailView';

export const metadata: Metadata = {
  title: 'Verificar correo | JaldiShop',
  description: 'Confirma tu dirección de correo electrónico para activar tu cuenta en JaldiShop.',
  robots: {
    index: false,
    follow: false,
  },
  referrer: 'no-referrer',
};

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<div className="w-full min-h-screen bg-[#faf7f2]" />}>
      <VerifyEmailView />
    </Suspense>
  );
}
