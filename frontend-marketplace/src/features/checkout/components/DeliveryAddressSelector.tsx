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
  CheckCircle2,
  AlertCircle,
  Compass,
  User,
  Phone,
  ShieldCheck,
  Truck,
  RotateCcw,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Skeleton } from '@/shared/components/ui/Skeleton';

export interface DeliveryAddressSelectorProps {
  store: PublicStore;
  value: DeliveryFormData | null;
  onChange: (data: DeliveryFormData | null, isValid: boolean) => void;
  disabled?: boolean;
  defaultRecipientName?: string;
  defaultRecipientPhone?: string;
}

export function DeliveryAddressSelector({
  store,
  onChange,
  disabled = false,
  defaultRecipientName,
  defaultRecipientPhone,
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
  const [latitudeInput, setLatitudeInput] = useState('');
  const [longitudeInput, setLongitudeInput] = useState('');
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
    if (defaultRecipientPhone && !recipientPhone) {
      setRecipientPhone(defaultRecipientPhone);
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
      const cleanPhone = recipientPhone.replace(/[\s-]/g, '');
      const isPhoneValid = /^(?:\+?51)?9\d{8}$/.test(cleanPhone);
      const isComplete =
        recipientName.trim().length >= 3 &&
        isPhoneValid &&
        Boolean(department) &&
        Boolean(province) &&
        Boolean(district) &&
        Boolean(ubigeoCode) &&
        addressLine.trim().length >= 5;

      const parsedLat = latitudeInput.trim() ? Number(latitudeInput) : null;
      const parsedLng = longitudeInput.trim() ? Number(longitudeInput) : null;
      const formData: DeliveryFormData = {
        mode: 'manual',
        alias: alias.trim() || undefined,
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        department,
        province,
        district,
        ubigeoCode,
        addressLine: addressLine.trim(),
        reference: reference.trim() || null,
        latitude: parsedLat !== null && Number.isFinite(parsedLat) ? parsedLat : null,
        longitude: parsedLng !== null && Number.isFinite(parsedLng) ? parsedLng : null,
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
    latitudeInput,
    longitudeInput,
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
    const parsedLat = latitudeInput.trim() ? Number(latitudeInput) : null;
    const parsedLng = longitudeInput.trim() ? Number(longitudeInput) : null;
    try {
      const created = await createAddress({
        alias: alias.trim() || 'Mi dirección',
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        department,
        province,
        district,
        ubigeoCode,
        streetAddress: addressLine.trim(),
        addressLine: addressLine.trim(),
        reference: reference.trim() || null,
        latitude: parsedLat !== null && Number.isFinite(parsedLat) ? parsedLat : null,
        longitude: parsedLng !== null && Number.isFinite(parsedLng) ? parsedLng : null,
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
    <div className="bg-[#061a1c] rounded-[12px] p-5 sm:p-7 border border-[#1e2c31] text-white space-y-5">
      {/* Header: Title & Store Delivery Fee */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-[#1e2c31]">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#36f4a4]" />
            <h3 className="text-base sm:text-lg font-medium text-white tracking-tight flex items-center gap-2">
              Dirección de Entrega
            </h3>
          </div>
          <p className="text-xs text-[#99b3ad] mt-0.5">
            Indica a dónde debe llegar tu pedido cuando el repartidor de {store.name} salga en camino.
          </p>
        </div>

        {store.deliveryEnabled && (
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-[#072720] border border-[#133b32] text-[#36f4a4]">
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
          className="p-4 rounded-[12px] bg-amber-950/40 border border-amber-500/40 flex items-start gap-3 text-xs text-amber-200"
        >
          <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" aria-hidden="true" />
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
          className="p-3.5 rounded-[12px] bg-[#02090a] border border-[#1e2c31] flex items-start gap-2.5 text-xs text-[#99b3ad]"
        >
          <RotateCcw className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" aria-hidden="true" />
          <div>
            <p className="font-bold text-white">La dirección seleccionada fue modificada o eliminada en tu cuenta.</p>
            <p className="mt-0.5">
              Hemos conservado los datos ingresados para que no los pierdas ({lastSelectedSnapshot?.addressLine}), o puedes seleccionar otra de tus direcciones guardadas.
            </p>
          </div>
        </div>
      )}

      {inlineSaveSuccess && (
        <div role="status" className="p-3 rounded-[12px] bg-[#072720] border border-[#36f4a4]/40 text-xs text-[#36f4a4]">
          {inlineSaveSuccess}
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-[12px] bg-[#072720]" />
        </div>
      ) : (
        <div className="space-y-4">
          {/* Selector de Modo: Direcciones Guardadas vs Nueva Dirección */}
          {addresses.length > 0 && (
            <div className="flex items-center gap-2 p-1 bg-[#02090a] rounded-full border border-[#1e2c31] max-w-md">
              <button
                type="button"
                onClick={() => setMode('saved')}
                className={cn(
                  'flex-1 py-1.5 px-3.5 rounded-full text-xs font-semibold transition-all cursor-pointer text-center',
                  mode === 'saved'
                    ? 'bg-white text-[#02090a]'
                    : 'text-[#99b3ad] hover:text-white'
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
                    ? 'bg-white text-[#02090a]'
                    : 'text-[#99b3ad] hover:text-white'
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
              className="grid grid-cols-1 sm:grid-cols-2 gap-3"
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
                      'relative p-4 rounded-[12px] border transition-all flex flex-col justify-between space-y-3',
                      isSelected
                        ? 'border-[#36f4a4] bg-[#072720] ring-1 ring-[#36f4a4]/40'
                        : 'border-[#1e2c31] bg-[#02090a] hover:border-white/30'
                    )}
                  >
                    <button
                      type="button"
                      role="radio"
                      aria-checked={isSelected}
                      onClick={() => handleSelectSaved(addr)}
                      disabled={disabled || !store.deliveryEnabled}
                      className="text-left cursor-pointer w-full space-y-1.5 focus-visible:outline-none"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-[#061a1c] border border-[#1e2c31] text-white">
                          <MapPin className="w-3 h-3 text-[#36f4a4]" aria-hidden="true" />
                          <span>{addr.alias}</span>
                        </span>

                        {addr.isDefault && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#36f4a4]">
                            <CheckCircle2 className="w-3 h-3 text-[#36f4a4]" aria-hidden="true" />
                            <span>Predeterminada</span>
                          </span>
                        )}
                      </div>

                      <p className="text-xs sm:text-sm font-medium text-white leading-snug line-clamp-2">
                        {addr.addressLine}
                      </p>
                      <p className="text-[11px] text-[#99b3ad]">{locationLabel}</p>

                      {addr.reference && (
                        <div className="flex items-start gap-1 text-[11px] text-[#9dabad] italic">
                          <Compass className="w-3 h-3 mt-0.5 shrink-0 text-[#99b3ad]" aria-hidden="true" />
                          <span className="line-clamp-1">{addr.reference}</span>
                        </div>
                      )}
                    </button>

                    <div className="pt-2 border-t border-[#1e2c31] flex items-center justify-between gap-2 text-[11px] text-[#99b3ad]">
                      <span className="font-medium text-white truncate">{addr.recipientName}</span>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="font-mono">{addr.recipientPhone}</span>
                        {!addr.isDefault && (
                          <button
                            type="button"
                            disabled={disabled || isSavingInline}
                            onClick={() => void setDefaultAddress?.(addr.id)}
                            className="text-[#36f4a4] hover:underline cursor-pointer disabled:opacity-50"
                          >
                            Hacer predeterminada
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* MODO 2: Formulario de Dirección Manual / Nueva Dirección */}
          {mode === 'manual' && (
            <div className="space-y-4 pt-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Nombre de quien recibe */}
                <div>
                  <label htmlFor="chk-recipient-name" className="block text-xs font-medium text-[#99b3ad] mb-1">
                    Nombre del destinatario <span className="text-rose-400">*</span>
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-[#99b3ad] absolute left-3 top-2.5 pointer-events-none" aria-hidden="true" />
                    <input
                      id="chk-recipient-name"
                      type="text"
                      value={recipientName}
                      onChange={(e) => setRecipientName(e.target.value)}
                      placeholder="Nombre y apellido completo"
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none"
                      maxLength={100}
                    />
                  </div>
                </div>

                {/* Teléfono */}
                <div>
                  <label htmlFor="chk-recipient-phone" className="block text-xs font-medium text-[#99b3ad] mb-1">
                    Teléfono celular <span className="text-rose-400">*</span>
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 text-[#99b3ad] absolute left-3 top-2.5 pointer-events-none" aria-hidden="true" />
                    <input
                      id="chk-recipient-phone"
                      type="tel"
                      value={recipientPhone}
                      onChange={(e) => setRecipientPhone(e.target.value)}
                      placeholder="987654321"
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none"
                      maxLength={15}
                    />
                  </div>
                </div>
              </div>

              {/* Cascada UBIGEO Oficial */}
              <div className="p-3.5 rounded-[12px] bg-[#02090a] border border-[#1e2c31] space-y-3">
                <span className="text-[11px] font-semibold text-[#36f4a4] uppercase tracking-[0.06em] block">
                  Ubicación de entrega (Perú)
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <div>
                    <label htmlFor="chk-department" className="block text-[11px] font-medium text-[#99b3ad] mb-1">
                      Departamento <span className="text-rose-400">*</span>
                    </label>
                    <select
                      id="chk-department"
                      value={department}
                      onChange={(e) => handleDepartmentChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full px-2.5 py-1.5 text-xs bg-[#061a1c] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none"
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
                    <label htmlFor="chk-province" className="block text-[11px] font-medium text-[#99b3ad] mb-1">
                      Provincia <span className="text-rose-400">*</span>
                    </label>
                    <select
                      id="chk-province"
                      value={province}
                      onChange={(e) => handleProvinceChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !department}
                      className="w-full px-2.5 py-1.5 text-xs bg-[#061a1c] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none disabled:opacity-50"
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
                    <label htmlFor="chk-district" className="block text-[11px] font-medium text-[#99b3ad] mb-1">
                      Distrito <span className="text-rose-400">*</span>
                    </label>
                    <select
                      id="chk-district"
                      value={district}
                      onChange={(e) => handleDistrictChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !province}
                      className="w-full px-2.5 py-1.5 text-xs bg-[#061a1c] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none disabled:opacity-50"
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
                  <p className="text-[10px] text-[#99b3ad]">
                    UBIGEO validado: <span className="font-mono font-semibold text-[#36f4a4]">{ubigeoCode}</span>
                  </p>
                )}
              </div>

              {/* Dirección exacta */}
              <div>
                <label htmlFor="chk-address-line" className="block text-xs font-medium text-[#99b3ad] mb-1">
                  Dirección exacta (Calle, Nro, Interior) <span className="text-rose-400">*</span>
                </label>
                <input
                  id="chk-address-line"
                  type="text"
                  value={addressLine}
                  onChange={(e) => setAddressLine(e.target.value)}
                  placeholder="Ej. Jr. Huancavelica 340, Dpto 201"
                  disabled={disabled || !store.deliveryEnabled}
                  className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none"
                  maxLength={255}
                />
              </div>

              {/* Referencia + Coordenadas opcionales */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-3">
                  <label htmlFor="chk-reference" className="block text-xs font-medium text-[#99b3ad] mb-1">
                    Referencia de entrega (opcional)
                  </label>
                  <input
                    id="chk-reference"
                    type="text"
                    value={reference}
                    onChange={(e) => setReference(e.target.value)}
                    placeholder="Ej. Frente a la farmacia, portón marrón"
                    disabled={disabled || !store.deliveryEnabled}
                    className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none"
                    maxLength={255}
                  />
                </div>
                <div>
                  <label htmlFor="chk-latitude" className="block text-[11px] font-medium text-[#9dabad] mb-1">
                    Latitud (opcional)
                  </label>
                  <input
                    id="chk-latitude"
                    type="number"
                    step="any"
                    value={latitudeInput}
                    onChange={(e) => setLatitudeInput(e.target.value)}
                    placeholder="-12.0464"
                    disabled={disabled || !store.deliveryEnabled}
                    className="w-full px-3 py-1.5 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none font-mono"
                  />
                </div>
                <div>
                  <label htmlFor="chk-longitude" className="block text-[11px] font-medium text-[#9dabad] mb-1">
                    Longitud (opcional)
                  </label>
                  <input
                    id="chk-longitude"
                    type="number"
                    step="any"
                    value={longitudeInput}
                    onChange={(e) => setLongitudeInput(e.target.value)}
                    placeholder="-77.0428"
                    disabled={disabled || !store.deliveryEnabled}
                    className="w-full px-3 py-1.5 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none font-mono"
                  />
                </div>
              </div>

              {inlineSaveError && (
                <p role="alert" className="text-xs text-rose-300">
                  {inlineSaveError}
                </p>
              )}

              {/* Checkbox para guardar opcionalmente en Mis Direcciones + botón inline */}
              <div className="pt-2 border-t border-[#1e2c31] space-y-3">
                <label className="flex items-start gap-2.5 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={canAddAddress && saveToAddresses}
                    disabled={disabled || !canAddAddress || !store.deliveryEnabled}
                    onChange={(e) => setSaveToAddresses(e.target.checked)}
                    className="mt-0.5 w-4 h-4 rounded-[4px] border-[#1e2c31] bg-[#02090a] text-[#36f4a4] focus:ring-[#36f4a4] accent-[#36f4a4]"
                  />
                  <span className="text-xs text-white">
                    <span className="font-semibold block">Guardar esta dirección en Mis Direcciones</span>
                    <span className="text-[#99b3ad] text-[11px]">
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
                      className="w-full max-w-xs px-3 py-1.5 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white placeholder-[#71717a] outline-none"
                      maxLength={50}
                    />
                    <button
                      type="button"
                      disabled={
                        disabled ||
                        isSavingInline ||
                        !recipientName.trim() ||
                        !recipientPhone.trim() ||
                        !ubigeoCode ||
                        addressLine.trim().length < 5
                      }
                      onClick={() => void handleSaveAddressInline()}
                      className="rounded-full border border-white/30 px-4 py-1.5 text-xs font-semibold text-white hover:bg-white/10 disabled:opacity-40 cursor-pointer"
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
      <div className="pt-2 text-[11px] text-[#99b3ad] flex items-center justify-between gap-2 border-t border-[#1e2c31]">
        <span className="flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
          <span>La entrega se coordinará directamente según el radio de despacho de {store.name}.</span>
        </span>
      </div>
    </div>
  );
}
