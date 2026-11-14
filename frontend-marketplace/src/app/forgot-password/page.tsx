import type { Metadata } from 'next';
import { ForgotPasswordForm } from '@/features/auth/components/ForgotPasswordForm';

export const metadata: Metadata = {
  title: 'Recuperar contraseña | JaldiShop',
  description: 'Solicita un enlace para restablecer tu contraseña en JaldiShop.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
