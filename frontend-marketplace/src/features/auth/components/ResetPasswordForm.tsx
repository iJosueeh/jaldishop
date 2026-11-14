'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import {
  Lock,
  Eye,
  EyeOff,
  ArrowLeft,
  AlertCircle,
  CheckCircle2,
  ShieldCheck,
  KeyRound,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { ApiException } from '@/core/api/apiClient';
import { customerAuthService } from '../services/customerAuthService';
import {
  isValidResetToken,
  validateResetPassword,
  validateConfirmPassword,
} from '../utils/validation';

/**
 * Token Security Notes:
 * 1. The token is extracted from the URL query parameter (?token=...) upon entry.
 * 2. On component mount, the address bar is cleaned via `window.history.replaceState`.
 *    NOTE: While `replaceState` removes the token from the browser address bar, UI and
 *    subsequent navigations (mitigating referrer leaks, shoulder surfing, and browser history persistence),
 *    it CANNOT erase records of already-completed HTTP requests from network/CDN access logs
 *    or server access logs of the initial GET request.
 * 3. Token is kept exclusively in transient React component memory; never stored in localStorage,
 *    sessionStorage, cookies, or sent to telemetry/analytics.
 * 4. The page route enforces `Referrer-Policy: no-referrer` in Next.js page metadata.
 */
export function ResetPasswordForm() {
  const searchParams = useSearchParams();

  // Extract token from URL once on mount, then sanitize the address bar immediately
  const [token, setToken] = useState<string | null>(() => {
    const param = searchParams.get('token');
    return param ? param.trim() : null;
  });

  useEffect(() => {
    // Sanitize address bar to prevent token leakage via screenshots, history, or referrers
    if (typeof window !== 'undefined' && window.location.search.includes('token')) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isTokenInvalid, setIsTokenInvalid] = useState(() => !isValidResetToken(token));
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    newPassword?: string;
    confirmPassword?: string;
  }>({});

  const validate = (): boolean => {
    const errors: { newPassword?: string; confirmPassword?: string } = {};

    const passErr = validateResetPassword(newPassword);
    if (passErr) errors.newPassword = passErr;

    const confirmErr = validateConfirmPassword(newPassword, confirmPassword);
    if (confirmErr) errors.confirmPassword = confirmErr;

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleBlur = (field: 'newPassword' | 'confirmPassword') => {
    if (field === 'newPassword') {
      const err = validateResetPassword(newPassword);
      setFieldErrors((prev) => ({ ...prev, newPassword: err || undefined }));
    } else if (field === 'confirmPassword') {
      const err = validateConfirmPassword(newPassword, confirmPassword);
      setFieldErrors((prev) => ({ ...prev, confirmPassword: err || undefined }));
    }
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    if (!token || !isValidResetToken(token)) {
      setIsTokenInvalid(true);
      return;
    }

    if (!validate() || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      await customerAuthService.resetPassword({
        token,
        newPassword,
      });
      setIsSuccess(true);
      // Clear token and password state in memory
      setToken(null);
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      if (err instanceof ApiException) {
        if (
          err.status === 422 &&
          (err.errorData?.code === 'INVALID_PASSWORD_RESET_TOKEN' ||
            err.errorData?.code === undefined)
        ) {
          setIsTokenInvalid(true);
          return;
        }

        if (err.status === 400) {
          setErrorMessage(
            err.errorData?.message ||
              err.message ||
              'Los datos enviados no son válidos. Verifica los requisitos de la contraseña.'
          );
          return;
        }

        if (err.status === 429) {
          setErrorMessage(
            err.errorData?.message ||
              'Demasiados intentos. Por favor, espera unos minutos antes de reintentar.'
          );
          return;
        }

        if (err.status === 503 || err.status === 408) {
          setErrorMessage(
            'El servicio no se encuentra disponible temporalmente. Conserva tus datos y vuelve a intentarlo en unos instantes.'
          );
          return;
        }

        setErrorMessage(
          err.errorData?.message ||
            err.message ||
            'Ocurrió un error al procesar tu solicitud.'
        );
        return;
      }

      const message =
        err instanceof Error
          ? err.message
          : 'Ocurrió un error inesperado al actualizar la contraseña.';
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
            <span>Volver a iniciar sesión</span>
          </Link>
        </div>

        <div className="relative z-10 my-auto py-8 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider mb-6">
            <Sparkles className="w-3.5 h-3.5 text-[#36f4a4]" />
            Nueva Credencial
          </div>

          <h2 className="text-3xl xl:text-4xl font-display font-bold text-white tracking-tight leading-tight">
            Crea una contraseña segura para tu cuenta.
          </h2>

          <p className="mt-4 text-sm text-[#99b3ad] leading-relaxed">
            Al cambiar tu contraseña, cualquier sesión abierta anteriormente en otros navegadores o dispositivos se cerrará de forma automática.
          </p>

          <div className="mt-8 p-6 rounded-2xl bg-[#061a1c]/90 border border-[#1e2c31] backdrop-blur-sm shadow-xl shadow-black/50 space-y-3">
            <div className="flex items-center gap-2 text-xs font-bold text-white">
              <KeyRound className="w-4 h-4 text-[#36f4a4]" />
              <span>Requisitos de seguridad</span>
            </div>
            <ul className="text-xs text-[#99b3ad] space-y-1.5 list-disc list-inside">
              <li>Mínimo 8 caracteres de longitud.</li>
              <li>Máximo 72 caracteres UTF-8.</li>
              <li>Confirmación idéntica en ambos campos.</li>
            </ul>
          </div>
        </div>

        <div className="relative z-10 pt-6 border-t border-[#1e2c31]/60 flex items-center gap-6 text-xs text-[#99b3ad]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
            <span>Cierre de sesiones previas</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-[#36f4a4]" />
            <span>Encriptación con algoritmo seguro</span>
          </div>
        </div>
      </div>

      {/* =========================================================================
          PANEL DERECHO: FORMULARIO CENTRADO EN MÓVIL Y ESCRITORIO
          ========================================================================= */}
      <div className="w-full lg:w-1/2 min-h-screen flex flex-col justify-center items-center p-6 sm:p-10 lg:p-16 bg-[#fbfcfb]">
        <div className="w-full max-w-md my-auto">
          {/* Header móvil */}
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

          {isSuccess ? (
            /* =========================================================================
               ESTADO DE ÉXITO
               ========================================================================= */
            <div className="space-y-6 text-center animate-fadeIn" data-testid="reset-password-success">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-[#005141]">
                <CheckCircle2 className="w-8 h-8 text-[#005141]" />
              </div>

              <div>
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  ¡Contraseña actualizada!
                </h1>
                <p className="mt-3 text-sm text-stone-600 leading-relaxed">
                  Tu contraseña ha sido modificada con éxito. Por seguridad, tus sesiones anteriores han sido invalidadas.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-stone-50 border border-stone-200 text-left text-xs text-stone-600">
                <p>Ya puedes iniciar sesión con tu nueva contraseña para seguir comprando en tus tiendas locales favoritas.</p>
              </div>

              <div className="pt-2">
                <Link
                  href="/login"
                  className="w-full inline-flex justify-center items-center py-3 px-4 rounded-xl font-bold text-sm bg-[#005141] text-white hover:bg-[#00382e] shadow-md shadow-[#005141]/20 transition-all cursor-pointer"
                >
                  Iniciar sesión con nueva contraseña
                </Link>
              </div>
            </div>
          ) : isTokenInvalid ? (
            /* =========================================================================
               ESTADO DE TOKEN INVÁLIDO O VENCIDO (422)
               ========================================================================= */
            <div className="space-y-6 text-center animate-fadeIn" data-testid="reset-password-invalid-token">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-600">
                <AlertCircle className="w-8 h-8 text-amber-600" />
              </div>

              <div>
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  Enlace vencido o inválido
                </h1>
                <p className="mt-3 text-sm text-stone-600 leading-relaxed">
                  El enlace para restablecer tu contraseña no es válido, ha caducado (vence a los 15 minutos) o ya fue utilizado anteriormente.
                </p>
              </div>

              <div className="pt-2 flex flex-col gap-3">
                <Link
                  href="/forgot-password"
                  className="w-full inline-flex justify-center items-center py-3 px-4 rounded-xl font-bold text-sm bg-[#005141] text-white hover:bg-[#00382e] shadow-md shadow-[#005141]/20 transition-all cursor-pointer"
                >
                  Solicitar un nuevo enlace
                </Link>
                <Link
                  href="/login"
                  className="text-xs font-semibold text-stone-500 hover:text-stone-800 hover:underline transition-colors py-2"
                >
                  Volver al inicio de sesión
                </Link>
              </div>
            </div>
          ) : (
            /* =========================================================================
               FORMULARIO PRINCIPAL DE RESET
               ========================================================================= */
            <div>
              <div className="mb-8">
                <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
                  Restablecer contraseña
                </h1>
                <p className="mt-2 text-sm text-stone-600">
                  Ingresa tu nueva contraseña para acceder a tu cuenta de JaldiShop.
                </p>
              </div>

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
                {/* Campo Nueva Contraseña */}
                <div>
                  <label
                    htmlFor="new-password"
                    className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                  >
                    Nueva contraseña
                  </label>
                  <div className="relative">
                    <div
                      className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                      aria-hidden="true"
                    >
                      <Lock className="h-4 w-4" />
                    </div>
                    <input
                      id="new-password"
                      name="newPassword"
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      required
                      value={newPassword}
                      onChange={(e) => {
                        setNewPassword(e.target.value);
                        if (fieldErrors.newPassword) {
                          setFieldErrors((prev) => ({ ...prev, newPassword: undefined }));
                        }
                      }}
                      onBlur={() => handleBlur('newPassword')}
                      disabled={isSubmitting}
                      aria-invalid={Boolean(fieldErrors.newPassword)}
                      aria-describedby={fieldErrors.newPassword ? 'new-password-error' : undefined}
                      placeholder="Mínimo 8 caracteres"
                      className={`w-full pl-10 pr-11 py-3 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                        fieldErrors.newPassword
                          ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                          : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-stone-400 hover:text-stone-700 focus:text-[#005141] focus:outline-none transition-colors cursor-pointer"
                      aria-label={showPassword ? 'Ocultar nueva contraseña' : 'Mostrar nueva contraseña'}
                      aria-pressed={showPassword}
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {fieldErrors.newPassword && (
                    <p id="new-password-error" role="alert" className="mt-1.5 text-xs text-rose-600 font-medium">
                      {fieldErrors.newPassword}
                    </p>
                  )}
                </div>

                {/* Campo Confirmar Contraseña */}
                <div>
                  <label
                    htmlFor="confirm-password"
                    className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                  >
                    Confirmar nueva contraseña
                  </label>
                  <div className="relative">
                    <div
                      className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                      aria-hidden="true"
                    >
                      <Lock className="h-4 w-4" />
                    </div>
                    <input
                      id="confirm-password"
                      name="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      required
                      value={confirmPassword}
                      onChange={(e) => {
                        setConfirmPassword(e.target.value);
                        if (fieldErrors.confirmPassword) {
                          setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                        }
                      }}
                      onBlur={() => handleBlur('confirmPassword')}
                      disabled={isSubmitting}
                      aria-invalid={Boolean(fieldErrors.confirmPassword)}
                      aria-describedby={fieldErrors.confirmPassword ? 'confirm-password-error' : undefined}
                      placeholder="Repite tu contraseña"
                      className={`w-full pl-10 pr-11 py-3 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                        fieldErrors.confirmPassword
                          ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                          : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((prev) => !prev)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-stone-400 hover:text-stone-700 focus:text-[#005141] focus:outline-none transition-colors cursor-pointer"
                      aria-label={showConfirmPassword ? 'Ocultar confirmación' : 'Mostrar confirmación'}
                      aria-pressed={showConfirmPassword}
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {fieldErrors.confirmPassword && (
                    <p id="confirm-password-error" role="alert" className="mt-1.5 text-xs text-rose-600 font-medium">
                      {fieldErrors.confirmPassword}
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
                    {isSubmitting ? 'Actualizando contraseña...' : 'Actualizar contraseña'}
                  </Button>
                </div>
              </form>

              <div className="mt-8 pt-6 border-t border-stone-200 text-center">
                <Link
                  href="/login"
                  className="font-semibold text-xs text-stone-500 hover:text-stone-800 hover:underline transition-colors inline-flex items-center gap-1"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
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
