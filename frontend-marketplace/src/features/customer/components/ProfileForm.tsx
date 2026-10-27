'use client';

import React, { useState, useEffect } from 'react';
import { useCustomerProfile, useUpdateCustomerProfile } from '../hooks/useCustomerProfile';
import {
  validateFirstName,
  validateLastName,
  validatePhoneField,
  normalizePhone,
} from '@/features/auth/utils/validation';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { toast } from 'sonner';
import { User, Mail, Phone, Calendar, ShieldCheck, CheckCircle2 } from 'lucide-react';

export function ProfileForm() {
  const { data: profile, isLoading, isError, refetch } = useCustomerProfile();
  const updateMutation = useUpdateCustomerProfile();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [errors, setErrors] = useState<{ firstName?: string; lastName?: string; phone?: string }>({});
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (profile) {
      setFirstName(profile.firstName || '');
      setLastName(profile.lastName || '');
      setPhone(profile.phone || '');
    }
  }, [profile]);

  if (isLoading) {
    return (
      <div className="space-y-6 w-full">
        <div className="p-8 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-6">
          <Skeleton className="h-6 w-48 bg-stone-200" />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Skeleton className="h-10 w-full bg-stone-200" />
            <Skeleton className="h-10 w-full bg-stone-200" />
          </div>
          <Skeleton className="h-10 w-full bg-stone-200" />
        </div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="p-8 rounded-2xl bg-white border border-rose-200 text-center max-w-xl mx-auto shadow-xs">
        <p className="text-sm text-rose-600 font-semibold mb-3">
          No pudimos cargar la información de tu perfil.
        </p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          Reintentar
        </Button>
      </div>
    );
  }

  const validate = () => {
    const errs: { firstName?: string; lastName?: string; phone?: string } = {};

    const nameErr = validateFirstName(firstName);
    if (nameErr) errs.firstName = nameErr;

    const lastNameErr = validateLastName(lastName);
    if (lastNameErr) errs.lastName = lastNameErr;

    const phoneErr = validatePhoneField(phone);
    if (phoneErr) errs.phone = phoneErr;

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSuccess(false);

    if (!validate()) return;

    try {
      await updateMutation.mutateAsync({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone.trim() ? normalizePhone(phone.trim()) : null,
      });

      setIsSuccess(true);
      toast.success('Perfil actualizado correctamente.');
      setTimeout(() => setIsSuccess(false), 4000);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'No se pudo actualizar el perfil. Inténtalo de nuevo.';
      toast.error(message);
    }
  };

  const formattedDate = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString('es-PE', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    : 'No registrada';

  return (
    <form onSubmit={handleSubmit} noValidate className="w-full">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Columna Izquierda: Formulario de Datos Personales (2 Columnas) */}
        <div className="lg:col-span-2 p-6 sm:p-8 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 pb-4">
            <div>
              <h2 className="text-lg sm:text-xl font-display font-bold text-stone-900 flex items-center gap-2">
                <User className="w-5 h-5 text-[#005141]" />
                <span>Datos Personales</span>
              </h2>
              <p className="text-xs text-stone-500 mt-0.5">
                Esta información se utiliza para coordinar tus compras y la entrega de pedidos.
              </p>
            </div>
            <Badge variant="jade" size="sm" className="font-bold">
              Cuenta de Cliente
            </Badge>
          </div>

          {/* Nombres y Apellidos */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-xs font-bold text-stone-700 mb-1.5">
                Nombres <span className="text-[#005141]">*</span>
              </label>
              <input
                id="firstName"
                type="text"
                value={firstName}
                onChange={(e) => {
                  setFirstName(e.target.value);
                  if (errors.firstName) setErrors((prev) => ({ ...prev, firstName: undefined }));
                }}
                aria-invalid={Boolean(errors.firstName)}
                aria-describedby={errors.firstName ? 'firstName-error' : undefined}
                className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-4 py-2.5 text-sm text-stone-900 placeholder-stone-400 transition-all outline-none"
                placeholder="Tus nombres"
                maxLength={50}
                required
              />
              {errors.firstName && (
                <p id="firstName-error" className="text-xs text-rose-600 mt-1 font-medium">
                  {errors.firstName}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="lastName" className="block text-xs font-bold text-stone-700 mb-1.5">
                Apellidos <span className="text-[#005141]">*</span>
              </label>
              <input
                id="lastName"
                type="text"
                value={lastName}
                onChange={(e) => {
                  setLastName(e.target.value);
                  if (errors.lastName) setErrors((prev) => ({ ...prev, lastName: undefined }));
                }}
                aria-invalid={Boolean(errors.lastName)}
                aria-describedby={errors.lastName ? 'lastName-error' : undefined}
                className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-4 py-2.5 text-sm text-stone-900 placeholder-stone-400 transition-all outline-none"
                placeholder="Tus apellidos"
                maxLength={50}
                required
              />
              {errors.lastName && (
                <p id="lastName-error" className="text-xs text-rose-600 mt-1 font-medium">
                  {errors.lastName}
                </p>
              )}
            </div>
          </div>

          {/* Teléfono de Contacto */}
          <div>
            <label htmlFor="phone" className="block text-xs font-bold text-stone-700 mb-1.5">
              Teléfono celular para entregas y WhatsApp
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-stone-400">
                <Phone className="w-4 h-4" />
              </div>
              <input
                id="phone"
                type="tel"
                value={phone}
                onChange={(e) => {
                  setPhone(e.target.value);
                  if (errors.phone) setErrors((prev) => ({ ...prev, phone: undefined }));
                }}
                aria-invalid={Boolean(errors.phone)}
                aria-describedby={errors.phone ? 'phone-error' : 'phone-hint'}
                className="w-full pl-10 bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-4 py-2.5 text-sm text-stone-900 placeholder-stone-400 transition-all outline-none"
                placeholder="Ej. 987654321 o +51987654321"
                maxLength={15}
              />
            </div>
            {errors.phone ? (
              <p id="phone-error" className="text-xs text-rose-600 mt-1 font-medium">
                {errors.phone}
              </p>
            ) : (
              <p id="phone-hint" className="text-[11px] text-stone-500 mt-1.5">
                Opcional. Permite que el comercio te avise por WhatsApp cuando tu pedido esté listo para retiro o entrega.
              </p>
            )}
          </div>

          {/* Feedback de Éxito Inline */}
          {isSuccess && (
            <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-2.5 text-xs text-[#005141] font-semibold">
              <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0" />
              <span>Tus cambios se han guardado exitosamente.</span>
            </div>
          )}

          {/* Botón de Guardar */}
          <div className="pt-2 flex justify-end">
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={updateMutation.isPending}
              disabled={updateMutation.isPending}
              className="w-full sm:w-auto rounded-full px-8 font-bold shadow-md shadow-[#005141]/10"
            >
              Guardar cambios
            </Button>
          </div>
        </div>

        {/* Columna Derecha: Identidad y Seguridad (1 Columna) */}
        <div className="space-y-6">
          <div className="p-6 sm:p-8 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
            <h3 className="text-xs font-bold text-stone-500 uppercase tracking-wider flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-[#005141]" />
              <span>Seguridad de la Cuenta</span>
            </h3>

            <div className="space-y-4 pt-2">
              {/* Correo Electrónico */}
              <div className="p-4 rounded-xl bg-[#faf7f2] border border-stone-200/80">
                <div className="flex items-center gap-2 text-xs text-stone-500 mb-1">
                  <Mail className="w-3.5 h-3.5 text-stone-400" />
                  <span className="font-semibold">Correo electrónico</span>
                </div>
                <p className="text-sm font-bold text-stone-900 break-all">{profile.email}</p>
                <p className="text-[11px] text-stone-500 mt-1">
                  Identificador único e intransferible.
                </p>
              </div>

              {/* Fecha de Registro */}
              <div className="p-4 rounded-xl bg-[#faf7f2] border border-stone-200/80">
                <div className="flex items-center gap-2 text-xs text-stone-500 mb-1">
                  <Calendar className="w-3.5 h-3.5 text-stone-400" />
                  <span className="font-semibold">Miembro desde</span>
                </div>
                <p className="text-sm font-bold text-stone-900">{formattedDate}</p>
                <p className="text-[11px] text-stone-500 mt-1">
                  Rol: Cliente JaldiShop
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  );
}
