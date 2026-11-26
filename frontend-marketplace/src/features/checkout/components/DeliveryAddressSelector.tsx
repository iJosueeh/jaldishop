'use client';

import React, { useState, useEffect, useMemo, useRef } from 'react';
import { CustomerAddress } from '@/features/customer/types/customer.types';
import { useCustomerAddresses } from '@/features/customer/hooks/useCustomerAddresses';
import { PublicStore } from '@/features/storefront/types/storefront.types';
import { DeliveryFormData } from '../types/checkout.types';
import {
  getDepartments,
  getProvinces,
  getDistricts,
  formatUbigeoLocation,
} from '@/shared/utils/ubigeo';
import { formatCurrency } from '@/shared/utils/formatters';
import {
  MapPin,
  Home,
  Briefcase,
  Star,
  AlertCircle,
  User,
  ShieldCheck,
  Truck,
  RotateCcw,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { PeruPhoneInput } from '@/shared/components/ui/PeruPhoneInput';
import {
  toLocalPeruPhone,
  validateLocalPeruPhone,
} from '@/features/auth/utils/validation';

export interface DeliveryAddressSelectorProps {
  store: PublicStore;
  value: DeliveryFormData | null;
  onChange: (data: DeliveryFormData | null, isValid: boolean) => void;
  disabled?: boolean;
  defaultRecipientName?: string;
  defaultRecipientPhone?: string;
  embedded?: boolean;
}

export function DeliveryAddressSelector({
  store,
  onChange,
  disabled = false,
  defaultRecipientName,
  defaultRecipientPhone,
  embedded = false,
}: DeliveryAddressSelectorProps) {
  const {
    addresses,
    total,
    defaultAddress,
    canAddAddress,
    isLoading,
    createAddress,
    setDefaultAddress,
  } = useCustomerAddresses();
  const [isSavingInline, setIsSavingInline] = useState(false);
  const hasAutofilledProfileRef = useRef(false);

  // Mode: 'saved' or 'manual'
  const [mode, setMode] = useState<'saved' | 'manual'>(() => {
    return addresses.length > 0 ? 'saved' : 'manual';
  });

  const [selectedAddressId, setSelectedAddressId] = useState<string>('');
  const [lastSelectedSnapshot, setLastSelectedSnapshot] = useState<CustomerAddress | null>(null);

  // Manual entry fields
  const [alias, setAlias] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [department, setDepartment] = useState('');
  const [province, setProvince] = useState('');
  const [district, setDistrict] = useState('');
  const [ubigeoCode, setUbigeoCode] = useState('');
  const [addressLine, setAddressLine] = useState('');
  const [reference, setReference] = useState('');
  const [saveToAddresses, setSaveToAddresses] = useState(false);
  const [inlineSaveError, setInlineSaveError] = useState<string | null>(null);
  const [inlineSaveSuccess, setInlineSaveSuccess] = useState<string | null>(null);

  // Cascading UBIGEO options
  const departments = useMemo(() => getDepartments(), []);
  const provinces = useMemo(() => getProvinces(department), [department]);
  const districts = useMemo(
    () => getDistricts(department, province),
    [department, province]
  );

  // Auto-select default or first address once loaded
  useEffect(() => {
    if (isLoading) return;

    if (addresses.length > 0) {
      if (!selectedAddressId || !addresses.some((a) => a.id === selectedAddressId)) {
        const initial = defaultAddress || addresses[0];
        setSelectedAddressId(initial.id);
        setLastSelectedSnapshot(initial);
        setMode('saved');
      }
    } else {
      setMode('manual');
    }
  }, [addresses, defaultAddress, isLoading, selectedAddressId]);

  // Autofill manual recipient name & phone from account defaults once
  useEffect(() => {
    if (hasAutofilledProfileRef.current) return;
    if (!defaultRecipientName && !defaultRecipientPhone) return;
    hasAutofilledProfileRef.current = true;
    if (defaultRecipientName && !recipientName) {
      setRecipientName(defaultRecipientName);
    }
    const localDefaultPhone = toLocalPeruPhone(defaultRecipientPhone);
    if (localDefaultPhone && !recipientPhone) {
      setRecipientPhone(localDefaultPhone);
    }
  }, [defaultRecipientName, defaultRecipientPhone, recipientName, recipientPhone]);

  // Synchronize with parent state
  const lastEmittedRef = useRef<string | null>(null);
  useEffect(() => {
    // Evita emitir al padre un payload con el mismo contenido que el anterior:
    // sin esta guarda, un onChange con referencia inestable (inline) provoca
    // un bucle infinito de re-renders padre → effect → setState → render.
    const emit = (data: DeliveryFormData | null, valid: boolean) => {
      const fingerprint = JSON.stringify({ data, valid });
      if (lastEmittedRef.current === fingerprint) return;
      lastEmittedRef.current = fingerprint;
      onChange(data, valid);
    };

    if (!store.deliveryEnabled) {
      emit(null, false);
      return;
    }

    if (mode === 'saved') {
      const activeAddr = addresses.find((a) => a.id === selectedAddressId) || lastSelectedSnapshot;
      if (activeAddr) {
        const formData: DeliveryFormData = {
          mode: 'saved',
          selectedAddressId: activeAddr.id,
          recipientName: activeAddr.recipientName,
          recipientPhone: activeAddr.recipientPhone,
          department: activeAddr.department,
          province: activeAddr.province,
          district: activeAddr.district,
          ubigeoCode: activeAddr.ubigeoCode,
          addressLine: activeAddr.streetAddress || activeAddr.addressLine || '',
          reference: activeAddr.reference,
          latitude: activeAddr.latitude,
          longitude: activeAddr.longitude,
          saveToAddresses: false,
        };
        emit(formData, true);
      } else {
        emit(null, false);
      }
    } else {
      // Manual mode validation
      const localPhone = toLocalPeruPhone(recipientPhone);
      const isPhoneValid = validateLocalPeruPhone(localPhone, true) === null;
      const isComplete =
        recipientName.trim().length >= 3 &&
        isPhoneValid &&
        Boolean(department) &&
        Boolean(province) &&
        Boolean(district) &&
        Boolean(ubigeoCode) &&
        addressLine.trim().length >= 5;

      const formData: DeliveryFormData = {
        mode: 'manual',
        alias: alias.trim() || undefined,
        recipientName: recipientName.trim(),
        recipientPhone: localPhone,
        department,
        province,
        district,
        ubigeoCode,
        addressLine: addressLine.trim(),
        reference: reference.trim() || null,
        latitude: null,
        longitude: null,
        saveToAddresses: canAddAddress && saveToAddresses,
      };

      emit(formData, isComplete);
    }
  }, [
    mode,
    selectedAddressId,
    lastSelectedSnapshot,
    addresses,
    recipientName,
    recipientPhone,
    department,
    province,
    district,
    ubigeoCode,
    addressLine,
    reference,
    saveToAddresses,
    alias,
    canAddAddress,
    store.deliveryEnabled,
    onChange,
  ]);

  const handleSelectSaved = (addr: CustomerAddress) => {
    setSelectedAddressId(addr.id);
    setLastSelectedSnapshot(addr);
    setMode('saved');
  };

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
    setUbigeoCode(found ? found.code : '');
  };

  const handleSaveAddressInline = async () => {
    if (!createAddress || !canAddAddress) return;
    setInlineSaveError(null);
    setInlineSaveSuccess(null);
    setIsSavingInline(true);
    try {
      const created = await createAddress({
        alias: alias.trim() || 'Mi dirección',
        recipientName: recipientName.trim(),
        recipientPhone: toLocalPeruPhone(recipientPhone),
        department,
        province,
        district,
        ubigeoCode,
        streetAddress: addressLine.trim(),
        addressLine: addressLine.trim(),
        reference: reference.trim() || null,
        latitude: null,
        longitude: null,
        isDefault: addresses.length === 0,
      });
      setSelectedAddressId(created.id);
      setLastSelectedSnapshot(created);
      setMode('saved');
      setInlineSaveSuccess('Dirección guardada y seleccionada.');
    } catch (err) {
      setInlineSaveError(err instanceof Error ? err.message : 'No se pudo guardar la dirección.');
    } finally {
      setIsSavingInline(false);
    }
  };

  // Check if previously selected address was deleted in another session/tab
  const isAddressDeletedRemotely =
    mode === 'saved' &&
    selectedAddressId &&
    !addresses.some((a) => a.id === selectedAddressId) &&
    Boolean(lastSelectedSnapshot);

  return (
    <div className={embedded ? 'p-5 sm:p-7 text-[#1c1917] space-y-5' : 'bg-white rounded-[12px] p-5 sm:p-7 border border-[#e7e0d6] text-[#1c1917] space-y-5'}>
      {/* Header: Title & Store Delivery Fee */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-[#e7e0d6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#005141]" />
            <h3 className="font-display text-base sm:text-lg font-semibold text-[#1c1917] tracking-tight flex items-center gap-2">
              Dirección de Entrega
            </h3>
          </div>
          <p className="text-xs text-[#57534e] mt-0.5">
            Indica a dónde debe llegar tu pedido cuando el repartidor de {store.name} salga en camino.
          </p>
        </div>

        {store.deliveryEnabled && (
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-[#f0fdfa] border border-[#ccfbf1] text-[#005141] tabular-nums">
              <Truck className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Costo de envío: {formatCurrency(store.deliveryFeeAmount ?? 0, 'PEN')}</span>
            </span>
          </div>
        )}
      </div>

      {/* Alerta: Si la tienda no realiza envíos a domicilio */}
      {!store.deliveryEnabled && (
        <div
          role="alert"
          className="p-4 rounded-[10px] bg-amber-50 border border-amber-200 flex items-start gap-3 text-xs text-amber-900"
        >
          <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
          <div className="space-y-1">
            <p className="font-bold">Envíos a domicilio no disponibles</p>
            <p className="leading-relaxed">
              {store.name} no realiza envíos a domicilio en este momento. Puedes optar por cambiar tu modalidad a <strong>Retiro en Tienda</strong> para completar tu pedido.
            </p>
          </div>
        </div>
      )}

      {/* Alerta: Dirección eliminada remotamente durante el checkout */}
      {isAddressDeletedRemotely && (
        <div
          role="status"
          className="p-3.5 rounded-[10px] bg-[#faf7f2] border border-[#e7e0d6] flex items-start gap-2.5 text-xs text-[#57534e]"
        >
          <RotateCcw className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
          <div>
            <p className="font-bold text-[#1c1917]">La dirección seleccionada fue modificada o eliminada en tu cuenta.</p>
            <p className="mt-0.5">
              Hemos conservado los datos ingresados para que no los pierdas ({lastSelectedSnapshot?.addressLine}), o puedes seleccionar otra de tus direcciones guardadas.
            </p>
          </div>
        </div>
      )}

      {inlineSaveSuccess && (
        <div role="status" className="p-3 rounded-[10px] bg-[#f0fdfa] border border-[#ccfbf1] text-xs font-medium text-[#005141]">
          {inlineSaveSuccess}
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-[12px] bg-[#f7f3ec]" />
        </div>
      ) : (
        <div className="space-y-4">
          {/* Selector de Modo: Direcciones Guardadas vs Nueva Dirección */}
          {addresses.length > 0 && (
            <div className="flex items-center gap-1.5 p-1 bg-[#f7f3ec] rounded-full border border-[#e7e0d6] max-w-md">
              <button
                type="button"
                onClick={() => setMode('saved')}
                className={cn(
                  'flex-1 py-1.5 px-3.5 rounded-full text-xs font-semibold transition-all cursor-pointer text-center',
                  mode === 'saved'
                    ? 'bg-[#005141] text-white'
                    : 'text-[#57534e] hover:text-[#1c1917]'
                )}
              >
                Mis direcciones guardadas ({total})
              </button>
              <button
                type="button"
                onClick={() => setMode('manual')}
                className={cn(
                  'flex-1 py-1.5 px-3.5 rounded-full text-xs font-semibold transition-all cursor-pointer text-center',
                  mode === 'manual'
                    ? 'bg-[#005141] text-white'
                    : 'text-[#57534e] hover:text-[#1c1917]'
                )}
              >
                Usar otra dirección
              </button>
            </div>
          )}

          {/* MODO 1: Lista de Direcciones Guardadas */}
          {mode === 'saved' && addresses.length > 0 && (
            <div
              role="radiogroup"
              aria-label="Selecciona tu dirección guardada"
              className="grid grid-cols-1 sm:grid-cols-2 gap-2.5"
            >
              {addresses.map((addr) => {
                const isSelected = selectedAddressId === addr.id;
                const locationLabel = formatUbigeoLocation(
                  addr.department,
                  addr.province,
                  addr.district
                );

                return (
                  <div
                    key={addr.id}
                    className={cn(
                      'group relative p-3.5 rounded-[10px] border transition-colors flex flex-col justify-between gap-2',
                      isSelected
                        ? 'border-[#005141] bg-[#f0fdfa] ring-1 ring-[#005141]/20'
                        : 'border-[#e7e0d6] bg-[#faf7f2]/50 hover:border-[#005141]/40 hover:bg-white'
                    )}
                  >
                    <button
                      type="button"
                      role="radio"
                      aria-checked={isSelected}
                      onClick={() => handleSelectSaved(addr)}
                      disabled={disabled || !store.deliveryEnabled}
                      className="text-left cursor-pointer w-full focus-visible:outline-none flex items-start gap-3"
                    >
                      <span
                        aria-hidden="true"
                        className={cn(
                          'mt-0.5 w-4 h-4 rounded-full border shrink-0 flex items-center justify-center transition-colors',
                          isSelected
                            ? 'border-[#005141] bg-[#005141]'
                            : 'border-[#78716c]/50 bg-white group-hover:border-[#005141]'
                        )}
                      >
                        {isSelected && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </span>

                      <div className="min-w-0 flex-1 space-y-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#1c1917] truncate">
                            {addr.alias.toLowerCase().includes('casa') ? (
                              <Home className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                            ) : addr.alias.toLowerCase().includes('oficina') || addr.alias.toLowerCase().includes('trabajo') ? (
                              <Briefcase className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                            ) : (
                              <MapPin className="w-3.5 h-3.5 text-[#005141] shrink-0" aria-hidden="true" />
                            )}
                            <span className="truncate">{addr.alias}</span>
                          </span>

                          {addr.isDefault && (
                            <span className="shrink-0 inline-flex items-center gap-1 rounded-full bg-[#005141]/10 px-2 py-0.5 text-[10px] font-semibold text-[#005141]">
                              <Star className="w-2.5 h-2.5 fill-[#005141]" aria-hidden="true" />
                              <span>Predeterminada</span>
                            </span>
                          )}
                        </div>

                        <p className="text-xs font-medium text-[#1c1917] leading-snug truncate">
                          {addr.addressLine}
                        </p>

                        <p className="text-[11px] text-[#57534e] truncate flex items-center gap-1.5">
                          <span className="truncate">{locationLabel}</span>
                          <span aria-hidden="true">·</span>
                          <span className="inline-flex items-center gap-1 shrink-0">
                            <User className="w-3 h-3 text-[#78716c]" aria-hidden="true" />
                            <span>{addr.recipientName}</span>
                          </span>
                        </p>

                        {isSelected && addr.reference && (
                          <p className="text-[11px] text-[#005141]/85 truncate pt-0.5">
                            Ref: {addr.reference}
                          </p>
                        )}
                      </div>
                    </button>

                    {!addr.isDefault && (
                      <div className="pl-7 flex justify-end">
                        <button
                          type="button"
                          disabled={disabled || isSavingInline}
                          onClick={() => void setDefaultAddress?.(addr.id)}
                          className="text-[11px] font-medium text-[#005141] hover:underline cursor-pointer disabled:opacity-50"
                        >
                          Hacer predeterminada
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {/* MODO 2: Formulario de Dirección Manual / Nueva Dirección */}
          {mode === 'manual' && (
            <div className="space-y-4 pt-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                {/* Nombre de quien recibe */}
                <div>
                  <label htmlFor="chk-recipient-name" className="block text-xs font-medium text-[#57534e] mb-1.5">
                    Nombre del destinatario <span className="text-rose-600">*</span>
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-[#78716c] absolute left-3 top-2.5 pointer-events-none" aria-hidden="true" />
                    <input
                      id="chk-recipient-name"
                      type="text"
                      value={recipientName}
                      onChange={(e) => setRecipientName(e.target.value)}
                      placeholder="Nombre y apellido completo"
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none transition-colors"
                      maxLength={100}
                    />
                  </div>
                </div>

                {/* Teléfono */}
                <div>
                  <label htmlFor="chk-recipient-phone" className="block text-xs font-medium text-[#57534e] mb-1.5">
                    Teléfono celular <span className="text-rose-600">*</span>
                  </label>
                  <PeruPhoneInput
                    id="chk-recipient-phone"
                    value={recipientPhone}
                    onChange={setRecipientPhone}
                    disabled={disabled || !store.deliveryEnabled}
                    error={
                      recipientPhone.length > 0
                        ? (validateLocalPeruPhone(recipientPhone, true) ?? undefined)
                        : undefined
                    }
                    required
                  />
                </div>
              </div>

              {/* Cascada UBIGEO Oficial */}
              <div className="p-3.5 rounded-[10px] bg-[#faf7f2] border border-[#e7e0d6] space-y-3">
                <span className="text-[11px] font-semibold text-[#005141] uppercase tracking-[0.06em] block">
                  Ubicación de entrega (Perú)
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <div>
                    <label htmlFor="chk-department" className="block text-[11px] font-medium text-[#57534e] mb-1">
                      Departamento <span className="text-rose-600">*</span>
                    </label>
                    <select
                      id="chk-department"
                      value={department}
                      onChange={(e) => handleDepartmentChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full px-2.5 py-2 text-xs bg-white border border-[#e7e0d6] focus:border-[#005141] rounded-[8px] text-[#1c1917] outline-none"
                    >
                      <option value="">Selecciona...</option>
                      {departments.map((d) => (
                        <option key={d} value={d}>
                          {d}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label htmlFor="chk-province" className="block text-[11px] font-medium text-[#57534e] mb-1">
                      Provincia <span className="text-rose-600">*</span>
                    </label>
                    <select
                      id="chk-province"
                      value={province}
                      onChange={(e) => handleProvinceChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !department}
                      className="w-full px-2.5 py-2 text-xs bg-white border border-[#e7e0d6] focus:border-[#005141] rounded-[8px] text-[#1c1917] outline-none disabled:opacity-50"
                    >
                      <option value="">Selecciona...</option>
                      {provinces.map((p) => (
                        <option key={p} value={p}>
                          {p}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label htmlFor="chk-district" className="block text-[11px] font-medium text-[#57534e] mb-1">
                      Distrito <span className="text-rose-600">*</span>
                    </label>
                    <select
                      id="chk-district"
                      value={district}
                      onChange={(e) => handleDistrictChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !province}
                      className="w-full px-2.5 py-2 text-xs bg-white border border-[#e7e0d6] focus:border-[#005141] rounded-[8px] text-[#1c1917] outline-none disabled:opacity-50"
                    >
                      <option value="">Selecciona...</option>
                      {districts.map((di) => (
                        <option key={di.code} value={di.name}>
                          {di.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {ubigeoCode && (
                  <p className="text-[10px] text-[#57534e]">
                    UBIGEO validado: <span className="font-mono font-semibold text-[#005141] tabular-nums">{ubigeoCode}</span>
                  </p>
                )}
              </div>

              {/* Dirección exacta */}
              <div>
                <label htmlFor="chk-address-line" className="block text-xs font-medium text-[#57534e] mb-1.5">
                  Dirección exacta (Calle, Nro, Interior) <span className="text-rose-600">*</span>
                </label>
                <input
                  id="chk-address-line"
                  type="text"
                  value={addressLine}
                  onChange={(e) => setAddressLine(e.target.value)}
                  placeholder="Ej. Jr. Huancavelica 340, Dpto 201"
                  disabled={disabled || !store.deliveryEnabled}
                  className="w-full px-3 py-2 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none transition-colors"
                  maxLength={255}
                />
              </div>

              {/* Referencia */}
              <div>
                <label htmlFor="chk-reference" className="block text-xs font-medium text-[#57534e] mb-1.5">
                  Referencia de entrega (opcional)
                </label>
                <input
                  id="chk-reference"
                  type="text"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  placeholder="Ej. Frente a la farmacia, portón marrón"
                  disabled={disabled || !store.deliveryEnabled}
                  className="w-full px-3 py-2 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none transition-colors"
                  maxLength={255}
                />
              </div>

              {inlineSaveError && (
                <p role="alert" className="text-xs text-rose-700">
                  {inlineSaveError}
                </p>
              )}

              {/* Checkbox para guardar opcionalmente en Mis Direcciones + botón inline */}
              <div className="pt-3 border-t border-[#e7e0d6] space-y-3">
                <label className="flex items-start gap-2.5 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={canAddAddress && saveToAddresses}
                    disabled={disabled || !canAddAddress || !store.deliveryEnabled}
                    onChange={(e) => setSaveToAddresses(e.target.checked)}
                    className="mt-0.5 w-4 h-4 rounded-[4px] border-[#e7e0d6] bg-white text-[#005141] focus:ring-[#005141] accent-[#005141]"
                  />
                  <span className="text-xs text-[#1c1917]">
                    <span className="font-semibold block">Guardar esta dirección en Mis Direcciones</span>
                    <span className="text-[#57534e] text-[11px]">
                      {canAddAddress
                        ? 'Podrás reutilizarla rápidamente en tus próximas compras sin volver a escribirla.'
                        : 'Has alcanzado el límite de 5 direcciones. Puedes usar esta dirección para este pedido sin guardarla.'}
                    </span>
                  </span>
                </label>

                {saveToAddresses && canAddAddress && (
                  <div className="pl-6 flex flex-wrap items-center gap-2">
                    <input
                      type="text"
                      value={alias}
                      onChange={(e) => setAlias(e.target.value)}
                      placeholder="Alias (ej. Playa, Taller)"
                      className="w-full max-w-xs px-3 py-1.5 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none"
                      maxLength={50}
                    />
                    <button
                      type="button"
                      disabled={
                        disabled ||
                        isSavingInline ||
                        !recipientName.trim() ||
                        validateLocalPeruPhone(recipientPhone, true) !== null ||
                        !ubigeoCode ||
                        addressLine.trim().length < 5
                      }
                      onClick={() => void handleSaveAddressInline()}
                      className="rounded-full border border-[#e7e0d6] bg-white px-4 py-1.5 text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] disabled:opacity-40 cursor-pointer"
                    >
                      {isSavingInline ? 'Guardando...' : 'Guardar dirección ahora'}
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Nota de Cobertura y Contacto Directo */}
      <div className="pt-3 text-[11px] text-[#57534e] flex items-center justify-between gap-2 border-t border-[#e7e0d6]">
        <span className="flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
          <span>La entrega se coordinará directamente según el radio de despacho de {store.name}.</span>
        </span>
      </div>
    </div>
  );
}
