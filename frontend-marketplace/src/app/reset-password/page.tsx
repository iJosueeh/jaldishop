import type { Metadata } from 'next';
import { Suspense } from 'react';
import { ResetPasswordForm } from '@/features/auth/components/ResetPasswordForm';

export const metadata: Metadata = {
  title: 'Restablecer contraseña | JaldiShop',
  description: 'Crea una nueva contraseña segura para tu cuenta en JaldiShop.',
  robots: {
    index: false,
    follow: false,
  },
  referrer: 'no-referrer',
};

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="w-full min-h-screen bg-[#fbfcfb]" />}>
      <ResetPasswordForm />
    </Suspense>
  );
}
