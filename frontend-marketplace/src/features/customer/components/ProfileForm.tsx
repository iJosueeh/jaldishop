'use client';

import React, { useState, useEffect } from 'react';
import { useCustomerProfile, useUpdateCustomerProfile } from '../hooks/useCustomerProfile';
import {
  validateFirstName,
  validateLastName,
  validateLocalPeruPhone,
  toLocalPeruPhone,
} from '@/features/auth/utils/validation';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { PeruPhoneInput } from '@/shared/components/ui/PeruPhoneInput';
import { notify, notifyKeys } from '@/shared/utils/notify';
import { User, Mail, Calendar, ShieldCheck, CheckCircle2 } from 'lucide-react';

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
      setPhone(toLocalPeruPhone(profile.phone || ''));
    }
  }, [profile]);

  if (isLoading) {
    return (
      <div className="space-y-6 w-full" role="status" aria-busy="true" aria-label="Cargando perfil">
        <div className="p-6 sm:p-8 rounded-xl bg-white border border-[#e7e0d6] space-y-6">
          <Skeleton className="h-6 w-48 bg-[#f0eae0]" />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Skeleton className="h-11 w-full bg-[#f0eae0]" />
            <Skeleton className="h-11 w-full bg-[#f0eae0]" />
          </div>
          <Skeleton className="h-11 w-full bg-[#f0eae0]" />
        </div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div
        role="alert"
        className="p-8 rounded-xl bg-white border border-rose-200 text-center max-w-xl mx-auto"
      >
        <p className="text-sm text-rose-700 font-semibold mb-3">
          No pudimos cargar la información de tu perfil.
        </p>
        <Button variant="outline" size="sm" onClick={() => refetch()} className="min-h-[44px]">
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

    const phoneErr = validateLocalPeruPhone(phone);
    if (phoneErr) errs.phone = phoneErr;

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSuccess(false);

    if (!validate()) return;

    try {
      const localPhone = toLocalPeruPhone(phone);
      await updateMutation.mutateAsync({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: localPhone || null,
      });

      setIsSuccess(true);
      setTimeout(() => setIsSuccess(false), 4000);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'No se pudo actualizar el perfil. Inténtalo de nuevo.';
      notify.error(message, { id: notifyKeys.profileError() });
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
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)] gap-6 items-start">
        {/* Columna Izquierda: Formulario de Datos Personales */}
        <div className="p-5 sm:p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e7e0d6]/70 pb-4">
            <div>
              <h2 className="text-base sm:text-lg font-display font-bold text-[#1c1917] flex items-center gap-2">
                <User className="w-4 h-4 text-[#005141]" />
                <span>Datos Personales</span>
              </h2>
              <p className="text-xs text-[#57534e] mt-0.5">
                Esta información se utiliza para coordinar tus compras y la entrega de pedidos.
              </p>
            </div>
            <Badge variant="jade" size="sm" className="font-bold">
              Cuenta de Cliente
            </Badge>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-xs font-bold text-[#1c1917] mb-1.5">
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
                className="w-full min-h-[44px] bg-white border border-[#e7e0d6] focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-lg px-3.5 py-2 text-sm text-[#1c1917] placeholder-[#78716c] transition-colors outline-none"
                placeholder="Tus nombres"
                maxLength={50}
                required
              />
              {errors.firstName && (
                <p id="firstName-error" className="text-xs text-rose-700 mt-1 font-medium">
                  {errors.firstName}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="lastName" className="block text-xs font-bold text-[#1c1917] mb-1.5">
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
                className="w-full min-h-[44px] bg-white border border-[#e7e0d6] focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-lg px-3.5 py-2 text-sm text-[#1c1917] placeholder-[#78716c] transition-colors outline-none"
                placeholder="Tus apellidos"
                maxLength={50}
                required
              />
              {errors.lastName && (
                <p id="lastName-error" className="text-xs text-rose-700 mt-1 font-medium">
                  {errors.lastName}
                </p>
              )}
            </div>
          </div>

          <div>
            <label htmlFor="phone" className="block text-xs font-bold text-[#1c1917] mb-1.5">
              Teléfono celular para entregas y WhatsApp
            </label>
            <PeruPhoneInput
              id="phone"
              value={phone}
              onChange={(localPhone) => {
                setPhone(localPhone);
                if (errors.phone) setErrors((prev) => ({ ...prev, phone: undefined }));
              }}
              error={errors.phone}
              errorId="phone-error"
              hint="Opcional. Permite que el comercio te avise por WhatsApp cuando tu pedido esté listo para retiro o entrega."
              hintId="phone-hint"
            />
          </div>

          {isSuccess && (
            <div
              role="status"
              className="p-3.5 rounded-lg bg-[#f0fdfa] border border-[#ccfbf1] flex items-center gap-2.5 text-xs text-[#005141] font-semibold"
            >
              <CheckCircle2 className="w-4 h-4 text-[#005141] shrink-0" />
              <span>Tus cambios se han guardado exitosamente.</span>
            </div>
          )}

          <div className="pt-2 flex justify-end">
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={updateMutation.isPending}
              disabled={updateMutation.isPending}
              className="min-h-[44px] w-full sm:w-auto rounded-full px-8 font-bold"
            >
              Guardar cambios
            </Button>
          </div>
        </div>

        {/* Columna Derecha: Seguridad de la Cuenta (una sola superficie con divide-y) */}
        <aside className="p-5 sm:p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-4">
          <h2 className="text-xs font-bold text-[#57534e] uppercase tracking-wider flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-[#005141]" />
            <span>Seguridad de la Cuenta</span>
          </h2>

          <dl className="divide-y divide-[#e7e0d6]/70">
            <div className="py-3.5 first:pt-1">
              <dt className="flex items-center gap-2 text-xs text-[#57534e] mb-1">
                <Mail className="w-3.5 h-3.5 text-[#78716c]" aria-hidden="true" />
                <span className="font-semibold">Correo electrónico</span>
              </dt>
              <dd className="text-sm font-bold text-[#1c1917] break-all">{profile.email}</dd>
              <dd className="text-xs text-[#78716c] mt-0.5">
                Identificador único e intransferible.
              </dd>
            </div>

            <div className="py-3.5 last:pb-0">
              <dt className="flex items-center gap-2 text-xs text-[#57534e] mb-1">
                <Calendar className="w-3.5 h-3.5 text-[#78716c]" aria-hidden="true" />
                <span className="font-semibold">Miembro desde</span>
              </dt>
              <dd className="text-sm font-bold text-[#1c1917] tabular-nums">{formattedDate}</dd>
              <dd className="text-xs text-[#78716c] mt-0.5">
                Rol: Cliente JaldiShop
              </dd>
            </div>
          </dl>
        </aside>
      </div>
    </form>
  );
}
