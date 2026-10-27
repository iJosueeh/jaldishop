'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  Eye,
  EyeOff,
  Lock,
  Mail,
  Store,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  Clock,
  ShieldCheck,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { env } from '@/core/config/env';
import { customerAuthService } from '../services/customerAuthService';
import { getSafeRedirectUrl } from '../utils/safeRedirect';
import { isValidEmail } from '../utils/validation';

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const redirectUrl = getSafeRedirectUrl(searchParams.get('redirect'), '/');
  const registeredSuccess = searchParams.get('registrado') === 'true';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  const validateEmail = (val: string): string | null => {
    const trimmed = val.trim();
    if (!trimmed) {
      return 'Ingresa tu correo electrónico.';
    }
    if (!isValidEmail(trimmed)) {
      return 'Ingresa un correo electrónico con formato válido (ej. usuario@correo.com).';
    }
    return null;
  };

  const validatePassword = (val: string): string | null => {
    if (!val) {
      return 'Ingresa tu contraseña.';
    }
    return null;
  };

  const validate = (): boolean => {
    const errors: { email?: string; password?: string } = {};

    const emailErr = validateEmail(email);
    if (emailErr) errors.email = emailErr;

    const passErr = validatePassword(password);
    if (passErr) errors.password = passErr;

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleBlur = (field: 'email' | 'password') => {
    if (field === 'email') {
      const err = validateEmail(email);
      setFieldErrors((prev) => ({ ...prev, email: err || undefined }));
    } else if (field === 'password') {
      const err = validatePassword(password);
      setFieldErrors((prev) => ({ ...prev, password: err || undefined }));
    }
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    if (!validate() || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await customerAuthService.login({
        email: email.trim().toLowerCase(),
        password,
      });

      if (response.authenticated) {
        await queryClient.invalidateQueries({ queryKey: ['customer-session'] });
        await queryClient.invalidateQueries({ queryKey: ['customer-cart'] });

        toast.success(
          response.fullName
            ? `¡Bienvenido de vuelta, ${response.fullName}!`
            : '¡Sesión iniciada con éxito!'
        );

        router.push(redirectUrl);
        router.refresh();
      } else {
        setErrorMessage(response.message || 'No se pudo iniciar sesión.');
      }
    } catch (err: unknown) {
      const message =
        err instanceof Error
          ? err.message
          : 'Ocurrió un error inesperado al intentar iniciar sesión.';
      setErrorMessage(message);
      toast.error(message);
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
        {/* Glow atmosférico suave */}
        <div
          className="absolute -top-32 -left-32 w-96 h-96 bg-[#005141]/30 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />
        <div
          className="absolute bottom-0 right-0 w-80 h-80 bg-[#36f4a4]/10 rounded-full blur-[100px] pointer-events-none"
          aria-hidden="true"
        />

        {/* Header superior izquierdo: Logo oficial claro y enlace volver */}
        <div className="relative z-10 flex items-center justify-between">
          <Link
            href="/"
            className="inline-block transition-transform hover:scale-105 active:scale-95 duration-150 rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]"
            aria-label="Ir a la página de inicio de JaldiShop"
          >
            <JaldiShopLogo size="md" variant="light" />
          </Link>
          <Link
            href={redirectUrl}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#99b3ad] hover:text-[#36f4a4] transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Volver a la tienda</span>
          </Link>
        </div>

        {/* Contenido central: Propuesta de valor y Demo Card de comercio */}
        <div className="relative z-10 my-auto py-8 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider mb-6">
            <Sparkles className="w-3.5 h-3.5 text-[#36f4a4]" />
            Comercio Local Garantizado
          </div>

          <h2 className="text-3xl xl:text-4xl font-display font-bold text-white tracking-tight leading-tight">
            Pide en tus tiendas favoritas con horario asegurado y cero sobreventa.
          </h2>

          <p className="mt-4 text-sm text-[#99b3ad] leading-relaxed">
            Inicia sesión para recuperar tu carrito guardado, consultar tus pedidos pendientes y coordinar con el comerciante directo por WhatsApp.
          </p>

          {/* Tarjeta destacada de tienda local */}
          <div className="mt-8 p-6 rounded-2xl bg-[#061a1c]/90 border border-[#1e2c31] backdrop-blur-sm shadow-xl shadow-black/50">
            <div className="flex items-center justify-between pb-3.5 border-b border-[#1e2c31]/80">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#005141]/40 border border-[#133b32] flex items-center justify-center text-[#36f4a4]">
                  <Store className="w-5 h-5" />
                </div>
                <div>
                  <span className="text-xs font-bold text-white block">Panadería Artesanal San Pedro</span>
                  <span className="text-[11px] text-[#99b3ad]">Miraflores, Lima</span>
                </div>
              </div>
              <span className="text-[11px] font-semibold text-[#36f4a4] bg-[#005141]/30 border border-[#133b32] px-2.5 py-1 rounded-full flex items-center gap-1.5">
                <Clock className="w-3 h-3 text-[#36f4a4]" /> Turno 10:30 AM
              </span>
            </div>
            <div className="pt-3.5 flex items-center justify-between text-xs text-[#99b3ad]">
              <span>1x Pan de Masa Madre + 2x Croissants</span>
              <span className="font-bold text-white text-sm">S/ 24.50</span>
            </div>
          </div>
        </div>

        {/* Footer inferior izquierdo: Garantías de confianza */}
        <div className="relative z-10 pt-6 border-t border-[#1e2c31]/60 flex items-center gap-6 text-xs text-[#99b3ad]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
            <span>Compra 100% segura</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-[#36f4a4]" />
            <span>Sin sobreventa ni cobros a ciegas</span>
          </div>
        </div>
      </div>

      {/* =========================================================================
          PANEL DERECHO: FORMULARIO CENTRADO EN MÓVIL Y ESCRITORIO (#fbfcfb)
          ========================================================================= */}
      <div className="w-full lg:w-1/2 min-h-screen flex flex-col justify-center items-center p-6 sm:p-10 lg:p-16 bg-[#fbfcfb]">
        <div className="w-full max-w-md my-auto">
          {/* Cabecera para móvil con Logo y Volver */}
          <div className="flex items-center justify-between mb-8 lg:hidden">
            <Link href="/" aria-label="JaldiShop">
              <JaldiShopLogo size="sm" variant="dark" />
            </Link>
            <Link
              href={redirectUrl}
              className="inline-flex items-center gap-1 text-xs font-semibold text-stone-600 hover:text-[#005141]"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Volver</span>
            </Link>
          </div>

          {/* Título de la vista de autenticación */}
          <div className="mb-8">
            <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
              Bienvenido de vuelta
            </h1>
            <p className="mt-2 text-sm text-stone-600">
              Ingresa tu correo y contraseña para acceder a tus pedidos.
            </p>
          </div>

          {/* Banner de éxito tras registro */}
          {registeredSuccess && (
            <div
              role="status"
              className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-start gap-2.5"
            >
              <div className="w-2 h-2 rounded-full bg-emerald-600 mt-1 shrink-0" />
              <span>
                ¡Tu cuenta fue creada con éxito! Ingresa tus credenciales para continuar con tus compras.
              </span>
            </div>
          )}

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

          {/* Formulario principal */}
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Campo Correo */}
            <div>
              <label
                htmlFor="email"
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
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="username"
                  required
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (fieldErrors.email) {
                      setFieldErrors((prev) => ({ ...prev, email: undefined }));
                    }
                  }}
                  onBlur={() => handleBlur('email')}
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.email)}
                  aria-describedby={fieldErrors.email ? 'email-error' : undefined}
                  placeholder="ejemplo@correo.com"
                  className={`w-full pl-10 pr-4 py-3 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                    fieldErrors.email
                      ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                      : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                  }`}
                />
              </div>
              {fieldErrors.email && (
                <p id="email-error" role="alert" className="mt-1.5 text-xs text-rose-600 font-medium">
                  {fieldErrors.email}
                </p>
              )}
            </div>

            {/* Campo Contraseña */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label
                  htmlFor="password"
                  className="block text-xs font-semibold uppercase tracking-wider text-stone-700"
                >
                  Contraseña
                </label>
              </div>
              <div className="relative">
                <div
                  className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                  aria-hidden="true"
                >
                  <Lock className="h-4 w-4" />
                </div>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (fieldErrors.password) {
                      setFieldErrors((prev) => ({ ...prev, password: undefined }));
                    }
                  }}
                  onBlur={() => handleBlur('password')}
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.password)}
                  aria-describedby={fieldErrors.password ? 'password-error' : undefined}
                  placeholder="Ingresa tu contraseña"
                  className={`w-full pl-10 pr-11 py-3 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                    fieldErrors.password
                      ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                      : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-stone-400 hover:text-stone-700 focus:text-[#005141] focus:outline-none transition-colors cursor-pointer"
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.password && (
                <p id="password-error" role="alert" className="mt-1.5 text-xs text-rose-600 font-medium">
                  {fieldErrors.password}
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
                {isSubmitting ? 'Iniciando sesión...' : 'Iniciar Sesión'}
              </Button>
            </div>
          </form>

          {/* Enlace alternativo hacia Registro */}
          <div className="mt-8 pt-6 border-t border-stone-200 text-center">
            <p className="text-xs sm:text-sm text-stone-600">
              ¿Aún no tienes una cuenta?{' '}
              <Link
                href={`/registro${searchParams.get('redirect') ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                className="font-bold text-[#005141] hover:underline transition-colors inline-flex items-center gap-1"
              >
                Regístrate aquí
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </p>
          </div>

          {/* Enlace discreto para comerciantes */}
          <div className="mt-8 text-center">
            <a
              href={env.merchantUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs text-stone-500 hover:text-stone-800 hover:bg-stone-100 border border-transparent hover:border-stone-200 transition-all group"
            >
              <Store className="w-3.5 h-3.5 text-[#ea580c] group-hover:scale-110 transition-transform" />
              <span>¿Tienes un negocio o tienda local? Accede al portal MYPE ↗</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
