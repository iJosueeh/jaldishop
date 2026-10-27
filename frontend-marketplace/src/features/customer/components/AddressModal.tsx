'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { CustomerAddress } from '../types/customer.types';
import {
  getDepartments,
  getProvinces,
  getDistricts,
  findUbigeoByCode,
} from '@/shared/utils/ubigeo';
import { Button } from '@/shared/components/ui/Button';
import { X, MapPin, CheckCircle2, ShieldCheck, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

interface AddressModalProps {
  isOpen: boolean;
  onClose: () => void;
  addressToEdit?: CustomerAddress | null;
  onSubmit: (data: {
    alias: string;
    recipientName: string;
    recipientPhone: string;
    department: string;
    province: string;
    district: string;
    ubigeoCode: string;
    addressLine: string;
    reference?: string | null;
    isDefault?: boolean;
  }) => Promise<void>;
  isSaving: boolean;
  isFirstAddress?: boolean;
}

export function AddressModal({
  isOpen,
  onClose,
  addressToEdit,
  onSubmit,
  isSaving,
  isFirstAddress = false,
}: AddressModalProps) {
  const isEditing = Boolean(addressToEdit);

  const [alias, setAlias] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [department, setDepartment] = useState('');
  const [province, setProvince] = useState('');
  const [district, setDistrict] = useState('');
  const [ubigeoCode, setUbigeoCode] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [reference, setReference] = useState('');
  const [isDefault, setIsDefault] = useState(false);

  const [errors, setErrors] = useState<Record<string, string>>({});

  // Cascading options
  const departments = useMemo(() => getDepartments(), []);
  const provinces = useMemo(() => getProvinces(department), [department]);
  const districts = useMemo(
    () => getDistricts(department, province),
    [department, province]
  );

  // Initialize form state
  useEffect(() => {
    if (isOpen) {
      if (addressToEdit) {
        setAlias(addressToEdit.alias || '');
        setRecipientName(addressToEdit.recipientName || '');
        setRecipientPhone(addressToEdit.recipientPhone || '');
        setDepartment(addressToEdit.department || '');
        setProvince(addressToEdit.province || '');
        setDistrict(addressToEdit.district || '');
        setUbigeoCode(addressToEdit.ubigeoCode || '');
        setAddressLine(addressToEdit.addressLine || '');
        setReference(addressToEdit.reference || '');
        setIsDefault(addressToEdit.isDefault);
      } else {
        setAlias('');
        setRecipientName('');
        setRecipientPhone('');
        setDepartment('');
        setProvince('');
        setDistrict('');
        setUbigeoCode('');
        setAddressLine('');
        setReference('');
        setIsDefault(isFirstAddress);
      }
      setErrors({});
    }
  }, [isOpen, addressToEdit, isFirstAddress]);

  if (!isOpen) return null;

  const handleDepartmentChange = (dep: string) => {
    setDepartment(dep);
    setProvince('');
    setDistrict('');
    setUbigeoCode('');
  };

  const handleProvinceChange = (prov: string) => {
    setProvince(prov);
    setDistrict('');
    setUbigeoCode('');
  };

  const handleDistrictChange = (distName: string) => {
    setDistrict(distName);
    const found = districts.find((d) => d.name === distName);
    if (found) {
      setUbigeoCode(found.code);
    } else {
      setUbigeoCode('');
    }
  };

  const validate = () => {
    const errs: Record<string, string> = {};

    if (!alias.trim()) {
      errs.alias = 'Ingresa un alias para identificar tu dirección (ej. Casa, Oficina).';
    } else if (alias.trim().length > 50) {
      errs.alias = 'El alias no debe exceder 50 caracteres.';
    }

    if (!recipientName.trim()) {
      errs.recipientName = 'Ingresa el nombre de la persona que recibirá el pedido.';
    } else if (recipientName.trim().length < 3) {
      errs.recipientName = 'El nombre debe tener al menos 3 caracteres.';
    } else if (recipientName.trim().length > 100) {
      errs.recipientName = 'El nombre no debe exceder 100 caracteres.';
    }

    const cleanPhone = recipientPhone.replace(/[\s-]/g, '');
    if (!cleanPhone) {
      errs.recipientPhone = 'Ingresa un teléfono de contacto para el repartidor.';
    } else if (!/^(?:\+?51)?9\d{8}$/.test(cleanPhone)) {
      errs.recipientPhone = 'Ingresa un número celular peruano válido (9 dígitos, ej. 987654321).';
    }

    if (!department) {
      errs.department = 'Selecciona un departamento.';
    }
    if (!province) {
      errs.province = 'Selecciona una provincia.';
    }
    if (!district || !ubigeoCode) {
      errs.district = 'Selecciona un distrito.';
    }

    if (!addressLine.trim()) {
      errs.addressLine = 'Ingresa la calle, avenida, número y departamento o interior.';
    } else if (addressLine.trim().length < 5) {
      errs.addressLine = 'La dirección debe tener al menos 5 caracteres.';
    } else if (addressLine.trim().length > 255) {
      errs.addressLine = 'La dirección no debe exceder 255 caracteres.';
    }

    if (reference.trim().length > 255) {
      errs.reference = 'La referencia no debe exceder 255 caracteres.';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Revisa los campos obligatorios antes de guardar.');
      return;
    }

    try {
      await onSubmit({
        alias: alias.trim(),
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        department,
        province,
        district,
        ubigeoCode,
        addressLine: addressLine.trim(),
        reference: reference.trim() || null,
        isDefault: isFirstAddress ? true : isDefault,
      });
      onClose();
    } catch {
      // Error handling is handled in the parent/hook toast
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="address-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/50 backdrop-blur-xs overflow-y-auto"
    >
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-2xl max-w-xl w-full my-8 overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4.5 border-b border-stone-100 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-[#005141]/10 flex items-center justify-center text-[#005141]">
              <MapPin className="w-4 h-4" />
            </div>
            <div>
              <h2 id="address-modal-title" className="text-base font-bold text-stone-900">
                {isEditing ? 'Editar dirección de entrega' : 'Nueva dirección de entrega'}
              </h2>
              <p className="text-xs text-stone-500">
                Cobertura oficial en todo el Perú (INEI UBIGEO)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar formulario"
            className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Alias y Teléfono */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="address-alias" className="block text-xs font-bold text-stone-700 mb-1">
                Alias de la dirección <span className="text-rose-500">*</span>
              </label>
              <input
                id="address-alias"
                type="text"
                value={alias}
                onChange={(e) => setAlias(e.target.value)}
                placeholder="Ej. Casa, Trabajo, Mamá"
                className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-3.5 py-2 text-sm text-stone-900 placeholder-stone-400 outline-none transition-all"
                maxLength={50}
              />
              {errors.alias && (
                <p className="text-xs text-rose-600 mt-1 font-medium">{errors.alias}</p>
              )}
            </div>

            <div>
              <label htmlFor="address-phone" className="block text-xs font-bold text-stone-700 mb-1">
                Teléfono de contacto <span className="text-rose-500">*</span>
              </label>
              <input
                id="address-phone"
                type="tel"
                value={recipientPhone}
                onChange={(e) => setRecipientPhone(e.target.value)}
                placeholder="987654321"
                className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-3.5 py-2 text-sm text-stone-900 placeholder-stone-400 outline-none transition-all"
                maxLength={15}
              />
              {errors.recipientPhone && (
                <p className="text-xs text-rose-600 mt-1 font-medium">{errors.recipientPhone}</p>
              )}
            </div>
          </div>

          {/* Destinatario */}
          <div>
            <label htmlFor="address-recipient" className="block text-xs font-bold text-stone-700 mb-1">
              Nombre de quien recibe el pedido <span className="text-rose-500">*</span>
            </label>
            <input
              id="address-recipient"
              type="text"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              placeholder="Nombre y apellido completo"
              className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-3.5 py-2 text-sm text-stone-900 placeholder-stone-400 outline-none transition-all"
              maxLength={100}
            />
            {errors.recipientName && (
              <p className="text-xs text-rose-600 mt-1 font-medium">{errors.recipientName}</p>
            )}
          </div>

          {/* UBIGEO Cascading Selectors: Departamento -> Provincia -> Distrito */}
          <div className="p-4 rounded-xl bg-[#faf7f2] border border-stone-200/80 space-y-3">
            <span className="text-xs font-bold text-stone-700 uppercase tracking-wider block">
              Ubicación Geográfica (Perú)
            </span>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* Departamento */}
              <div>
                <label htmlFor="select-department" className="block text-[11px] font-semibold text-stone-600 mb-1">
                  Departamento <span className="text-rose-500">*</span>
                </label>
                <select
                  id="select-department"
                  value={department}
                  onChange={(e) => handleDepartmentChange(e.target.value)}
                  className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-2.5 py-2 text-xs text-stone-900 outline-none transition-all"
                >
                  <option value="">Selecciona...</option>
                  {departments.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
                {errors.department && (
                  <p className="text-[11px] text-rose-600 mt-1 font-medium">{errors.department}</p>
                )}
              </div>

              {/* Provincia */}
              <div>
                <label htmlFor="select-province" className="block text-[11px] font-semibold text-stone-600 mb-1">
                  Provincia <span className="text-rose-500">*</span>
                </label>
                <select
                  id="select-province"
                  value={province}
                  onChange={(e) => handleProvinceChange(e.target.value)}
                  disabled={!department}
                  className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-2.5 py-2 text-xs text-stone-900 outline-none transition-all disabled:opacity-50 disabled:bg-stone-100"
                >
                  <option value="">Selecciona...</option>
                  {provinces.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
                {errors.province && (
                  <p className="text-[11px] text-rose-600 mt-1 font-medium">{errors.province}</p>
                )}
              </div>

              {/* Distrito */}
              <div>
                <label htmlFor="select-district" className="block text-[11px] font-semibold text-stone-600 mb-1">
                  Distrito <span className="text-rose-500">*</span>
                </label>
                <select
                  id="select-district"
                  value={district}
                  onChange={(e) => handleDistrictChange(e.target.value)}
                  disabled={!province}
                  className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-2.5 py-2 text-xs text-stone-900 outline-none transition-all disabled:opacity-50 disabled:bg-stone-100"
                >
                  <option value="">Selecciona...</option>
                  {districts.map((di) => (
                    <option key={di.code} value={di.name}>
                      {di.name}
                    </option>
                  ))}
                </select>
                {errors.district && (
                  <p className="text-[11px] text-rose-600 mt-1 font-medium">{errors.district}</p>
                )}
              </div>
            </div>

            {ubigeoCode && (
              <p className="text-[11px] text-stone-500">
                Código UBIGEO: <span className="font-mono font-semibold text-stone-700">{ubigeoCode}</span>
              </p>
            )}
          </div>

          {/* Dirección exacta */}
          <div>
            <label htmlFor="address-line" className="block text-xs font-bold text-stone-700 mb-1">
              Dirección exacta <span className="text-rose-500">*</span>
            </label>
            <input
              id="address-line"
              type="text"
              value={addressLine}
              onChange={(e) => setAddressLine(e.target.value)}
              placeholder="Ej. Av. Larco 1234, Dpto. 402, Urb. San Antonio"
              className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-3.5 py-2 text-sm text-stone-900 placeholder-stone-400 outline-none transition-all"
              maxLength={255}
            />
            {errors.addressLine && (
              <p className="text-xs text-rose-600 mt-1 font-medium">{errors.addressLine}</p>
            )}
          </div>

          {/* Referencia opcional */}
          <div>
            <label htmlFor="address-reference" className="block text-xs font-bold text-stone-700 mb-1">
              Referencia de entrega (opcional)
            </label>
            <input
              id="address-reference"
              type="text"
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              placeholder="Ej. Portón verde, frente al parque, timbre 4B"
              className="w-full bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl px-3.5 py-2 text-sm text-stone-900 placeholder-stone-400 outline-none transition-all"
              maxLength={255}
            />
            {errors.reference && (
              <p className="text-xs text-rose-600 mt-1 font-medium">{errors.reference}</p>
            )}
          </div>

          {/* Checkbox: Dirección Predeterminada */}
          <div className="pt-2">
            <label className="flex items-start gap-2.5 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={isFirstAddress || isDefault}
                disabled={isFirstAddress || (isEditing && addressToEdit?.isDefault)}
                onChange={(e) => setIsDefault(e.target.checked)}
                className="mt-0.5 w-4 h-4 rounded border-stone-300 text-[#005141] focus:ring-[#005141] accent-[#005141]"
              />
              <span className="text-xs text-stone-700">
                <span className="font-semibold block">Establecer como dirección predeterminada</span>
                <span className="text-stone-500 text-[11px]">
                  {isFirstAddress
                    ? 'Esta será tu primera dirección, por lo que se marcará como predeterminada automáticamente.'
                    : isEditing && addressToEdit?.isDefault
                    ? 'Esta es tu dirección predeterminada activa.'
                    : 'Será sugerida automáticamente durante tus próximas compras con entrega a domicilio.'}
                </span>
              </span>
            </label>
          </div>

          {/* Footer Actions */}
          <div className="pt-4 border-t border-stone-100 flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-full px-5 text-xs font-semibold"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSaving}
              disabled={isSaving}
              className="rounded-full px-6 font-bold shadow-md shadow-[#005141]/10 text-xs"
            >
              {isEditing ? 'Guardar cambios' : 'Registrar dirección'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
