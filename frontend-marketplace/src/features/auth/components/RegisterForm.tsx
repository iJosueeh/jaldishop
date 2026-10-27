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
  User,
  Phone,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  Check,
  ShoppingBag,
  Clock,
  Sparkles,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { customerAuthService } from '../services/customerAuthService';
import { getSafeRedirectUrl } from '../utils/safeRedirect';
import {
  normalizePhone,
  validateFirstName,
  validateLastName,
  validateEmailField,
  validateRegisterPassword,
  validateConfirmPassword,
  validatePhoneField,
} from '../utils/validation';

export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const redirectUrl = getSafeRedirectUrl(searchParams.get('redirect'), '/');
  const isFromStore = redirectUrl.startsWith('/tienda/');

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    password?: string;
    confirmPassword?: string;
  }>({});

  const [touched, setTouched] = useState<{
    firstName?: boolean;
    lastName?: boolean;
    email?: boolean;
    phone?: boolean;
    password?: boolean;
    confirmPassword?: boolean;
  }>({});

  const validateAll = (): boolean => {
    const errors: typeof fieldErrors = {};

    const firstNameErr = validateFirstName(firstName);
    if (firstNameErr) errors.firstName = firstNameErr;

    const lastNameErr = validateLastName(lastName);
    if (lastNameErr) errors.lastName = lastNameErr;

    const emailErr = validateEmailField(email);
    if (emailErr) errors.email = emailErr;

    const phoneErr = validatePhoneField(phone);
    if (phoneErr) errors.phone = phoneErr;

    const passwordErr = validateRegisterPassword(password);
    if (passwordErr) errors.password = passwordErr;

    const confirmErr = validateConfirmPassword(password, confirmPassword);
    if (confirmErr) errors.confirmPassword = confirmErr;

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleBlur = (field: keyof typeof touched) => {
    setTouched((prev) => ({ ...prev, [field]: true }));

    switch (field) {
      case 'firstName': {
        const err = validateFirstName(firstName);
        setFieldErrors((prev) => ({ ...prev, firstName: err || undefined }));
        break;
      }
      case 'lastName': {
        const err = validateLastName(lastName);
        setFieldErrors((prev) => ({ ...prev, lastName: err || undefined }));
        break;
      }
      case 'email': {
        const err = validateEmailField(email);
        setFieldErrors((prev) => ({ ...prev, email: err || undefined }));
        break;
      }
      case 'phone': {
        const err = validatePhoneField(phone);
        setFieldErrors((prev) => ({ ...prev, phone: err || undefined }));
        break;
      }
      case 'password': {
        const err = validateRegisterPassword(password);
        setFieldErrors((prev) => ({ ...prev, password: err || undefined }));
        break;
      }
      case 'confirmPassword': {
        const err = validateConfirmPassword(password, confirmPassword);
        setFieldErrors((prev) => ({ ...prev, confirmPassword: err || undefined }));
        break;
      }
    }
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setGeneralError(null);

    // Marcar todos como tocados al intentar enviar
    setTouched({
      firstName: true,
      lastName: true,
      email: true,
      phone: true,
      password: true,
      confirmPassword: true,
    });

    if (!validateAll() || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await customerAuthService.register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim() ? normalizePhone(phone.trim()) : undefined,
        password,
      });

      if (response.autoLogin) {
        await queryClient.invalidateQueries({ queryKey: ['customer-session'] });
        await queryClient.invalidateQueries({ queryKey: ['customer-cart'] });

        toast.success(
          response.fullName
            ? `¡Bienvenido a JaldiShop, ${response.fullName}!`
            : '¡Cuenta creada con éxito!'
        );

        router.push(redirectUrl);
        router.refresh();
      } else {
        toast.info('Tu cuenta fue creada con éxito. Por favor inicia sesión.');
        router.push(
          `/login?registrado=true&redirect=${encodeURIComponent(redirectUrl)}`
        );
      }
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'errorData' in err) {
        const errorData = (
          err as {
            errorData?: {
              code?: string;
              message?: string;
              errors?: Record<string, string>;
            };
          }
        ).errorData;
        if (errorData?.code === 'EMAIL_ALREADY_EXISTS') {
          setFieldErrors((prev) => ({
            ...prev,
            email: 'Este correo ya se encuentra registrado.',
          }));
          setGeneralError(
            'El correo ya está registrado. Si ya tienes cuenta, por favor inicia sesión.'
          );
        } else if (errorData?.errors) {
          setFieldErrors((prev) => ({ ...prev, ...errorData.errors }));
          setGeneralError(
            errorData.message || 'Por favor corrige los campos señalados.'
          );
        } else {
          setGeneralError(
            errorData?.message || 'No se pudo completar el registro.'
          );
        }
      } else {
        const message =
          err instanceof Error
            ? err.message
            : 'Ocurrió un error inesperado al procesar el registro.';
        setGeneralError(message);
        toast.error(message);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const isPasswordLengthValid = password.length >= 6;
  const doPasswordsMatch =
    password.length >= 6 &&
    confirmPassword.length > 0 &&
    password === confirmPassword;

  return (
    <div className="w-full min-h-screen flex flex-col lg:flex-row bg-[#fbfcfb]">
      {/* =========================================================================
          PANEL IZQUIERDO (SPLIT-SCREEN): VERDE GREENHOUSE INSTITUCIONAL (DESKTOP)
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

        {/* Header superior izquierdo */}
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

        {/* Contenido de valor de registro */}
        <div className="relative z-10 my-auto py-8 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#072720] border border-[#133b32] text-[#36f4a4] text-xs font-bold uppercase tracking-wider mb-6">
            <Sparkles className="w-3.5 h-3.5 text-[#36f4a4]" />
            Cuenta de Cliente JaldiShop
          </div>

          <h2 className="text-3xl xl:text-4xl font-display font-bold text-white tracking-tight leading-tight">
            Únete y compra en los mejores comercios de tu ciudad sin esperas.
          </h2>

          <p className="mt-4 text-sm text-[#99b3ad] leading-relaxed">
            Una sola cuenta para pedir en pastelerías artesanales, talleres florales, cafeterías y negocios independientes cerca de ti.
          </p>

          {/* Tarjeta de Beneficios Clave */}
          <div className="mt-8 space-y-4 p-6 rounded-2xl bg-[#061a1c]/90 border border-[#1e2c31] backdrop-blur-sm shadow-xl shadow-black/50">
            <div className="flex items-start gap-3.5">
              <div className="w-8 h-8 rounded-xl bg-[#005141]/50 text-[#36f4a4] flex items-center justify-center shrink-0 mt-0.5">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <div>
                <span className="text-xs font-bold text-white block">Tus carritos nunca se pierden</span>
                <span className="text-[11px] text-[#99b3ad]">Agrega productos hoy y retoma tu pedido cuando quieras.</span>
              </div>
            </div>

            <div className="flex items-start gap-3.5 pt-3 border-t border-[#1e2c31]/60">
              <div className="w-8 h-8 rounded-xl bg-[#005141]/50 text-[#36f4a4] flex items-center justify-center shrink-0 mt-0.5">
                <Clock className="w-4 h-4" />
              </div>
              <div>
                <span className="text-xs font-bold text-white block">Turnos de retiro sin colas</span>
                <span className="text-[11px] text-[#99b3ad]">Elige tu horario de recojo exacto y ahorra tiempo todos los días.</span>
              </div>
            </div>
          </div>
        </div>

        {/* Footer inferior izquierdo */}
        <div className="relative z-10 pt-6 border-t border-[#1e2c31]/60 flex items-center gap-6 text-xs text-[#99b3ad]">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#36f4a4]" />
            <span>Datos protegidos con cifrado bancario</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Check className="w-4 h-4 text-[#36f4a4]" />
            <span>Sin cobros ocultos</span>
          </div>
        </div>
      </div>

      {/* =========================================================================
          PANEL DERECHO: FORMULARIO CENTRADO EN MÓVIL Y ESCRITORIO (#fbfcfb)
          ========================================================================= */}
      <div className="w-full lg:w-1/2 min-h-screen flex flex-col justify-center items-center p-6 sm:p-10 lg:p-16 bg-[#fbfcfb]">
        <div className="w-full max-w-md my-auto">
          {/* Cabecera móvil con Logo y Volver */}
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

          {/* Contexto de la tienda si viene de una tienda */}
          {isFromStore && (
            <div className="mb-6 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-[#005141] shrink-0" />
              <span>Crea tu cuenta para continuar con tu pedido en esta tienda.</span>
            </div>
          )}

          {/* Título de la vista */}
          <div className="mb-8">
            <h1 className="text-2xl sm:text-3xl font-display font-bold text-[#1c1917] tracking-tight">
              Crea tu cuenta de cliente
            </h1>
            <p className="mt-2 text-sm text-stone-600">
              Completa tus datos para comprar en tus comercios locales favoritos.
            </p>
          </div>

          {/* Banner de error general */}
          {generalError && (
            <div
              role="alert"
              className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5 animate-fadeIn"
            >
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span className="flex-1 font-medium">{generalError}</span>
            </div>
          )}

          {/* Formulario */}
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Nombres y Apellidos en dos columnas */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label
                  htmlFor="firstName"
                  className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                >
                  Nombres
                </label>
                <div className="relative">
                  <div
                    className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                    aria-hidden="true"
                  >
                    <User className="h-4 w-4" />
                  </div>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    autoComplete="given-name"
                    required
                    value={firstName}
                    onChange={(e) => {
                      setFirstName(e.target.value);
                      if (fieldErrors.firstName) {
                        setFieldErrors((prev) => ({ ...prev, firstName: undefined }));
                      }
                    }}
                    onBlur={() => handleBlur('firstName')}
                    disabled={isSubmitting}
                    aria-invalid={Boolean(fieldErrors.firstName)}
                    aria-describedby={fieldErrors.firstName ? 'firstName-error' : undefined}
                    placeholder="Juan"
                    className={`w-full pl-10 pr-3 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                      fieldErrors.firstName
                        ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                        : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                    }`}
                  />
                </div>
                {fieldErrors.firstName && (
                  <p id="firstName-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                    {fieldErrors.firstName}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="lastName"
                  className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
                >
                  Apellidos
                </label>
                <div className="relative">
                  <div
                    className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                    aria-hidden="true"
                  >
                    <User className="h-4 w-4" />
                  </div>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    autoComplete="family-name"
                    required
                    value={lastName}
                    onChange={(e) => {
                      setLastName(e.target.value);
                      if (fieldErrors.lastName) {
                        setFieldErrors((prev) => ({ ...prev, lastName: undefined }));
                      }
                    }}
                    onBlur={() => handleBlur('lastName')}
                    disabled={isSubmitting}
                    aria-invalid={Boolean(fieldErrors.lastName)}
                    aria-describedby={fieldErrors.lastName ? 'lastName-error' : undefined}
                    placeholder="Pérez"
                    className={`w-full pl-10 pr-3 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                      fieldErrors.lastName
                        ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                        : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                    }`}
                  />
                </div>
                {fieldErrors.lastName && (
                  <p id="lastName-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                    {fieldErrors.lastName}
                  </p>
                )}
              </div>
            </div>

            {/* Email */}
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
                  autoComplete="email"
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
                  className={`w-full pl-10 pr-4 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                    fieldErrors.email
                      ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                      : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                  }`}
                />
              </div>
              {fieldErrors.email && (
                <p id="email-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                  {fieldErrors.email}
                </p>
              )}
            </div>

            {/* Celular con prefijo visual de Perú */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label
                  htmlFor="phone"
                  className="block text-xs font-semibold uppercase tracking-wider text-stone-700"
                >
                  Celular <span className="text-stone-500 font-normal lowercase">(opcional)</span>
                </label>
                <span className="text-[11px] text-stone-500">Para avisos de entrega por WhatsApp</span>
              </div>
              <div
                className={`relative flex rounded-xl border bg-white transition-all overflow-hidden ${
                  fieldErrors.phone
                    ? 'border-rose-400 focus-within:border-rose-500 focus-within:ring-1 focus-within:ring-rose-400/40'
                    : 'border-stone-300 hover:border-stone-400 focus-within:border-[#005141] focus-within:ring-1 focus-within:ring-[#005141]'
                }`}
              >
                <div className="flex items-center gap-1.5 px-3 bg-stone-50 border-r border-stone-200 text-stone-600 select-none shrink-0 text-xs font-bold">
                  <Phone className="h-3.5 w-3.5 text-stone-400" />
                  <span>+51</span>
                </div>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  autoComplete="tel"
                  value={phone}
                  onChange={(e) => {
                    setPhone(e.target.value);
                    if (fieldErrors.phone) {
                      setFieldErrors((prev) => ({ ...prev, phone: undefined }));
                    }
                  }}
                  onBlur={() => handleBlur('phone')}
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.phone)}
                  aria-describedby={fieldErrors.phone ? 'phone-error' : undefined}
                  placeholder="912 345 678"
                  className="w-full px-3.5 py-2.5 bg-transparent text-sm text-stone-900 placeholder-stone-400 focus:outline-none"
                />
              </div>
              {fieldErrors.phone && (
                <p id="phone-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                  {fieldErrors.phone}
                </p>
              )}
            </div>

            {/* Contraseña */}
            <div>
              <label
                htmlFor="password"
                className="block text-xs font-semibold uppercase tracking-wider text-stone-700 mb-1.5"
              >
                Contraseña
              </label>
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
                  autoComplete="new-password"
                  required
                  value={password}
                  onChange={(e) => {
                    const newPass = e.target.value;
                    setPassword(newPass);
                    if (fieldErrors.password) {
                      setFieldErrors((prev) => ({ ...prev, password: undefined }));
                    }
                    if (confirmPassword) {
                      if (newPass === confirmPassword) {
                        setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                      } else if (touched.confirmPassword || confirmPassword.length >= newPass.length) {
                        setFieldErrors((prev) => ({ ...prev, confirmPassword: 'Las contraseñas no coinciden.' }));
                      }
                    }
                  }}
                  onBlur={() => handleBlur('password')}
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.password)}
                  aria-describedby="password-rules"
                  placeholder="Mínimo 6 caracteres"
                  className={`w-full pl-10 pr-11 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
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
              <div id="password-rules" className="mt-1.5 flex items-center gap-1.5 text-xs text-stone-500">
                <span
                  className={`inline-flex items-center justify-center w-3.5 h-3.5 rounded-full text-[10px] ${
                    isPasswordLengthValid ? 'bg-emerald-100 text-[#005141]' : 'bg-stone-200 text-stone-400'
                  }`}
                >
                  <Check className="w-2.5 h-2.5" />
                </span>
                <span className={isPasswordLengthValid ? 'text-[#005141] font-medium' : 'text-stone-500'}>
                  Al menos 6 caracteres
                </span>
              </div>
              {fieldErrors.password && (
                <p role="alert" className="mt-1 text-xs text-rose-600 font-medium">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            {/* Confirmar Contraseña */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label
                  htmlFor="confirmPassword"
                  className="block text-xs font-semibold uppercase tracking-wider text-stone-700"
                >
                  Confirmar contraseña
                </label>
                {doPasswordsMatch && (
                  <span className="text-[11px] font-semibold text-[#005141] flex items-center gap-1">
                    <Check className="w-3 h-3" /> Las contraseñas coinciden
                  </span>
                )}
              </div>
              <div className="relative">
                <div
                  className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400"
                  aria-hidden="true"
                >
                  <Lock className="h-4 w-4" />
                </div>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  required
                  value={confirmPassword}
                  onChange={(e) => {
                    const newConfirm = e.target.value;
                    setConfirmPassword(newConfirm);
                    if (newConfirm === password) {
                      setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                    } else if (fieldErrors.confirmPassword || (newConfirm.length >= password.length && password.length > 0)) {
                      setFieldErrors((prev) => ({ ...prev, confirmPassword: 'Las contraseñas no coinciden.' }));
                    }
                  }}
                  onBlur={() => handleBlur('confirmPassword')}
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.confirmPassword)}
                  aria-describedby={fieldErrors.confirmPassword ? 'confirmPassword-error' : undefined}
                  placeholder="Repite tu contraseña"
                  className={`w-full pl-10 pr-11 py-2.5 bg-white border rounded-xl text-sm text-stone-900 placeholder-stone-400 transition-all duration-150 focus:outline-none ${
                    fieldErrors.confirmPassword
                      ? 'border-rose-400 focus:border-rose-500 focus:ring-1 focus:ring-rose-400/40'
                      : 'border-stone-300 hover:border-stone-400 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-stone-400 hover:text-stone-700 focus:text-[#005141] focus:outline-none transition-colors cursor-pointer"
                  aria-label={showConfirmPassword ? 'Ocultar confirmación de contraseña' : 'Mostrar confirmación de contraseña'}
                  aria-pressed={showConfirmPassword}
                >
                  {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.confirmPassword && (
                <p id="confirmPassword-error" role="alert" className="mt-1 text-xs text-rose-600 font-medium">
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
                {isSubmitting ? 'Creando tu cuenta...' : 'Crear mi cuenta'}
              </Button>
            </div>
          </form>

          {/* Enlace alternativo a Login */}
          <div className="mt-8 pt-6 border-t border-stone-200 text-center">
            <p className="text-xs sm:text-sm text-stone-600">
              ¿Ya tienes una cuenta de cliente?{' '}
              <Link
                href={`/login${searchParams.get('redirect') ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                className="font-bold text-[#005141] hover:underline transition-colors inline-flex items-center gap-1"
              >
                Inicia sesión aquí
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
