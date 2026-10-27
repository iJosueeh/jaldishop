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
  Plus,
  Compass,
  User,
  Phone,
  BookmarkCheck,
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
}

export function DeliveryAddressSelector({
  store,
  value,
  onChange,
  disabled = false,
}: DeliveryAddressSelectorProps) {
  const {
    addresses,
    total,
    defaultAddress,
    canAddAddress,
    isLoading,
  } = useCustomerAddresses();

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
          addressLine: activeAddr.addressLine,
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

  // Check if previously selected address was deleted in another session/tab
  const isAddressDeletedRemotely =
    mode === 'saved' &&
    selectedAddressId &&
    !addresses.some((a) => a.id === selectedAddressId) &&
    Boolean(lastSelectedSnapshot);

  return (
    <div className="bg-white rounded-[12px] p-5 sm:p-7 border border-stone-200/90 shadow-sm space-y-5">
      {/* Header: Title & Store Delivery Fee */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-stone-100">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#005141]" />
            <h3 className="text-base sm:text-lg font-black text-stone-900 tracking-tight flex items-center gap-2">
              Dirección de Entrega
            </h3>
          </div>
          <p className="text-xs text-stone-600 mt-0.5">
            Indica a dónde debe llegar tu pedido cuando el repartidor de {store.name} salga en camino.
          </p>
        </div>

        {store.deliveryEnabled && (
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-[#faf7f2] border border-stone-200 text-[#005141]">
              <Truck className="w-3.5 h-3.5" />
              <span>Costo de envío: {formatCurrency(store.deliveryFeeAmount ?? 0, 'PEN')}</span>
            </span>
          </div>
        )}
      </div>

      {/* Alerta: Si la tienda no realiza envíos a domicilio */}
      {!store.deliveryEnabled && (
        <div
          role="alert"
          className="p-4 rounded-xl bg-amber-50 border border-amber-200/90 flex items-start gap-3 text-xs text-amber-900"
        >
          <AlertCircle className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
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
          className="p-3.5 rounded-xl bg-stone-50 border border-stone-300 flex items-start gap-2.5 text-xs text-stone-700"
        >
          <RotateCcw className="w-4 h-4 text-[#ea580c] shrink-0 mt-0.5" />
          <div>
            <p className="font-bold text-stone-900">La dirección seleccionada fue modificada o eliminada en tu cuenta.</p>
            <p className="text-stone-600 mt-0.5">
              Hemos conservado los datos ingresados para que no los pierdas ({lastSelectedSnapshot?.addressLine}), o puedes seleccionar otra de tus direcciones guardadas.
            </p>
          </div>
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-xl bg-stone-200" />
        </div>
      ) : (
        <div className="space-y-4">
          {/* Selector de Modo: Direcciones Guardadas vs Nueva Dirección */}
          {addresses.length > 0 && (
            <div className="flex items-center gap-2 p-1 bg-stone-100/80 rounded-full border border-stone-200/80 max-w-md">
              <button
                type="button"
                onClick={() => setMode('saved')}
                className={cn(
                  'flex-1 py-1.5 px-3.5 rounded-full text-xs font-bold transition-all cursor-pointer text-center',
                  mode === 'saved'
                    ? 'bg-[#005141] text-white shadow-xs'
                    : 'text-stone-600 hover:text-stone-900'
                )}
              >
                Mis direcciones guardadas ({total})
              </button>
              <button
                type="button"
                onClick={() => setMode('manual')}
                className={cn(
                  'flex-1 py-1.5 px-3.5 rounded-full text-xs font-bold transition-all cursor-pointer text-center',
                  mode === 'manual'
                    ? 'bg-[#005141] text-white shadow-xs'
                    : 'text-stone-600 hover:text-stone-900'
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
                  <button
                    key={addr.id}
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    onClick={() => handleSelectSaved(addr)}
                    disabled={disabled || !store.deliveryEnabled}
                    className={cn(
                      'relative p-4 rounded-xl text-left border transition-all cursor-pointer flex flex-col justify-between space-y-3',
                      isSelected
                        ? 'border-[#005141] bg-emerald-50/50 ring-2 ring-[#005141]/20 shadow-xs'
                        : 'border-stone-200/90 bg-white hover:border-stone-300 hover:bg-stone-50/60 shadow-2xs'
                    )}
                  >
                    <div className="space-y-1.5 w-full">
                      <div className="flex items-center justify-between gap-2">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-white border border-stone-200 text-stone-800">
                          <MapPin className="w-3 h-3 text-[#005141]" />
                          <span>{addr.alias}</span>
                        </span>

                        {addr.isDefault && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#005141]">
                            <CheckCircle2 className="w-3 h-3 text-[#005141]" />
                            <span>Predeterminada</span>
                          </span>
                        )}
                      </div>

                      <p className="text-xs sm:text-sm font-bold text-stone-900 leading-snug line-clamp-2">
                        {addr.addressLine}
                      </p>
                      <p className="text-[11px] text-stone-600">{locationLabel}</p>

                      {addr.reference && (
                        <div className="flex items-start gap-1 text-[11px] text-stone-500 italic">
                          <Compass className="w-3 h-3 mt-0.5 shrink-0 text-stone-400" />
                          <span className="line-clamp-1">{addr.reference}</span>
                        </div>
                      )}
                    </div>

                    <div className="pt-2 border-t border-stone-100 flex items-center justify-between text-[11px] text-stone-600">
                      <span className="font-semibold text-stone-800 truncate">{addr.recipientName}</span>
                      <span className="text-stone-500 font-mono">{addr.recipientPhone}</span>
                    </div>
                  </button>
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
                  <label htmlFor="chk-recipient-name" className="block text-xs font-bold text-stone-700 mb-1">
                    Nombre del destinatario <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-stone-400 absolute left-3 top-2.5 pointer-events-none" />
                    <input
                      id="chk-recipient-name"
                      type="text"
                      value={recipientName}
                      onChange={(e) => setRecipientName(e.target.value)}
                      placeholder="Nombre y apellido completo"
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl text-stone-900 placeholder-stone-400 outline-none"
                      maxLength={100}
                    />
                  </div>
                </div>

                {/* Teléfono */}
                <div>
                  <label htmlFor="chk-recipient-phone" className="block text-xs font-bold text-stone-700 mb-1">
                    Teléfono celular <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 text-stone-400 absolute left-3 top-2.5 pointer-events-none" />
                    <input
                      id="chk-recipient-phone"
                      type="tel"
                      value={recipientPhone}
                      onChange={(e) => setRecipientPhone(e.target.value)}
                      placeholder="987654321"
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full pl-9 pr-3 py-2 text-xs bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl text-stone-900 placeholder-stone-400 outline-none"
                      maxLength={15}
                    />
                  </div>
                </div>
              </div>

              {/* Cascada UBIGEO Oficial */}
              <div className="p-3.5 rounded-xl bg-[#faf7f2] border border-stone-200/80 space-y-3">
                <span className="text-[11px] font-bold text-stone-700 uppercase tracking-wider block">
                  Ubicación de entrega (Perú)
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <div>
                    <label htmlFor="chk-department" className="block text-[11px] font-semibold text-stone-600 mb-1">
                      Departamento <span className="text-rose-500">*</span>
                    </label>
                    <select
                      id="chk-department"
                      value={department}
                      onChange={(e) => handleDepartmentChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled}
                      className="w-full px-2.5 py-1.5 text-xs bg-white border border-stone-200 focus:border-[#005141] rounded-xl text-stone-900 outline-none"
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
                    <label htmlFor="chk-province" className="block text-[11px] font-semibold text-stone-600 mb-1">
                      Provincia <span className="text-rose-500">*</span>
                    </label>
                    <select
                      id="chk-province"
                      value={province}
                      onChange={(e) => handleProvinceChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !department}
                      className="w-full px-2.5 py-1.5 text-xs bg-white border border-stone-200 focus:border-[#005141] rounded-xl text-stone-900 outline-none disabled:opacity-50"
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
                    <label htmlFor="chk-district" className="block text-[11px] font-semibold text-stone-600 mb-1">
                      Distrito <span className="text-rose-500">*</span>
                    </label>
                    <select
                      id="chk-district"
                      value={district}
                      onChange={(e) => handleDistrictChange(e.target.value)}
                      disabled={disabled || !store.deliveryEnabled || !province}
                      className="w-full px-2.5 py-1.5 text-xs bg-white border border-stone-200 focus:border-[#005141] rounded-xl text-stone-900 outline-none disabled:opacity-50"
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
                  <p className="text-[10px] text-stone-500">
                    UBIGEO validado: <span className="font-mono font-semibold text-stone-700">{ubigeoCode}</span>
                  </p>
                )}
              </div>

              {/* Dirección exacta */}
              <div>
                <label htmlFor="chk-address-line" className="block text-xs font-bold text-stone-700 mb-1">
                  Dirección exacta (Calle, Nro, Interior) <span className="text-rose-500">*</span>
                </label>
                <input
                  id="chk-address-line"
                  type="text"
                  value={addressLine}
                  onChange={(e) => setAddressLine(e.target.value)}
                  placeholder="Ej. Jr. Huancavelica 340, Dpto 201"
                  disabled={disabled || !store.deliveryEnabled}
                  className="w-full px-3 py-2 text-xs bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl text-stone-900 placeholder-stone-400 outline-none"
                  maxLength={255}
                />
              </div>

              {/* Referencia opcional */}
              <div>
                <label htmlFor="chk-reference" className="block text-xs font-bold text-stone-700 mb-1">
                  Referencia de entrega (opcional)
                </label>
                <input
                  id="chk-reference"
                  type="text"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  placeholder="Ej. Frente a la farmacia, portón marrón"
                  disabled={disabled || !store.deliveryEnabled}
                  className="w-full px-3 py-2 text-xs bg-white border border-stone-200 focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/10 rounded-xl text-stone-900 placeholder-stone-400 outline-none"
                  maxLength={255}
                />
              </div>

              {/* Checkbox para guardar opcionalmente en Mis Direcciones */}
              <div className="pt-2 border-t border-stone-100 space-y-2">
                <label className="flex items-start gap-2.5 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={canAddAddress && saveToAddresses}
                    disabled={disabled || !canAddAddress || !store.deliveryEnabled}
                    onChange={(e) => setSaveToAddresses(e.target.checked)}
                    className="mt-0.5 w-4 h-4 rounded border-stone-300 text-[#005141] focus:ring-[#005141] accent-[#005141]"
                  />
                  <span className="text-xs text-stone-700">
                    <span className="font-semibold block">Guardar esta dirección en Mis Direcciones</span>
                    <span className="text-stone-500 text-[11px]">
                      {canAddAddress
                        ? 'Podrás reutilizarla rápidamente en tus próximas compras sin volver a escribirla.'
                        : 'Has alcanzado el límite de 5 direcciones. Puedes usar esta dirección para este pedido sin guardarla.'}
                    </span>
                  </span>
                </label>

                {saveToAddresses && canAddAddress && (
                  <div className="pl-6 pt-1 max-w-xs">
                    <input
                      type="text"
                      value={alias}
                      onChange={(e) => setAlias(e.target.value)}
                      placeholder="Alias (ej. Playa, Taller)"
                      className="w-full px-3 py-1.5 text-xs bg-white border border-stone-200 focus:border-[#005141] rounded-lg text-stone-900 placeholder-stone-400 outline-none"
                      maxLength={50}
                    />
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Nota de Cobertura y Contacto Directo */}
      <div className="pt-2 text-[11px] text-stone-500 flex items-center justify-between gap-2 border-t border-stone-100">
        <span className="flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" />
          <span>La entrega se coordinará directamente según el radio de despacho de {store.name}.</span>
        </span>
      </div>
    </div>
  );
}
