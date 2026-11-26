'use client';

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { useQueries, useQuery } from '@tanstack/react-query';
import { ArrowLeft, ArrowDown, ShoppingBag, FileText, IdCard, Sparkles } from 'lucide-react';
import { notify, notifyKeys } from '@/shared/utils/notify';
import { Container } from '@/shared/components/ui/Container';
import { formatCurrency } from '@/shared/utils/formatters';
import { useStorefrontCart } from '@/features/storefront/cart/StorefrontCartProvider';
import { cartService } from '@/features/storefront/cart/services/cartService';
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
import { PeruPhoneInput } from '@/shared/components/ui/PeruPhoneInput';
import {
  toLocalPeruPhone,
  validateLocalPeruPhone,
} from '@/features/auth/utils/validation';

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

  const storeCartQueries = useQueries({
    queries: recentStores.map((entry) => ({
      queryKey: ['customer-cart', entry.id],
      queryFn: () => cartService.get(entry.id),
      enabled: authenticated,
      retry: false,
      staleTime: 0,
    })),
  });

  const populatedStores = useMemo(
    () =>
      recentStores.filter(
        (_entry, idx) => (storeCartQueries[idx]?.data?.totalItems ?? 0) > 0
      ),
    [recentStores, storeCartQueries]
  );

  const {
    data: store = null,
    isLoading: isStoreQueryLoading,
    isError: isStoreQueryError,
  } = useQuery<PublicStore | null>({
    queryKey: ['checkout-store', targetSlug],
    queryFn: () => storeService.getStoreBySlugClient(targetSlug),
    enabled: Boolean(targetSlug),
    staleTime: 60_000,
    retry: false,
  });

  const { data: storeSettings = null } = useQuery<PublicStoreSettings | null>({
    queryKey: ['checkout-store-settings', store?.id],
    queryFn: () => storeService.getPublicStoreSettingsClient(store!.id).catch(() => null),
    enabled: Boolean(store?.id),
    staleTime: 60_000,
    retry: false,
  });

  const isLoadingStore = Boolean(targetSlug) && isStoreQueryLoading;
  const storeLoadError = isStoreQueryError
    ? 'No pudimos cargar la información de la tienda.'
    : Boolean(targetSlug) && !isStoreQueryLoading && !store
    ? 'No encontramos la tienda solicitada.'
    : null;

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
  const [requestedReceiptType, setRequestedReceiptType] = useState<'BOLETA' | 'FACTURA'>('BOLETA');
  const [customerLegalName, setCustomerLegalName] = useState('');
  const [orderNotes, setOrderNotes] = useState('');
  const [discountCode, setDiscountCode] = useState('');
  const [appliedPromo, setAppliedPromo] = useState<{ code: string; discountAmount: number } | null>(null);
  const [promoError, setPromoError] = useState<string | null>(null);
  const [isValidatingPromo, setIsValidatingPromo] = useState(false);
  const validatedSubtotalRef = useRef<number | null>(null);
  const restoredDraftStoreRef = useRef<string | null>(null);
  const draftSlotIdRef = useRef<string | null>(null);
  const [hasAutofilledFromAccount, setHasAutofilledFromAccount] = useState(false);
  const [activePaymentSession, setActivePaymentSession] = useState<{
    paymentId: string;
    initPoint: string;
    popupBlocked: boolean;
  } | null>(null);
  const hasAutofilledRef = useRef(false);

  const checkActivePaymentStatus = useCallback(
    async (paymentId: string) => {
      try {
        const res = await checkoutService.getPaymentStatus(paymentId);
        if ((res.orderCreated && res.orderNumber) || res.paymentStatus === 'FAILED') {
          router.push(`/checkout/success?payment_id=${encodeURIComponent(paymentId)}`);
        }
      } catch {
        // Silent retry on transient polling errors
      }
    },
    [router]
  );

  useEffect(() => {
    if (!activePaymentSession?.paymentId) return;
    const interval = setInterval(() => {
      void checkActivePaymentStatus(activePaymentSession.paymentId);
    }, 4000);
    return () => clearInterval(interval);
  }, [activePaymentSession?.paymentId, checkActivePaymentStatus]);

  const { data: platformSettingsData } = useQuery<{
    customerServiceFeeRate?: number;
    customerServiceFeeCap?: number | null;
  } | null>({
    queryKey: ['platform-settings'],
    queryFn: async () => {
      const res = await fetch('/api/platform-settings', { cache: 'no-store' });
      return res.ok ? res.json() : null;
    },
    staleTime: 60_000,
    retry: false,
  });

  const serviceFeeRate =
    typeof platformSettingsData?.customerServiceFeeRate === 'number'
      ? platformSettingsData.customerServiceFeeRate
      : 0;
  const serviceFeeCap =
    typeof platformSettingsData?.customerServiceFeeCap === 'number'
      ? platformSettingsData.customerServiceFeeCap
      : null;

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
    const localPhone = toLocalPeruPhone(customerProfile.phone);
    if (localPhone && !customerPhone) {
      setCustomerPhone(localPhone);
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

  // Sincronizar tienda seleccionada y modalidad por defecto cuando carga la tienda
  useEffect(() => {
    if (!store) return;
    selectStore({ id: store.id, name: store.name, slug: store.slug });
    if (!store.deliveryEnabled && store.pickupEnabled) {
      setFulfillment('PICKUP');
    } else if (!store.pickupEnabled && store.deliveryEnabled) {
      setFulfillment('DELIVERY');
    }
  }, [store, selectStore]);

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
        const mapped = mapBackendSlotsToPickerSlots(res.slots);
        setSlots(mapped);
        if (draftSlotIdRef.current) {
          const match = mapped.find(
            (s) => s.id === draftSlotIdRef.current && (s.status === 'available' || s.status === 'few_left')
          );
          if (match) {
            setSelectedSlot(match);
          }
          draftSlotIdRef.current = null;
        }
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

  const clearStoredReservation = useCallback(() => {
    if (!store?.id || !selectedSlot?.id) return;
    try {
      sessionStorage.removeItem(`jaldishop-checkout-reservation:${store.id}:${selectedSlot.id}`);
    } catch { /* Storage is optional */ }
  }, [store?.id, selectedSlot?.id]);

  // Restaurar borrador de checkout por tienda (horario, modalidad, cupón) al volver a la página
  useEffect(() => {
    if (!store?.id || restoredDraftStoreRef.current === store.id) return;
    restoredDraftStoreRef.current = store.id;
    try {
      const raw = sessionStorage.getItem(`jaldishop-checkout-draft:${store.id}`);
      if (!raw) return;
      const draft = JSON.parse(raw) as {
        selectedDate?: string;
        selectedSlotId?: string | null;
        fulfillment?: CheckoutFulfillmentType;
        discountCode?: string;
        appliedPromo?: { code: string; discountAmount: number } | null;
        subtotalSnapshot?: number;
      };
      if (draft.selectedDate) setSelectedDate(draft.selectedDate);
      if (draft.selectedSlotId) draftSlotIdRef.current = draft.selectedSlotId;
      if (
        draft.fulfillment &&
        ((draft.fulfillment === 'DELIVERY' && store.deliveryEnabled) ||
          (draft.fulfillment === 'PICKUP' && store.pickupEnabled))
      ) {
        setFulfillment(draft.fulfillment);
      }
      if (
        draft.appliedPromo?.code &&
        typeof draft.subtotalSnapshot === 'number' &&
        draft.subtotalSnapshot === subtotal
      ) {
        setDiscountCode(draft.appliedPromo.code);
        setAppliedPromo(draft.appliedPromo);
        validatedSubtotalRef.current = subtotal;
      }
    } catch { /* Storage is optional */ }
  }, [store?.id, store?.deliveryEnabled, store?.pickupEnabled, subtotal]);

  // Persistir borrador de checkout por tienda
  useEffect(() => {
    if (!store?.id || restoredDraftStoreRef.current !== store.id) return;
    try {
      sessionStorage.setItem(
        `jaldishop-checkout-draft:${store.id}`,
        JSON.stringify({
          selectedDate,
          selectedSlotId: selectedSlot?.id ?? null,
          fulfillment,
          discountCode,
          appliedPromo,
          subtotalSnapshot: subtotal,
        })
      );
    } catch { /* Storage is optional */ }
  }, [store?.id, selectedDate, selectedSlot?.id, fulfillment, discountCode, appliedPromo, subtotal]);

  useEffect(() => {
    if (validatedSubtotalRef.current !== null && validatedSubtotalRef.current !== subtotal) {
      setAppliedPromo(null);
      setPromoError(null);
      validatedSubtotalRef.current = null;
    }
  }, [subtotal]);

  useEffect(() => {
    if (!store?.id || !selectedSlot?.id) return;
    try {
      const raw = sessionStorage.getItem(`jaldishop-checkout-reservation:${store.id}:${selectedSlot.id}`);
      if (!raw) return;
      const parsed = JSON.parse(raw) as CheckoutResponse;
      const expiresMs = parsed?.reservationExpiresAt ? new Date(parsed.reservationExpiresAt).getTime() : 0;
      if (expiresMs > Date.now() + 5000) {
        setCheckoutResponse(parsed);
        if (parsed.pricing?.discountCode) {
          setDiscountCode(parsed.pricing.discountCode);
          setAppliedPromo({
            code: parsed.pricing.discountCode,
            discountAmount: Number(parsed.pricing.discountAmount || 0),
          });
        }
      } else {
        sessionStorage.removeItem(`jaldishop-checkout-reservation:${store.id}:${selectedSlot.id}`);
      }
    } catch { /* Storage is optional */ }
  }, [store?.id, selectedSlot?.id]);

  const handleDeliveryChange = useCallback((data: DeliveryFormData | null, isValid: boolean) => {
    setDeliveryData(data);
    setIsDeliveryValid(isValid);
  }, []);

  const deliveryFee = fulfillment === 'DELIVERY' ? store?.deliveryFeeAmount ?? 0 : 0;
  const discountPreview = appliedPromo ? Math.min(subtotal, Number(appliedPromo.discountAmount || 0)) : 0;
  const discountedSubtotal = Math.max(0, subtotal - discountPreview);
  const estimatedServiceFee = estimateServiceFee(discountedSubtotal, serviceFeeRate, serviceFeeCap);
  const totalAmount = Math.max(0, discountedSubtotal + deliveryFee + estimatedServiceFee);

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
      ? customerName.trim().length >= 2 &&
        validateLocalPeruPhone(customerPhone, true) === null
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

  const disabledHint = canProceed
    ? null
    : hasUnavailableItems
    ? 'Tienes productos sin stock disponible en tu carrito.'
    : isBelowMinDelivery
    ? 'Tu pedido aún no alcanza el monto mínimo de delivery.'
    : !selectedSlot
    ? 'Selecciona una franja horaria de entrega o retiro.'
    : fulfillment === 'DELIVERY' && !isDeliveryValid
    ? 'Selecciona o completa tu dirección de entrega.'
    : fulfillment === 'PICKUP' && !isPickupContactValid
    ? 'Ingresa tu nombre completo y teléfono para el retiro.'
    : !isDniValid
    ? 'Ingresa un DNI/RUC válido (8 a 11 dígitos).'
    : 'Completa los datos requeridos para continuar.';

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
            recipientPhone: toLocalPeruPhone(deliveryData.recipientPhone),
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
        customerPhone:
          toLocalPeruPhone(deliveryFields.customerPhone || customerPhone) || null,
        customerEmail: customerEmail.trim() || null,
        deliveryAddress: deliveryFields.deliveryAddress || null,
        deliveryReference: deliveryFields.deliveryReference || null,
        deliveryLatitude: deliveryFields.deliveryLatitude ?? null,
        deliveryLongitude: deliveryFields.deliveryLongitude ?? null,
        ...(requiresDni ? { customerDocumentNumber: customerDocumentNumber.trim() } : {}),
        ...(storeSettings?.allowOrderNotes !== false && orderNotes.trim()
          ? { orderNotes: orderNotes.trim() }
          : {}),
        ...(appliedPromo?.code || discountCode.trim()
          ? { discountCode: appliedPromo?.code || discountCode.trim() }
          : {}),
      };

      const result = await checkoutService.initiateCheckout(payload);
      setCheckoutResponse(result);
      try {
        sessionStorage.setItem(
          `jaldishop-checkout-reservation:${store.id}:${selectedSlot.id}`,
          JSON.stringify(result)
        );
      } catch { /* Storage is optional */ }
      return result;
    } catch (err: unknown) {
      const errorObj = err as { status?: number; code?: string; message?: string };
      if (errorObj?.status === 401) {
        router.push(`/login?redirect=${encodeURIComponent(`/checkout?store=${store.slug}`)}`);
        return null;
      }
      const msg = err instanceof Error ? err.message : 'No pudimos iniciar el checkout.';
      setPaymentError(msg);
      notify.error(msg, { id: notifyKeys.checkoutError() });
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
        returnBaseUrl: typeof window !== 'undefined' ? window.location.origin : undefined,
        discountCode: appliedPromo?.code || (discountCode.trim() ? discountCode.trim() : null),
        requestedReceiptType: store.canIssueInvoices ? requestedReceiptType : 'BOLETA',
        customerDocType:
          store.canIssueInvoices && requestedReceiptType === 'FACTURA'
            ? 'RUC'
            : customerDocumentNumber.trim().length === 11
              ? 'RUC'
              : customerDocumentNumber.trim().length === 8
                ? 'DNI'
                : null,
        customerDocNumber: customerDocumentNumber.trim() || null,
        customerLegalName:
          store.canIssueInvoices && requestedReceiptType === 'FACTURA' && customerLegalName.trim()
            ? customerLegalName.trim()
            : null,
      });

      if (!isValidMercadoPagoUrl(pref.initPoint)) {
        setPaymentError('La URL de Mercado Pago recibida no es válida.');
        return;
      }

      const openedTab =
        typeof window !== 'undefined'
          ? window.open(pref.initPoint, '_blank', 'noopener,noreferrer')
          : null;
      const popupBlocked = !openedTab;
      setActivePaymentSession({
        paymentId: pref.paymentId,
        initPoint: pref.initPoint,
        popupBlocked,
      });
      if (popupBlocked) {
        notify.info('Tu navegador bloqueó la nueva pestaña. Usa el enlace directo para abrir Mercado Pago.', {
          id: notifyKeys.mercadoPagoTab(),
        });
      } else {
        notify.success('Mercado Pago abierto en una nueva pestaña.', {
          id: notifyKeys.mercadoPagoTab(),
        });
      }
    } catch (err: unknown) {
      const errorObj = err as { code?: string; status?: number; message?: string };
      if (errorObj?.code === 'RESERVATION_EXPIRED' || errorObj?.code === 'INVENTORY_RESERVATION_EXPIRED') {
        setCheckoutResponse(null);
      }
      const msg = err instanceof Error ? err.message : 'No se pudo generar el enlace de Mercado Pago.';
      setPaymentError(msg);
      notify.error(msg, { id: notifyKeys.checkoutError() });
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
      <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-12">
        <Container size="full">
          <div role="status" className="rounded-[12px] border border-[#e7e0d6] bg-white p-10 text-center text-sm text-[#57534e]">
            Preparando tu checkout...
          </div>
        </Container>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-14">
        <Container size="md">
          <div className="rounded-[12px] border border-[#e7e0d6] bg-white p-8 sm:p-12 text-center space-y-4">
            <h1 className="font-display text-2xl sm:text-3xl font-semibold tracking-tight text-[#1c1917]">
              Inicia sesión para finalizar tu compra
            </h1>
            <p className="text-sm text-[#57534e] max-w-md mx-auto leading-relaxed">
              Necesitas tu cuenta de cliente para seleccionar tu dirección, reservar horario y pagar con Mercado Pago.
            </p>
            <Link
              href={`/login?redirect=${encodeURIComponent(targetSlug ? `/checkout?store=${targetSlug}` : '/checkout')}`}
              className="inline-flex rounded-full bg-[#005141] px-7 py-3 text-sm font-semibold text-white hover:bg-[#003d31] transition-colors"
            >
              Ingresar como cliente
            </Link>
          </div>
        </Container>
      </div>
    );
  }

  if (store && customerProfile?.ownedStoreId && customerProfile.ownedStoreId === store.id) {
    return (
      <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-14">
        <Container size="md">
          <div className="rounded-[12px] border border-amber-200 bg-white p-8 sm:p-12 text-center space-y-4">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-50 border border-amber-200">
              <ShoppingBag className="h-6 w-6 text-amber-700" aria-hidden="true" />
            </div>
            <h1 className="font-display text-2xl sm:text-3xl font-semibold tracking-tight text-[#1c1917]">
              No puedes comprar en tu propia tienda
            </h1>
            <p className="text-sm text-[#57534e] max-w-md mx-auto">
              Tu cuenta administra <strong>{store.name}</strong>. Por seguridad del marketplace, no está permitido realizar autocompras.
            </p>
            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              <Link
                href="/tiendas"
                className="rounded-full bg-[#005141] px-6 py-3 text-sm font-semibold text-white hover:bg-[#003d31] transition-colors"
              >
                Explorar otras tiendas
              </Link>
              <Link
                href="/carrito"
                className="rounded-full border border-[#e7e0d6] bg-white px-6 py-3 text-sm font-semibold text-[#1c1917] hover:bg-[#f7f3ec] transition-colors"
              >
                Ver mis carritos
              </Link>
            </div>
          </div>
        </Container>
      </div>
    );
  }

  if (storeLoadError || !store || count === 0) {
    return (
      <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-14">
        <Container size="md">
          <div className="rounded-[12px] border border-[#e7e0d6] bg-white p-8 sm:p-12 text-center space-y-4">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#f0fdfa] border border-[#ccfbf1]">
              <ShoppingBag className="h-6 w-6 text-[#005141]" aria-hidden="true" />
            </div>
            <h1 className="font-display text-2xl sm:text-3xl font-semibold tracking-tight text-[#1c1917]">
              {storeLoadError || 'Tu carrito en esta tienda está vacío'}
            </h1>
            <p className="text-sm text-[#57534e]">
              Agrega productos antes de continuar al checkout.
            </p>
            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              {store && (
                <Link
                  href={`/tienda/${store.slug}`}
                  className="rounded-full bg-[#005141] px-6 py-3 text-sm font-semibold text-white hover:bg-[#003d31] transition-colors"
                >
                  Volver a {store.name}
                </Link>
              )}
              <Link
                href="/carrito"
                className="rounded-full border border-[#e7e0d6] bg-white px-6 py-3 text-sm font-semibold text-[#1c1917] hover:bg-[#f7f3ec] transition-colors"
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
    <div className="min-h-screen bg-[#faf7f2] text-[#1c1917] pt-28 sm:pt-32 pb-24 lg:pb-16">
      <Container size="full" className="space-y-8">
        {/* Header */}
        <header className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 border-b border-[#e7e0d6] pb-6">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-3 text-xs font-medium text-[#005141]">
              <Link
                href={`/tienda/${store.slug}`}
                className="inline-flex items-center gap-1.5 hover:underline"
              >
                <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
                <span>Volver a {store.name}</span>
              </Link>
              <span className="text-[#e7e0d6]" aria-hidden="true">/</span>
              <Link href="/carrito" className="text-[#57534e] hover:text-[#1c1917] hover:underline">
                Mis carritos
              </Link>
            </div>
            <h1 className="font-display text-3xl sm:text-4xl font-semibold tracking-tight text-[#1c1917]">
              Checkout · {store.name}
            </h1>
          </div>

          {populatedStores.length > 1 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs text-[#57534e]">Tus carritos activos:</span>
              {populatedStores.map((entry) => (
                <button
                  key={entry.id}
                  type="button"
                  onClick={() => router.push(`/checkout?store=${encodeURIComponent(entry.slug)}`)}
                  className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition-colors cursor-pointer ${
                    entry.id === store.id
                      ? 'bg-[#005141] text-white'
                      : 'border border-[#e7e0d6] bg-white text-[#57534e] hover:text-[#1c1917] hover:bg-[#f7f3ec]'
                  }`}
                >
                  {entry.name}
                </button>
              ))}
            </div>
          )}
        </header>

        {/* Balanced 7 / 5 Layout: Left = Unified Editorial Form Sheet, Right = Sticky Summary + Mercado Pago CTA */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (7 cols): Unified Editorial Sheet */}
          <div className="lg:col-span-7 bg-white rounded-[12px] border border-[#e7e0d6] divide-y divide-[#e7e0d6]">
            <CapacitySlotPicker
              embedded
              hasActiveReservation={Boolean(checkoutResponse)}
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
                embedded
                store={store}
                value={deliveryData}
                onChange={handleDeliveryChange}
                defaultRecipientName={customerName}
                defaultRecipientPhone={customerPhone}
              />
            ) : (
              <section className="p-5 sm:p-7 space-y-4">
                <h3 className="font-display text-base sm:text-lg font-semibold text-[#1c1917]">
                  Datos para el retiro en tienda
                </h3>
                {store.address && (
                  <p className="text-xs text-[#57534e]">
                    Punto de retiro: <strong className="text-[#1c1917]">{store.address}</strong>
                  </p>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div>
                    <label htmlFor="pickup-customer-name" className="block text-xs font-medium text-[#57534e] mb-1.5">
                      Nombre completo *
                    </label>
                    <input
                      id="pickup-customer-name"
                      type="text"
                      value={customerName}
                      onChange={(e) => setCustomerName(e.target.value)}
                      placeholder="Tu nombre completo"
                      className="w-full px-3.5 py-2.5 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none transition-colors"
                    />
                  </div>
                  <div>
                    <label htmlFor="pickup-customer-phone" className="block text-xs font-medium text-[#57534e] mb-1.5">
                      Teléfono celular *
                    </label>
                    <PeruPhoneInput
                      id="pickup-customer-phone"
                      value={customerPhone}
                      onChange={setCustomerPhone}
                      error={
                        customerPhone.length > 0
                          ? (validateLocalPeruPhone(customerPhone, true) ?? undefined)
                          : undefined
                      }
                      required
                    />
                  </div>
                </div>
              </section>
            )}

            {/* Operational Fields: Email, DNI (if required), Order Notes */}
            <section className="p-5 sm:p-7 space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-display text-base sm:text-lg font-semibold text-[#1c1917]">
                  Datos de comprobante y notas
                </h3>
                {hasAutofilledFromAccount && (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-[#f0fdfa] border border-[#ccfbf1] px-2.5 py-0.5 text-[11px] font-semibold text-[#005141]">
                    <Sparkles className="h-3 w-3" aria-hidden="true" />
                    <span>Completado desde tu cuenta</span>
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="checkout-email" className="block text-xs font-medium text-[#57534e] mb-1.5">
                    Correo electrónico (opcional)
                  </label>
                  <input
                    id="checkout-email"
                    type="email"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                    placeholder="cliente@correo.com"
                    className="w-full px-3.5 py-2.5 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none transition-colors"
                  />
                </div>

                {requiresDni && (
                  <div>
                    <label htmlFor="checkout-dni" className="text-xs font-medium text-[#57534e] mb-1.5 flex items-center gap-1.5">
                      <IdCard className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
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
                      className="w-full px-3.5 py-2.5 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none font-mono tabular-nums transition-colors"
                    />
                    {customerDocumentNumber && !isDniValid && (
                      <p className="text-[11px] text-rose-600 mt-1">
                        Ingresa entre 8 y 11 dígitos numéricos.
                      </p>
                    )}
                  </div>
                )}
              </div>

              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-medium text-[#57534e]">Comprobante de pago:</span>
                  <div className="inline-flex rounded-lg border border-[#e7e0d6] bg-[#faf7f2] p-0.5">
                    <button
                      type="button"
                      onClick={() => setRequestedReceiptType('BOLETA')}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors cursor-pointer ${
                        requestedReceiptType === 'BOLETA'
                          ? 'bg-[#005141] text-white'
                          : 'text-[#57534e] hover:text-[#1c1917]'
                      }`}
                    >
                      Boleta de Venta (03)
                    </button>
                    {store?.canIssueInvoices && (
                      <button
                        type="button"
                        onClick={() => setRequestedReceiptType('FACTURA')}
                        className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors cursor-pointer ${
                          requestedReceiptType === 'FACTURA'
                            ? 'bg-[#005141] text-white'
                            : 'text-[#57534e] hover:text-[#1c1917]'
                        }`}
                      >
                        Factura Electrónica (01)
                      </button>
                    )}
                  </div>
                </div>

                {store?.canIssueInvoices && requestedReceiptType === 'FACTURA' && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-3.5 rounded-lg bg-[#faf7f2] border border-[#e7e0d6]">
                    <div>
                      <label htmlFor="checkout-factura-ruc" className="block text-xs font-medium text-[#57534e] mb-1.5">
                        RUC del receptor (11 dígitos) *
                      </label>
                      <input
                        id="checkout-factura-ruc"
                        type="text"
                        inputMode="numeric"
                        maxLength={11}
                        value={customerDocumentNumber}
                        onChange={(e) => setCustomerDocumentNumber(e.target.value.replace(/\D/g, ''))}
                        placeholder="20100070970"
                        className="w-full px-3.5 py-2.5 text-xs bg-white border border-[#e7e0d6] focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] font-mono tabular-nums outline-none"
                      />
                    </div>
                    <div>
                      <label htmlFor="checkout-factura-razon" className="block text-xs font-medium text-[#57534e] mb-1.5">
                        Razón Social *
                      </label>
                      <input
                        id="checkout-factura-razon"
                        type="text"
                        maxLength={200}
                        value={customerLegalName}
                        onChange={(e) => setCustomerLegalName(e.target.value)}
                        placeholder="EMPRESA S.A.C."
                        className="w-full px-3.5 py-2.5 text-xs bg-white border border-[#e7e0d6] focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] outline-none"
                      />
                    </div>
                  </div>
                )}
              </div>

              {storeSettings?.allowOrderNotes !== false && (
                <div>
                  <label htmlFor="checkout-order-notes" className="text-xs font-medium text-[#57534e] mb-1.5 flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-[#005141]" aria-hidden="true" />
                    <span>Notas para el comercio (opcional)</span>
                  </label>
                  <textarea
                    id="checkout-order-notes"
                    rows={2}
                    maxLength={500}
                    value={orderNotes}
                    onChange={(e) => setOrderNotes(e.target.value)}
                    placeholder="Instrucciones especiales de preparación o entrega..."
                    className="w-full px-3.5 py-2.5 text-xs bg-[#faf7f2] border border-[#e7e0d6] focus:bg-white focus:border-[#005141] focus:ring-2 focus:ring-[#005141]/15 rounded-[8px] text-[#1c1917] placeholder-[#78716c] outline-none resize-none transition-colors"
                  />
                </div>
              )}
            </section>
          </div>

          {/* Right Column (5 cols): Sticky Order Summary + Mercado Pago Payment Rail */}
          <div className="lg:col-span-5 lg:sticky lg:top-24 bg-white rounded-[12px] border border-[#e7e0d6] overflow-hidden divide-y divide-[#e7e0d6]">
            <CheckoutOrderSummary
              embedded
              storeName={store.name}
              items={cart}
              currency={currency}
              subtotal={subtotal}
              deliveryFee={store.deliveryFeeAmount ?? 0}
              serviceFeeRate={serviceFeeRate}
              serviceFeeCap={serviceFeeCap}
              taxRate={store.taxRate ?? 0.18}
              fulfillmentType={fulfillment}
              storeSettings={storeSettings}
              checkoutResponse={checkoutResponse}
              discountCode={discountCode}
              onDiscountCodeChange={(code) => {
                setDiscountCode(code);
                if (promoError) setPromoError(null);
              }}
              appliedPromo={appliedPromo}
              promoError={promoError}
              isValidatingPromo={isValidatingPromo}
              onApplyDiscount={async () => {
                if (!store || !discountCode.trim()) return;
                setIsValidatingPromo(true);
                setPromoError(null);
                try {
                  const res = await checkoutService.validatePromoCode({
                    storeId: store.id,
                    code: discountCode.trim(),
                    subtotal,
                  });
                  if (res.valid && res.code) {
                    setAppliedPromo({ code: res.code, discountAmount: Number(res.discountAmount || 0) });
                    validatedSubtotalRef.current = subtotal;
                    setDiscountCode(res.code);
                    setCheckoutResponse(null);
                    clearStoredReservation();
                  } else {
                    setAppliedPromo(null);
                    validatedSubtotalRef.current = null;
                    setPromoError(res.message || 'El código promocional no es válido.');
                  }
                } finally {
                  setIsValidatingPromo(false);
                }
              }}
              onClearDiscount={() => {
                setAppliedPromo(null);
                validatedSubtotalRef.current = null;
                setDiscountCode('');
                setPromoError(null);
                setCheckoutResponse(null);
                clearStoredReservation();
              }}
              onReservationExpired={() => {
                setCheckoutResponse(null);
                if (store && selectedSlot) {
                  try {
                    sessionStorage.removeItem(`jaldishop-checkout-reservation:${store.id}:${selectedSlot.id}`);
                  } catch { /* Storage is optional */ }
                }
                setPaymentError('Tu reserva de horario expiró. Vuelve a confirmar para generar una nueva.');
              }}
            />

            <CheckoutPaymentSection
              embedded
              storeSettings={storeSettings}
              totalAmount={checkoutResponse?.pricing?.totalAmount ?? totalAmount}
              currency={checkoutResponse?.pricing?.currency ?? currency}
              canProceed={canProceed}
              disabledHint={disabledHint}
              isSubmittingCheckout={isSubmittingCheckout}
              isInitiatingPayment={isInitiatingPayment}
              isRedirectingToMercadoPago={isRedirectingToMercadoPago}
              paymentError={paymentError}
              hasActiveReservation={Boolean(checkoutResponse)}
              activePaymentSession={activePaymentSession}
              onCheckPaymentStatus={() => {
                if (activePaymentSession) {
                  void checkActivePaymentStatus(activePaymentSession.paymentId);
                }
              }}
              onPayWithMercadoPago={handlePayWithMercadoPago}
              onProceedCashOnDelivery={
                storeSettings?.cashOnDeliveryEnabled ? handleProceedCashOnDelivery : undefined
              }
            />
          </div>
        </div>
      </Container>

      {/* Mobile Quick-Pay Bar */}
      <div className="lg:hidden fixed bottom-0 inset-x-0 z-30 border-t border-[#e7e0d6] bg-white/95 backdrop-blur-sm px-4 py-3 flex items-center justify-between gap-3">
        <div>
          <span className="text-[11px] text-[#57534e] block">Total a pagar</span>
          <span className="font-display text-base font-bold text-[#005141] tabular-nums">
            {formatCurrency(
              checkoutResponse?.pricing?.totalAmount ?? totalAmount,
              checkoutResponse?.pricing?.currency ?? currency
            )}
          </span>
        </div>
        <button
          type="button"
          onClick={() =>
            document
              .getElementById('checkout-payment-section')
              ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }
          className="inline-flex items-center gap-1.5 rounded-full bg-[#005141] hover:bg-[#003d31] text-white text-xs font-semibold px-4 py-2.5 cursor-pointer"
        >
          <span>Ir a pagar</span>
          <ArrowDown className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
      </div>

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
