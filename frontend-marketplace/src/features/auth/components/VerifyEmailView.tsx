'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Loader2,
  Mail,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  WifiOff,
} from 'lucide-react';
import { ApiException } from '@/core/api/apiClient';
import { Button } from '@/shared/components/ui/Button';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { useResendCooldown } from '../hooks/useResendCooldown';
import { customerAuthService } from '../services/customerAuthService';
import { mapAuthErrorMessage } from '../utils/authErrors';
import { getSafeRedirectUrl } from '../utils/safeRedirect';
import { isValidResetToken, validateEmailField } from '../utils/validation';

type VerificationStatus = 'verifying' | 'success' | 'invalid_token' | 'network_error';

/**
 * Token Security Notes:
 * 1. Token is extracted from ?token=... once on mount and immediately stripped from the URL via replaceState.
 * 2. A ref guard prevents React StrictMode double-mount from firing two single-use verification requests.
 */
export function VerifyEmailView() {
  const searchParams = useSearchParams();
  const redirectParam = searchParams.get('redirect');
  const safeRedirect = getSafeRedirectUrl(redirectParam, '/login');
  const ctaHref =
    safeRedirect === '/checkout' || safeRedirect.startsWith('/checkout')
      ? `/login?redirect=${encodeURIComponent(safeRedirect)}`
      : safeRedirect === '/login'
      ? '/login'
      : `/login?redirect=${encodeURIComponent(safeRedirect)}`;
  const ctaLabel =
    safeRedirect === '/checkout' || safeRedirect.startsWith('/checkout')
      ? 'Iniciar sesión para continuar al checkout'
      : 'Iniciar sesión';

  const [token] = useState<string | null>(() => {
    const param = searchParams.get('token');
    return param ? param.trim() : null;
  });

  const [status, setStatus] = useState<VerificationStatus>(() =>
    isValidResetToken(token) ? 'verifying' : 'invalid_token'
  );
  const [networkErrorMessage, setNetworkErrorMessage] = useState<string | null>(null);
  const [resendEmail, setResendEmail] = useState('');
  const [emailFieldError, setEmailFieldError] = useState<string | null>(null);
  const hasRequestedRef = useRef(false);

  const { cooldownSeconds, isResending, resendMessage, resendError, resend } =
    useResendCooldown(0);

  const verify = useCallback(async (verificationToken: string) => {
    setStatus('verifying');
    setNetworkErrorMessage(null);

    try {
      await customerAuthService.verifyEmail({ token: verificationToken });
      setStatus('success');
    } catch (err: unknown) {
      if (err instanceof ApiException) {
        if (
          err.status === 422 ||
          err.status === 400 ||
          err.errorData?.code === 'INVALID_EMAIL_VERIFICATION_TOKEN'
        ) {
          setStatus('invalid_token');
          return;
        }
        setNetworkErrorMessage(
          mapAuthErrorMessage(
            err,
            'No pudimos verificar tu correo en este momento. Inténtalo nuevamente.'
          )
        );
        setStatus('network_error');
        return;
      }

      setNetworkErrorMessage(
        mapAuthErrorMessage(
          err,
          'Ocurrió un error de conexión al verificar tu correo.'
        )
      );
      setStatus('network_error');
    }
  }, []);

  useEffect(() => {
    if (typeof window !== 'undefined' && window.location.search.includes('token')) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  useEffect(() => {
    if (!token || !isValidResetToken(token)) {
      return;
    }
    if (hasRequestedRef.current) {
      return;
    }
    hasRequestedRef.current = true;
    void verify(token);
  }, [token, verify]);

  const handleResendSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const err = validateEmailField(resendEmail);
    if (err) {
      setEmailFieldError(err);
      return;
    }
    setEmailFieldError(null);
    await resend(resendEmail);
  };

  return (
    <div className="w-full min-h-screen flex flex-col lg:flex-row bg-[#faf7f2]">
      {/* Panel Izquierdo Institucional */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-[#02090a] via-[#072720] to-[#061a1c] p-12 xl:p-16 flex-col justify-between border-r border-[#1e2c31] overflow-hidden">
        <div
          className="absolute -top-32 -left-32 w-96 h-96 bg-[#005141]/30 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />
        <div
          className="absolute bottom-0 right-0 w-80 h-80 bg-[#36f4a4]/10 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />

        <div className="relative z-10 flex items-center justify-between">
          <Link
            href="/"
            className="inline-block transition-transform hover:scale-105 active:scale-95 duration-150 rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
            aria-label="Ir a la página de inicio de JaldiShop"
          >
            <JaldiShopLogo size="md" variant="light" />
          </Link>
          <Link
            href="/login"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#99b3ad] hover:text-[#36f4a4] transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Ir al inicio de sesión</span>
          </Link>
        </div>

        <div className="relative z-10 my-auto py-8 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider mb-6">
            <Sparkles className="w-3.5 h-3.5 text-[#36f4a4]" />
            Activación de Cuenta
          </div>
          <h2 className="text-3xl xl:text-4xl font-display font-bold text-white tracking-tight leading-tight">
            Protegemos tu cuenta desde el primer ingreso.
          </h2>
          <p className="mt-4 text-sm text-[#99b3ad] leading-relaxed">
            Confirmar tu correo nos permite asegurar tus pedidos, enviarte comprobantes y proteger tu carrito en todos tus dispositivos.
          </p>
        </div>

        <div className="relative z-10 pt-6 border-t border-[#1e2c31]/60 flex items-center gap-6 text-xs text-[#99b3ad]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
            <span>Enlace de un solo uso</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-[#36f4a4]" />
            <span>Vigencia de 24 horas</span>
          </div>
        </div>
      </div>

      {/* Panel Derecho */}
      <div className="w-full lg:w-1/2 min-h-screen flex flex-col justify-center items-center p-6 sm:p-10 lg:p-16 bg-[#faf7f2]">
        <div className="w-full max-w-md my-auto bg-white border border-[#e7e0d6] rounded-2xl p-6 sm:p-8 shadow-sm">
          <div className="flex items-center justify-between mb-8 lg:hidden">
            <Link href="/" aria-label="JaldiShop">
              <JaldiShopLogo size="sm" variant="dark" />
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center gap-1 text-xs font-semibold text-stone-600 hover:text-[#005141]"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Ir a login</span>
            </Link>
          </div>

          {status === 'verifying' && (
            <div className="space-y-5 text-center py-6" role="status" aria-live="polite" data-testid="verify-email-loading">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141]">
                <Loader2 className="w-8 h-8 animate-spin text-[#005141]" aria-hidden="true" />
              </div>
              <div>
                <h1 className="text-2xl font-display font-bold text-[#1c1917] tracking-tight">
                  Verificando tu correo...
                </h1>
                <p className="mt-2 text-sm text-stone-600">
                  Estamos confirmando tu enlace de activación. Esto tomará solo un momento.
                </p>
              </div>
            </div>
          )}

          {status === 'success' && (
            <div className="space-y-6 text-center animate-fadeIn" data-testid="verify-email-success">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141]">
                <CheckCircle2 className="w-8 h-8 text-[#005141]" aria-hidden="true" />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  ¡Correo verificado!
                </h1>
                <p className="mt-3 text-sm text-stone-600 leading-relaxed">
                  Tu cuenta de cliente ya está activa. Inicia sesión para comenzar a comprar en tus tiendas locales favoritas.
                </p>
              </div>
              <div className="pt-2">
                <Link
                  href={ctaHref}
                  className="w-full inline-flex justify-center items-center py-3 px-4 rounded-xl font-bold text-sm bg-[#005141] text-white hover:bg-[#00382e] shadow-md shadow-[#005141]/20 transition-all"
                >
                  {ctaLabel}
                </Link>
              </div>
            </div>
          )}

          {status === 'invalid_token' && (
            <div className="space-y-6 animate-fadeIn" data-testid="verify-email-invalid">
              <div className="text-center">
                <div className="w-16 h-16 mx-auto rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-600 mb-4">
                  <AlertCircle className="w-8 h-8 text-amber-600" aria-hidden="true" />
                </div>
                <h1 className="text-2xl font-display font-bold text-[#1c1917] tracking-tight">
                  Enlace inválido o vencido
                </h1>
                <p className="mt-2 text-sm text-stone-600 leading-relaxed">
                  Este enlace de verificación no es válido, ya fue usado o ha expirado. Solicita uno nuevo ingresando tu correo.
                </p>
              </div>

              {resendMessage && (
                <div role="status" className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-start gap-2.5">
                  <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" aria-hidden="true" />
                  <span className="flex-1 font-medium">{resendMessage}</span>
                </div>
              )}

              {resendError && (
                <div role="alert" className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
                  <span className="flex-1 font-medium">{resendError}</span>
                </div>
              )}

              <form onSubmit={handleResendSubmit} noValidate className="space-y-4">
                <div>
                  <label
                    htmlFor="resend-email"
                    className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                  >
                    Correo electrónico
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400" aria-hidden="true">
                      <Mail className="h-4 w-4" />
                    </div>
                    <input
                      id="resend-email"
                      name="email"
                      type="email"
                      autoComplete="email"
                      value={resendEmail}
                      onChange={(e) => {
                        setResendEmail(e.target.value);
                        if (emailFieldError) setEmailFieldError(null);
                      }}
                      disabled={isResending}
                      aria-invalid={Boolean(emailFieldError)}
                      aria-describedby={emailFieldError ? 'resend-email-error' : undefined}
                      placeholder="ejemplo@correo.com"
                      className={`w-full pl-10 pr-4 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 focus:outline-none ${
                        emailFieldError
                          ? 'border-rose-400 focus:border-rose-500'
                          : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                      }`}
                    />
                  </div>
                  {emailFieldError && (
                    <p id="resend-email-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                      {emailFieldError}
                    </p>
                  )}
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  isLoading={isResending}
                  disabled={isResending || cooldownSeconds > 0}
                  className="w-full text-sm font-bold shadow-md shadow-[#005141]/20 cursor-pointer"
                >
                  <RefreshCw className="w-4 h-4 mr-2" aria-hidden="true" />
                  {isResending
                    ? 'Enviando enlace...'
                    : cooldownSeconds > 0
                    ? `Reenviar correo (${cooldownSeconds}s)`
                    : 'Reenviar correo de verificación'}
                </Button>
              </form>

              <div className="pt-2 text-center border-t border-stone-200">
                <Link href="/login" className="text-xs font-semibold text-[#005141] hover:underline">
                  Volver al inicio de sesión
                </Link>
              </div>
            </div>
          )}

          {status === 'network_error' && (
            <div className="space-y-6 text-center animate-fadeIn" data-testid="verify-email-network-error">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-rose-50 border border-rose-200 flex items-center justify-center text-rose-600">
                <WifiOff className="w-8 h-8 text-rose-600" aria-hidden="true" />
              </div>
              <div>
                <h1 className="text-2xl font-display font-bold text-[#1c1917] tracking-tight">
                  Error de conexión
                </h1>
                <p className="mt-3 text-sm text-stone-600 leading-relaxed">
                  {networkErrorMessage || 'No pudimos conectar con el servidor para validar tu enlace.'}
                </p>
              </div>
              <div className="pt-2 flex flex-col gap-3">
                {token && isValidResetToken(token) && (
                  <Button
                    type="button"
                    variant="primary"
                    size="lg"
                    onClick={() => verify(token)}
                    className="w-full text-sm font-bold cursor-pointer"
                  >
                    Reintentar verificación
                  </Button>
                )}
                <Link
                  href="/login"
                  className="text-xs font-semibold text-stone-600 hover:text-[#005141] hover:underline py-1"
                >
                  Volver al inicio de sesión
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
