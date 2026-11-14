'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  Mail,
  ArrowLeft,
  AlertCircle,
  KeyRound,
  ShieldCheck,
  CheckCircle2,
  MailCheck,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { customerAuthService } from '../services/customerAuthService';
import { validateEmailField } from '../utils/validation';

export function ForgotPasswordForm() {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [isSubmittedSuccess, setIsSubmittedSuccess] = useState(false);

  const handleBlur = () => {
    setEmailError(validateEmailField(email));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    const error = validateEmailField(email);
    if (error) {
      setEmailError(error);
      return;
    }

    setIsSubmitting(true);

    try {
      await customerAuthService.forgotPassword({
        email: email.trim().toLowerCase(),
      });
      // Always transition to success view to prevent account enumeration
      setIsSubmittedSuccess(true);
    } catch (err: unknown) {
      const message =
        err instanceof Error
          ? err.message
          : 'Ocurrió un error inesperado al solicitar el restablecimiento.';
      setErrorMessage(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full min-h-screen flex flex-col lg:flex-row bg-[#fbfcfb]">
      {/* =========================================================================
          PANEL IZQUIERDO (SPLIT-SCREEN): VERDE GREENHOUSE DE MARCA (DESKTOP)
          ========================================================================= */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-[#02090a] via-[#072720] to-[#061a1c] p-12 xl:p-16 flex-col justify-between border-r border-[#1e2c31] overflow-hidden">
        <div
          className="absolute -top-32 -left-32 w-96 h-96 bg-[#005141]/30 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />
        <div
          className="absolute bottom-0 right-0 w-80 h-80 bg-[#36f4a4]/10 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />

        {/* Header superior izquierdo: Logo oficial claro y enlace volver a login */}
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
            <span>Volver al inicio de sesión</span>
          </Link>
        </div>

        {/* Contenido central: Explicación de seguridad y recuperación */}
        <div className="relative z-10 my-auto py-8 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider mb-6">
            <Sparkles className="w-3.5 h-3.5 text-[#36f4a4]" />
            Acceso Protegido
          </div>

          <h2 className="text-3xl xl:text-4xl font-display font-bold text-white tracking-tight leading-tight">
            Recupera el acceso a tus compras y pedidos locales.
          </h2>

          <p className="mt-4 text-sm text-[#99b3ad] leading-relaxed">
            Te enviaremos un enlace confidencial y de uso único para crear una nueva contraseña de forma rápida y segura.
          </p>

          <div className="mt-8 p-6 rounded-2xl bg-[#061a1c]/90 border border-[#1e2c31] backdrop-blur-sm shadow-xl shadow-black/50">
            <div className="flex items-center gap-3 pb-3.5 border-b border-[#1e2c31]/80">
              <div className="w-10 h-10 rounded-xl bg-[#005141]/40 border border-[#133b32] flex items-center justify-center text-[#36f4a4]">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-bold text-white block">Enlace de un solo uso</span>
                <span className="text-[11px] text-[#99b3ad]">Expira en 15 minutos por tu seguridad</span>
              </div>
            </div>
            <p className="pt-3.5 text-xs text-[#99b3ad] leading-relaxed">
              Tus pedidos anteriores y direcciones guardadas permanecerán intactos en tu cuenta.
            </p>
          </div>
        </div>

        {/* Footer inferior izquierdo: Garantías de confianza */}
        <div className="relative z-10 pt-6 border-t border-[#1e2c31]/60 flex items-center gap-6 text-xs text-[#99b3ad]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
            <span>Encriptación de extremo a extremo</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-[#36f4a4]" />
            <span>Sin exposición de datos</span>
          </div>
        </div>
      </div>

      {/* =========================================================================
          PANEL DERECHO: FORMULARIO CENTRADO EN MÓVIL Y ESCRITORIO
          ========================================================================= */}
      <div className="w-full lg:w-1/2 min-h-screen flex flex-col justify-center items-center p-6 sm:p-10 lg:p-16 bg-[#fbfcfb]">
        <div className="w-full max-w-md my-auto">
          {/* Cabecera para móvil con Logo y Volver */}
          <div className="flex items-center justify-between mb-8 lg:hidden">
            <Link href="/" aria-label="JaldiShop">
              <JaldiShopLogo size="sm" variant="dark" />
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center gap-1 text-xs font-semibold text-stone-600 hover:text-[#005141]"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Volver</span>
            </Link>
          </div>

          {isSubmittedSuccess ? (
            /* =========================================================================
               ESTADO DE ÉXITO (CONFIRMACIÓN ANTI-ENUMERACIÓN)
               ========================================================================= */
            <div className="space-y-6 text-center animate-fadeIn" data-testid="forgot-password-success">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141]">
                <MailCheck className="w-8 h-8 text-[#005141]" />
              </div>

              <div>
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  Revisa tu correo
                </h1>
                <p className="mt-3 text-sm text-stone-600 leading-relaxed">
                  Si tu correo electrónico corresponde a una cuenta habilitada en JaldiShop, recibirás un enlace para restablecer tu contraseña.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-stone-50 border border-stone-200 text-left text-xs text-stone-600 space-y-2">
                <p className="font-semibold text-stone-800">Consejos importantes:</p>
                <ul className="list-disc list-inside space-y-1 text-stone-500">
                  <li>El enlace de recuperación es válido durante 15 minutos.</li>
                  <li>Revisa tu carpeta de spam o correo no deseado.</li>
                  <li>Si no recibes el correo, verifica haber ingresado la dirección correcta.</li>
                </ul>
              </div>

              <div className="pt-2 flex flex-col gap-3">
                <Link
                  href="/login"
                  className="w-full inline-flex justify-center items-center py-3 px-4 rounded-xl font-bold text-sm bg-[#005141] text-white hover:bg-[#00382e] shadow-md shadow-[#005141]/20 transition-all cursor-pointer"
                >
                  Volver al inicio de sesión
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    setIsSubmittedSuccess(false);
                    setEmail('');
                    setErrorMessage(null);
                    setEmailError(null);
                  }}
                  className="text-xs font-semibold text-stone-500 hover:text-stone-800 hover:underline transition-colors py-2"
                >
                  Intentar con otro correo electrónico
                </button>
              </div>
            </div>
          ) : (
            /* =========================================================================
               ESTADO DE FORMULARIO
               ========================================================================= */
            <div>
              <div className="mb-8">
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  ¿Olvidaste tu contraseña?
                </h1>
                <p className="mt-2 text-sm text-stone-600">
                  Ingresa tu correo electrónico registrado y te enviaremos las instrucciones de recuperación.
                </p>
              </div>

              {/* Banner de error */}
              {errorMessage && (
                <div
                  role="alert"
                  className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5 animate-fadeIn"
                >
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span className="flex-1 font-medium">{errorMessage}</span>
                </div>
              )}

              <form onSubmit={handleSubmit} noValidate className="space-y-4">
                {/* Campo Correo */}
                <div>
                  <label
                    htmlFor="forgot-email"
                    className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                  >
                    Correo electrónico
                  </label>
                  <div className="relative">
                    <div
                      className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                      aria-hidden="true"
                    >
                      <Mail className="h-4 w-4" />
                    </div>
                    <input
                      id="forgot-email"
                      name="email"
                      type="email"
                      autoComplete="email"
                      required
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        if (emailError) setEmailError(null);
                      }}
                      onBlur={handleBlur}
                      disabled={isSubmitting}
                      aria-invalid={Boolean(emailError)}
                      aria-describedby={emailError ? 'forgot-email-error' : undefined}
                      placeholder="ejemplo@correo.com"
                      className={`w-full pl-10 pr-4 py-3 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                        emailError
                          ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                          : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                      }`}
                    />
                  </div>
                  {emailError && (
                    <p id="forgot-email-error" role="alert" className="mt-1.5 text-xs text-rose-600 font-medium">
                      {emailError}
                    </p>
                  )}
                </div>

                {/* Botón de Enviar */}
                <div className="pt-3">
                  <Button
                    type="submit"
                    variant="primary"
                    size="lg"
                    isLoading={isSubmitting}
                    className="w-full text-base font-bold shadow-md shadow-[#005141]/20 hover:shadow-[#005141]/30 cursor-pointer"
                  >
                    {isSubmitting ? 'Enviando instrucciones...' : 'Enviar enlace de recuperación'}
                  </Button>
                </div>
              </form>

              {/* Enlace alternativo hacia Login */}
              <div className="mt-8 pt-6 border-t border-stone-200 text-center">
                <p className="text-xs sm:text-sm text-stone-600">
                  ¿Recordaste tu contraseña?{' '}
                  <Link
                    href="/login"
                    className="font-bold text-[#005141] hover:underline transition-colors inline-flex items-center gap-1"
                  >
                    Inicia sesión aquí
                  </Link>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
