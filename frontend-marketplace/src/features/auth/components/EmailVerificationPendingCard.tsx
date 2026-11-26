'use client';

import React from 'react';
import Link from 'next/link';
import { AlertCircle, CheckCircle2, MailCheck, RefreshCw } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { useResendCooldown } from '../hooks/useResendCooldown';
import { maskEmail } from '../utils/authErrors';

export interface EmailVerificationPendingCardProps {
  email: string;
  title?: string;
  description?: string;
  loginHref?: string;
  initialCooldown?: number;
  onSecondaryAction?: () => void;
  secondaryActionLabel?: string;
}

export function EmailVerificationPendingCard({
  email,
  title = 'Revisa tu correo',
  description = 'Te enviamos un enlace de verificación para activar tu cuenta antes de iniciar sesión.',
  loginHref = '/login',
  initialCooldown = 60,
  onSecondaryAction,
  secondaryActionLabel = 'Usar otro correo electrónico',
}: EmailVerificationPendingCardProps) {
  const { cooldownSeconds, isResending, resendMessage, resendError, resend } =
    useResendCooldown(initialCooldown);

  const maskedEmail = maskEmail(email);

  return (
    <div
      className="space-y-6 text-center animate-fadeIn"
      data-testid="email-verification-pending"
    >
      <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141]">
        <MailCheck className="w-8 h-8 text-[#005141]" aria-hidden="true" />
      </div>

      <div>
        <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
          {title}
        </h1>
        <p className="mt-3 text-sm text-stone-600 leading-relaxed">{description}</p>
        {maskedEmail && (
          <p className="mt-2 inline-flex items-center px-3 py-1 rounded-full bg-[#faf7f2] border border-[#e7e0d6] text-xs font-semibold text-[#005141]">
            {maskedEmail}
          </p>
        )}
      </div>

      {resendMessage && (
        <div
          role="status"
          className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-start gap-2.5 text-left"
        >
          <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" aria-hidden="true" />
          <span className="flex-1 font-medium">{resendMessage}</span>
        </div>
      )}

      {resendError && (
        <div
          role="alert"
          className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5 text-left"
        >
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
          <span className="flex-1 font-medium">{resendError}</span>
        </div>
      )}

      <div className="p-4 rounded-xl bg-[#faf7f2] border border-[#e7e0d6] text-left text-xs text-stone-600 space-y-2">
        <p className="font-semibold text-stone-800">Consejos importantes:</p>
        <ul className="list-disc list-inside space-y-1 text-stone-600">
          <li>El enlace de verificación es de un solo uso y caduca en 24 horas.</li>
          <li>Revisa tu carpeta de spam o correo no deseado si no lo ves en tu bandeja.</li>
        </ul>
      </div>

      <div className="pt-2 flex flex-col gap-3">
        <Button
          type="button"
          variant="primary"
          size="lg"
          isLoading={isResending}
          disabled={isResending || cooldownSeconds > 0}
          onClick={() => resend(email)}
          className="w-full text-sm font-bold shadow-md shadow-[#005141]/20 cursor-pointer"
        >
          <RefreshCw className="w-4 h-4 mr-2" aria-hidden="true" />
          {isResending
            ? 'Reenviando correo...'
            : cooldownSeconds > 0
            ? `Reenviar correo (${cooldownSeconds}s)`
            : 'Reenviar correo'}
        </Button>

        <Link
          href={loginHref}
          className="w-full inline-flex justify-center items-center py-3 px-4 rounded-xl font-bold text-sm bg-white border border-stone-300 text-stone-800 hover:bg-stone-50 transition-colors"
        >
          Ir al inicio de sesión
        </Link>

        {onSecondaryAction && (
          <button
            type="button"
            onClick={onSecondaryAction}
            className="text-xs font-semibold text-stone-500 hover:text-stone-800 hover:underline transition-colors py-1 cursor-pointer"
          >
            {secondaryActionLabel}
          </button>
        )}
      </div>
    </div>
  );
}
