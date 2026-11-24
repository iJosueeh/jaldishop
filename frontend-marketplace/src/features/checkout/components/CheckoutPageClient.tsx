'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { ArrowLeft, ShoppingBag, FileText, IdCard, Sparkles } from 'lucide-react';
import { toast } from 'sonner';
import { Container } from '@/shared/components/ui/Container';
import { useStorefrontCart } from '@/features/storefront/cart/StorefrontCartProvider';
import { useCustomerProfile } from '@/features/customer/hooks/useCustomerProfile';
import { storeService } from '@/features/storefront/services/storeService';
import { PublicStore, PublicStoreSettings } from '@/features/storefront/types/storefront.types';
import {
  capacityService,
  getBusinessTodayDate,
  mapBackendSlotsToPickerSlots,
} from '@/features/storefront/services/capacityService';
import { CapacitySlotPicker, CapacitySlot } from '@/features/storefront/components/CapacitySlotPicker';
import { DeliveryAddressSelector } from './DeliveryAddressSelector';
import { CheckoutOrderSummary } from './CheckoutOrderSummary';
import { CheckoutPaymentSection } from './CheckoutPaymentSection';
import {
  CheckoutFulfillmentType,
  CheckoutResponse,
  DeliveryFormData,
  InitiateCheckoutPayload,
} from '../types/checkout.types';
import { checkoutService, isValidMercadoPagoUrl } from '../services/checkoutService';
import { estimateServiceFee } from '../utils/pricing';
import { mapDeliveryFormDataToCheckoutFields, parseSlotTimeRange } from '../utils/addressMapper';
import { customerAddressService } from '@/features/customer/services/customerAddressService';
import { WhatsAppBridgeModal } from '@/features/storefront/components/WhatsAppBridgeModal';

export function CheckoutPageClient() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const queryStoreSlug = searchParams.get('store');

  const {
    items: cart,
    count,
    subtotal,
    currency,
    store: cartStore,
    recentStores,
    selectStore,
    authenticated,
    loading: cartLoading,
  } = useStorefrontCart();

  const targetSlug = queryStoreSlug || cartStore?.slug || '';

  const [store, setStore] = useState<PublicStore | null>(null);
  const [storeSettings, setStoreSettings] = useState<PublicStoreSettings | null>(null);
  const [isLoadingStore, setIsLoadingStore] = useState(true);
  const [storeLoadError, setStoreLoadError] = useState<string | null>(null);

  const [fulfillment, setFulfillment] = useState<CheckoutFulfillmentType>('DELIVERY');
  const [selectedDate, setSelectedDate] = useState<string>(() => getBusinessTodayDate());
  const [slots, setSlots] = useState<CapacitySlot[]>([]);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<CapacitySlot | null>(null);
  const [retryCounter, setRetryCounter] = useState(0);

  const [deliveryData, setDeliveryData] = useState<DeliveryFormData | null>(null);
  const [isDeliveryValid, setIsDeliveryValid] = useState(false);

  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [customerDocumentNumber, setCustomerDocumentNumber] = useState('');
  const [orderNotes, setOrderNotes] = useState('');
  const [discountCode, setDiscountCode] = useState('');
  const [hasAutofilledFromAccount, setHasAutofilledFromAccount] = useState(false);
  const [serviceFeeRate, setServiceFeeRate] = useState(0);
  const [serviceFeeCap, setServiceFeeCap] = useState<number | null>(null);
  const hasAutofilledRef = useRef(false);

  useEffect(() => {
    let active = true;
    fetch('/api/platform-settings', { cache: 'no-store' })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (active && data) {
          if (typeof data.customerServiceFeeRate === 'number') {
            setServiceFeeRate(data.customerServiceFeeRate);
          }
          if (typeof data.customerServiceFeeCap === 'number') {
            setServiceFeeCap(data.customerServiceFeeCap);
          }
        }
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  const { data: customerProfile } = useCustomerProfile(authenticated);

  useEffect(() => {
    if (!customerProfile || hasAutofilledRef.current) return;
    hasAutofilledRef.current = true;
    const fullName = `${customerProfile.firstName || ''} ${customerProfile.lastName || ''}`.trim();
    let applied = false;
    if (fullName && !customerName) {
      setCustomerName(fullName);
      applied = true;
    }
    if (customerProfile.phone && !customerPhone) {
      setCustomerPhone(customerProfile.phone);
      applied = true;
    }
    if (customerProfile.email && !customerEmail) {
      setCustomerEmail(customerProfile.email);
      applied = true;
    }
    if (applied) {
      setHasAutofilledFromAccount(true);
    }
  }, [customerProfile, customerName, customerPhone, customerEmail]);

  const [checkoutResponse, setCheckoutResponse] = useState<CheckoutResponse | null>(null);
  const [isSubmittingCheckout, setIsSubmittingCheckout] = useState(false);
  const [isInitiatingPayment, setIsInitiatingPayment] = useState(false);
  const [isRedirectingToMercadoPago, setIsRedirectingToMercadoPago] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [isWhatsAppModalOpen, setIsWhatsAppModalOpen] = useState(false);

  // Load store + settings
  useEffect(() => {
    if (!targetSlug) {
      setIsLoadingStore(false);
      return;
    }
    let active = true;
    setIsLoadingStore(true);
    setStoreLoadError(null);

    storeService
      .getStoreBySlugClient(targetSlug)
      .then(async (loadedStore) => {
        if (!active) return;
        if (!loadedStore) {
          setStore(null);
          setStoreLoadError('No encontramos la tienda solicitada.');
          return;
        }
        setStore(loadedStore);
        selectStore({ id: loadedStore.id, name: loadedStore.name, slug: loadedStore.slug });

        if (!loadedStore.deliveryEnabled && loadedStore.pickupEnabled) {
          setFulfillment('PICKUP');
        } else if (!loadedStore.pickupEnabled && loadedStore.deliveryEnabled) {
          setFulfillment('DELIVERY');
        }

        const settings = await storeService.getPublicStoreSettingsClient(loadedStore.id).catch(() => null);
        if (active) setStoreSettings(settings);
      })
      .catch(() => {
        if (active) setStoreLoadError('No pudimos cargar la información de la tienda.');
      })
      .finally(() => {
        if (active) setIsLoadingStore(false);
      });

    return () => {
      active = false;
    };
  }, [targetSlug, selectStore]);

  // Fetch capacity slots
  useEffect(() => {
    if (!store?.id || !authenticated || count <= 0) {
      setSlots([]);
      setSelectedSlot(null);
      return;
    }

    let isCurrent = true;
    setIsLoadingSlots(true);
    setSlotsError(null);

    capacityService
      .getCapacitySlots(store.id, selectedDate)
      .then((res) => {
        if (!isCurrent) return;
        setSlots(mapBackendSlotsToPickerSlots(res.slots));
      })
      .catch((err) => {
        if (!isCurrent) return;
        setSlotsError(err instanceof Error ? err.message : 'No pudimos consultar los horarios.');
      })
      .finally(() => {
        if (isCurrent) setIsLoadingSlots(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [store?.id, selectedDate, authenticated, count, retryCounter]);

  const handleDeliveryChange = useCallback((data: DeliveryFormData | null, isValid: boolean) => {
    setDeliveryData(data);
    setIsDeliveryValid(isValid);
  }, []);

  const deliveryFee = fulfillment === 'DELIVERY' ? store?.deliveryFeeAmount ?? 0 : 0;
  const estimatedServiceFee = estimateServiceFee(subtotal, serviceFeeRate, serviceFeeCap);
  const totalAmount = subtotal + deliveryFee + estimatedServiceFee;

  const hasUnavailableItems = cart.some((item) => item.available === false);
  const minDeliveryAmount = storeSettings?.minDeliveryAmount ?? null;
  const isBelowMinDelivery =
    fulfillment === 'DELIVERY' &&
    minDeliveryAmount !== null &&
    minDeliveryAmount > 0 &&
    subtotal < minDeliveryAmount;

  const requiresDni = Boolean(storeSettings?.requireCustomerDni);
  const isDniValid = !requiresDni || /^\d{8,11}$/.test(customerDocumentNumber.trim());

  const isPickupContactValid =
    fulfillment === 'PICKUP'
      ? customerName.trim().length >= 2 && customerPhone.trim().length >= 6
      : true;

  const canProceed = Boolean(
    Boolean(store) &&
    authenticated &&
    count > 0 &&
    !hasUnavailableItems &&
    !isBelowMinDelivery &&
    Boolean(selectedSlot) &&
    isDniValid &&
    (fulfillment === 'DELIVERY' ? Boolean(deliveryData) && isDeliveryValid && store?.deliveryEnabled : isPickupContactValid && store?.pickupEnabled)
  );

  const ensureReservation = async (): Promise<CheckoutResponse | null> => {
    if (!store || !selectedSlot) return null;

    if (checkoutResponse) {
      const expiresMs = new Date(checkoutResponse.reservationExpiresAt).getTime();
      if (expiresMs > Date.now() + 5000) {
        return checkoutResponse;
      }
      setCheckoutResponse(null);
    }

    setIsSubmittingCheckout(true);
    setPaymentError(null);

    try {
      if (fulfillment === 'DELIVERY' && deliveryData?.mode === 'manual' && deliveryData.saveToAddresses) {
        await customerAddressService
          .createAddress({
            alias: deliveryData.alias || 'Mi dirección',
            recipientName: deliveryData.recipientName,
            recipientPhone: deliveryData.recipientPhone,
            department: deliveryData.department,
            province: deliveryData.province,
            district: deliveryData.district,
            ubigeoCode: deliveryData.ubigeoCode,
            streetAddress: deliveryData.addressLine,
            addressLine: deliveryData.addressLine,
            reference: deliveryData.reference,
            latitude: deliveryData.latitude ?? null,
            longitude: deliveryData.longitude ?? null,
            isDefault: false,
          })
          .catch(() => null);
      }

      const serviceDate = selectedSlot.serviceDate || selectedDate || getBusinessTodayDate();
      const parsedTimes = parseSlotTimeRange(selectedSlot.timeRange);
      const startTime = selectedSlot.startTime || parsedTimes.startTime || null;
      const endTime = selectedSlot.endTime || parsedTimes.endTime || null;

      const deliveryFields =
        fulfillment === 'DELIVERY' && deliveryData
          ? mapDeliveryFormDataToCheckoutFields(deliveryData)
          : {
              deliveryAddress: null,
              deliveryReference: null,
              customerName: customerName.trim() || null,
              customerPhone: customerPhone.trim() || null,
              deliveryLatitude: null,
              deliveryLongitude: null,
            };

      const payload: InitiateCheckoutPayload = {
        storeId: store.id,
        fulfillmentType: fulfillment,
        serviceDate,
        startTime,
        endTime,
        customerName: deliveryFields.customerName || customerName.trim() || null,
        customerPhone: deliveryFields.customerPhone || customerPhone.trim() || null,
        customerEmail: customerEmail.trim() || null,
        deliveryAddress: deliveryFields.deliveryAddress || null,
        deliveryReference: deliveryFields.deliveryReference || null,
        deliveryLatitude: deliveryFields.deliveryLatitude ?? null,
        deliveryLongitude: deliveryFields.deliveryLongitude ?? null,
        ...(requiresDni ? { customerDocumentNumber: customerDocumentNumber.trim() } : {}),
        ...(storeSettings?.allowOrderNotes !== false && orderNotes.trim()
          ? { orderNotes: orderNotes.trim() }
          : {}),
        ...(discountCode.trim() ? { discountCode: discountCode.trim() } : {}),
      };

      const result = await checkoutService.initiateCheckout(payload);
      setCheckoutResponse(result);
      return result;
    } catch (err: unknown) {
      const errorObj = err as { status?: number; code?: string; message?: string };
      if (errorObj?.status === 401) {
        router.push(`/login?redirect=${encodeURIComponent(`/checkout?store=${store.slug}`)}`);
        return null;
      }
      const msg = err instanceof Error ? err.message : 'No pudimos iniciar el checkout.';
      setPaymentError(msg);
      toast.error(msg);
      return null;
    } finally {
      setIsSubmittingCheckout(false);
    }
  };

  const handlePayWithMercadoPago = async () => {
    if (!canProceed) {
      if (!isDniValid) {
        setPaymentError('Ingresa un número de DNI/RUC válido (8 a 11 dígitos).');
      } else if (isBelowMinDelivery) {
        setPaymentError('Tu pedido no alcanza el monto mínimo de delivery.');
      } else {
        setPaymentError('Completa tu horario y datos de entrega antes de continuar.');
      }
      return;
    }

    const reservation = await ensureReservation();
    if (!reservation?.reservationId || !store) return;

    setIsInitiatingPayment(true);
    setPaymentError(null);

    try {
      const pref = await checkoutService.createPaymentPreference({
        capacityReservationId: reservation.reservationId,
        fulfillmentType: fulfillment,
        customerName:
          deliveryData?.recipientName ||
          reservation.customerInfo?.customerName ||
          customerName.trim() ||
          null,
        customerPhone:
          deliveryData?.recipientPhone ||
          reservation.customerInfo?.customerPhone ||
          customerPhone.trim() ||
          null,
        customerEmail: customerEmail.trim() || reservation.customerInfo?.customerEmail || null,
        deliveryAddress: deliveryData?.addressLine || reservation.customerInfo?.deliveryAddress || null,
        deliveryReference: deliveryData?.reference || reservation.customerInfo?.deliveryReference || null,
        deliveryLatitude: deliveryData?.latitude ?? null,
        deliveryLongitude: deliveryData?.longitude ?? null,
        storeId: store.id,
        storeName: store.name,
      });

      if (!isValidMercadoPagoUrl(pref.initPoint)) {
        setPaymentError('La URL de Mercado Pago recibida no es válida.');
        return;
      }

      setIsRedirectingToMercadoPago(true);
      toast.success('Conectando con Mercado Pago...');
      if (typeof window !== 'undefined') {
        window.location.assign(pref.initPoint);
      }
    } catch (err: unknown) {
      const errorObj = err as { code?: string; status?: number; message?: string };
      if (errorObj?.code === 'RESERVATION_EXPIRED' || errorObj?.code === 'INVENTORY_RESERVATION_EXPIRED') {
        setCheckoutResponse(null);
      }
      const msg = err instanceof Error ? err.message : 'No se pudo generar el enlace de Mercado Pago.';
      setPaymentError(msg);
      toast.error(msg);
      setIsRedirectingToMercadoPago(false);
    } finally {
      setIsInitiatingPayment(false);
    }
  };

  const handleProceedCashOnDelivery = async () => {
    if (!canProceed) return;
    const reservation = await ensureReservation();
    if (reservation) {
      setIsWhatsAppModalOpen(true);
    }
  };

  if (isLoadingStore || cartLoading) {
    return (
      <div className="min-h-screen bg-[#02090a] text-white pt-28 sm:pt-32 pb-12">
        <Container size="full">
          <div role="status" className="rounded-[12px] border border-[#1e2c31] bg-[#061a1c] p-10 text-center text-sm text-[#99b3ad]">
            Preparando tu checkout...
          </div>
        </Container>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="min-h-screen bg-[#02090a] text-white pt-28 sm:pt-32 pb-14">
        <Container size="md">
          <div className="rounded-[12px] border border-[#1e2c31] bg-[#061a1c] p-8 sm:p-12 text-center space-y-4">
            <h1 className="font-display text-3xl font-light text-white">Inicia sesión para finalizar tu compra</h1>
            <p className="text-sm text-[#99b3ad] max-w-md mx-auto">
              Necesitas tu cuenta de cliente para seleccionar tu dirección, reservar horario y pagar con Mercado Pago.
            </p>
            <Link
              href={`/login?redirect=${encodeURIComponent(targetSlug ? `/checkout?store=${targetSlug}` : '/checkout')}`}
              className="inline-flex rounded-full bg-white px-7 py-3 text-sm font-semibold text-[#02090a] hover:bg-stone-100"
            >
              Ingresar como cliente
            </Link>
          </div>
        </Container>
      </div>
    );
  }

  if (storeLoadError || !store || count === 0) {
    return (
      <div className="min-h-screen bg-[#02090a] text-white pt-28 sm:pt-32 pb-14">
        <Container size="md">
          <div className="rounded-[12px] border border-[#1e2c31] bg-[#061a1c] p-8 sm:p-12 text-center space-y-4">
            <ShoppingBag className="h-10 w-10 text-[#36f4a4] mx-auto" aria-hidden="true" />
            <h1 className="font-display text-3xl font-light text-white">
              {storeLoadError || 'Tu carrito en esta tienda está vacío'}
            </h1>
            <p className="text-sm text-[#99b3ad]">
              Agrega productos antes de continuar al checkout.
            </p>
            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              {store && (
                <Link
                  href={`/tienda/${store.slug}`}
                  className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-[#02090a]"
                >
                  Volver a {store.name}
                </Link>
              )}
              <Link
                href="/carrito"
                className="rounded-full border border-white/30 px-6 py-3 text-sm font-semibold text-white hover:bg-white/10"
              >
                Ver mis carritos
              </Link>
            </div>
          </div>
        </Container>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#02090a] text-white pt-28 sm:pt-32 pb-12">
      <Container size="full" className="space-y-8">
        {/* Header */}
        <header className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#1e2c31] pb-6">
          <div className="space-y-1.5">
            <Link
              href={`/tienda/${store.slug}`}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-[#36f4a4] hover:underline"
            >
              <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Volver a {store.name}</span>
            </Link>
            <h1 className="font-display text-3xl sm:text-5xl font-light tracking-[0.02em] text-white">
              Checkout · {store.name}
            </h1>
          </div>

          {recentStores.length > 1 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs text-[#99b3ad]">Cambiar tienda:</span>
              {recentStores.map((entry) => (
                <button
                  key={entry.id}
                  type="button"
                  onClick={() => router.push(`/checkout?store=${encodeURIComponent(entry.slug)}`)}
                  className={`rounded-full px-3 py-1 text-xs font-medium cursor-pointer ${
                    entry.id === store.id
                      ? 'bg-white text-[#02090a]'
                      : 'border border-[#1e2c31] text-[#99b3ad] hover:text-white'
                  }`}
                >
                  {entry.name}
                </button>
              ))}
            </div>
          )}
        </header>

        {/* Asymmetric 7 / 5 Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (7 cols): Fulfillment, Address, Capacity Slot, Operational Fields, Payment */}
          <div className="lg:col-span-7 space-y-6">
            <CapacitySlotPicker
              slots={slots}
              selectedSlotId={selectedSlot?.id ?? null}
              onSelectSlot={(slot) => {
                setSelectedSlot(slot);
                setCheckoutResponse(null);
              }}
              fulfillmentType={fulfillment}
              onFulfillmentTypeChange={(next) => {
                setFulfillment(next);
                setCheckoutResponse(null);
              }}
              selectedDate={selectedDate}
              onDateChange={(date) => {
                setSelectedDate(date);
                setSelectedSlot(null);
                setCheckoutResponse(null);
              }}
              isLoading={isLoadingSlots}
              error={slotsError}
              onRetry={() => setRetryCounter((c) => c + 1)}
              minDate={getBusinessTodayDate()}
              deliveryEnabled={store.deliveryEnabled}
              pickupEnabled={store.pickupEnabled}
            />

            {fulfillment === 'DELIVERY' ? (
              <DeliveryAddressSelector
                store={store}
                value={deliveryData}
                onChange={handleDeliveryChange}
                defaultRecipientName={customerName}
                defaultRecipientPhone={customerPhone}
              />
            ) : (
              <section className="bg-[#061a1c] rounded-[12px] p-5 sm:p-7 border border-[#1e2c31] space-y-4">
                <h3 className="text-base sm:text-lg font-medium text-white">
                  Datos para el retiro en tienda
                </h3>
                {store.address && (
                  <p className="text-xs text-[#99b3ad]">
                    Punto de retiro: <strong className="text-white">{store.address}</strong>
                  </p>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="pickup-customer-name" className="block text-xs font-medium text-[#99b3ad] mb-1">
                      Nombre completo *
                    </label>
                    <input
                      id="pickup-customer-name"
                      type="text"
                      value={customerName}
                      onChange={(e) => setCustomerName(e.target.value)}
                      placeholder="Tu nombre completo"
                      className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="pickup-customer-phone" className="block text-xs font-medium text-[#99b3ad] mb-1">
                      Teléfono celular *
                    </label>
                    <input
                      id="pickup-customer-phone"
                      type="tel"
                      value={customerPhone}
                      onChange={(e) => setCustomerPhone(e.target.value)}
                      placeholder="987654321"
                      className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none"
                    />
                  </div>
                </div>
              </section>
            )}

            {/* Operational Fields: Email, DNI (if required), Order Notes */}
            <section className="bg-[#061a1c] rounded-[12px] p-5 sm:p-7 border border-[#1e2c31] space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-base sm:text-lg font-medium text-white">
                  Datos de comprobante y notas
                </h3>
                {hasAutofilledFromAccount && (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-[#072720] border border-[#133b32] px-2.5 py-0.5 text-[11px] font-medium text-[#36f4a4]">
                    <Sparkles className="h-3 w-3" aria-hidden="true" />
                    <span>Completado desde tu cuenta</span>
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="checkout-email" className="block text-xs font-medium text-[#99b3ad] mb-1">
                    Correo electrónico (opcional)
                  </label>
                  <input
                    id="checkout-email"
                    type="email"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                    placeholder="cliente@correo.com"
                    className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none"
                  />
                </div>

                {requiresDni && (
                  <div>
                    <label htmlFor="checkout-dni" className="text-xs font-medium text-[#99b3ad] mb-1 flex items-center gap-1.5">
                      <IdCard className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
                      <span>DNI / RUC (Obligatorio) *</span>
                    </label>
                    <input
                      id="checkout-dni"
                      type="text"
                      inputMode="numeric"
                      maxLength={11}
                      value={customerDocumentNumber}
                      onChange={(e) => setCustomerDocumentNumber(e.target.value.replace(/\D/g, ''))}
                      placeholder="12345678"
                      className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none font-mono"
                    />
                    {customerDocumentNumber && !isDniValid && (
                      <p className="text-[11px] text-rose-400 mt-1">
                        Ingresa entre 8 y 11 dígitos numéricos.
                      </p>
                    )}
                  </div>
                )}
              </div>

              {storeSettings?.allowOrderNotes !== false && (
                <div>
                  <label htmlFor="checkout-order-notes" className="text-xs font-medium text-[#99b3ad] mb-1 flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-[#36f4a4]" aria-hidden="true" />
                    <span>Notas para el comercio (opcional)</span>
                  </label>
                  <textarea
                    id="checkout-order-notes"
                    rows={2}
                    maxLength={500}
                    value={orderNotes}
                    onChange={(e) => setOrderNotes(e.target.value)}
                    placeholder="Instrucciones especiales de preparación o entrega..."
                    className="w-full px-3 py-2 text-xs bg-[#02090a] border border-[#1e2c31] focus:border-[#36f4a4] rounded-[4px] text-white outline-none resize-none"
                  />
                </div>
              )}
            </section>

            {/* Payment Section */}
            <CheckoutPaymentSection
              storeSettings={storeSettings}
              totalAmount={checkoutResponse?.pricing?.totalAmount ?? totalAmount}
              currency={checkoutResponse?.pricing?.currency ?? currency}
              canProceed={canProceed}
              isSubmittingCheckout={isSubmittingCheckout}
              isInitiatingPayment={isInitiatingPayment}
              isRedirectingToMercadoPago={isRedirectingToMercadoPago}
              paymentError={paymentError}
              hasActiveReservation={Boolean(checkoutResponse)}
              onPayWithMercadoPago={handlePayWithMercadoPago}
              onProceedCashOnDelivery={
                storeSettings?.cashOnDeliveryEnabled ? handleProceedCashOnDelivery : undefined
              }
            />
          </div>

          {/* Right Column (5 cols): Sticky Order Summary */}
          <div className="lg:col-span-5">
            <CheckoutOrderSummary
              storeName={store.name}
              items={cart}
              currency={currency}
              subtotal={subtotal}
              deliveryFee={store.deliveryFeeAmount ?? 0}
              serviceFeeRate={serviceFeeRate}
              serviceFeeCap={serviceFeeCap}
              fulfillmentType={fulfillment}
              storeSettings={storeSettings}
              checkoutResponse={checkoutResponse}
              discountCode={discountCode}
              onDiscountCodeChange={setDiscountCode}
              onApplyDiscount={() => {
                setCheckoutResponse(null);
                void ensureReservation();
              }}
              onReservationExpired={() => {
                setCheckoutResponse(null);
                setPaymentError('Tu reserva de horario expiró. Vuelve a confirmar para generar una nueva.');
              }}
            />
          </div>
        </div>
      </Container>

      <WhatsAppBridgeModal
        isOpen={isWhatsAppModalOpen}
        onClose={() => setIsWhatsAppModalOpen(false)}
        cart={cart}
        selectedSlot={selectedSlot}
        fulfillmentType={fulfillment}
        storeName={store.name}
        storePhone={store.whatsappNumber || store.phone || undefined}
        deliveryFee={deliveryFee}
        reservationId={checkoutResponse?.reservationId}
      />
    </div>
  );
}
