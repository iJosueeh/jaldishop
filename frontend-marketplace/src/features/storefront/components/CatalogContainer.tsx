'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Clock } from 'lucide-react';
import { CapacitySlotPicker, CapacitySlot } from './CapacitySlotPicker';
import { StorefrontNav } from './StorefrontNav';
import {
  ProductGrid,
  CatalogFilterBar,
  CatalogEmptyState,
} from './catalog';
import { ProductItem, ProductVariantItem, CartItem, PublicStore } from '../types/storefront.types';
import { StorefrontBadges } from './StorefrontBadges';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';
import { DeliveryAddressSelector } from '@/features/checkout/components/DeliveryAddressSelector';
import { DeliveryFormData, CheckoutResponse, InitiateCheckoutPayload } from '@/features/checkout/types/checkout.types';
import { checkoutService } from '@/features/checkout/services/checkoutService';
import { mapDeliveryFormDataToCheckoutFields, parseSlotTimeRange } from '@/features/checkout/utils/addressMapper';
import { customerAddressService } from '@/features/customer/services/customerAddressService';
import { PaymentHubModal } from './PaymentHubModal';
import { WhatsAppBridgeModal } from './WhatsAppBridgeModal';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';

export type { ProductItem, ProductVariantItem, CartItem };

interface CatalogContainerProps {
  storeSlug?: string;
  storeName: string;
  store?: PublicStore;
  products?: ProductItem[] | null;
  slots?: CapacitySlot[] | null;
  catalogError?: boolean;
  capacityError?: boolean;
  capacitySlot?: React.ReactNode;
}

export function CatalogContainer({
  storeName,
  store,
  products,
  slots,
  catalogError = false,
  capacityError = false,
  capacitySlot,
}: CatalogContainerProps) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const {
    items: cart,
    count,
    store: cartStore,
    changeQuantity,
    selectStore,
    pending,
    loading,
    subtotal,
    currency,
    authenticated,
  } = useStorefrontCart();

  const totalCartCount = cartStore?.id === store?.id ? count : 0;
  useEffect(() => {
    if (store) selectStore({ id: store.id, name: store.name, slug: store.slug });
  }, [store, selectStore]);

  const [selectedSlot, setSelectedSlot] = useState<CapacitySlot | null>(null);
  const [fulfillment, setFulfillment] = useState<'DELIVERY' | 'PICKUP'>('DELIVERY');
  const [deliveryData, setDeliveryData] = useState<DeliveryFormData | null>(null);
  const [isDeliveryValid, setIsDeliveryValid] = useState(false);

  // Modal & checkout state
  const [isSubmittingCheckout, setIsSubmittingCheckout] = useState(false);
  const [checkoutResponse, setCheckoutResponse] = useState<CheckoutResponse | null>(null);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [isWhatsAppModalOpen, setIsWhatsAppModalOpen] = useState(false);

  // Callback estable: si se pasa como inline function, DeliveryAddressSelector
  // (cuyo effect tiene onChange en sus dependencias) entra en un bucle infinito
  // de re-renders al re-ejecutarse en cada render del padre.
  const handleDeliveryChange = useCallback((data: DeliveryFormData | null, valid: boolean) => {
    setDeliveryData(data);
    setIsDeliveryValid(valid);
  }, []);

  const items = products ?? [];
  const contactNumber = (store?.whatsappNumber || store?.phone)?.replace(/\D/g, '');
  const contactUrl = contactNumber
    ? `https://wa.me/${contactNumber}?text=${encodeURIComponent(
        `Hola ${storeName}, quisiera consultar sus productos.`
      )}`
    : null;

  const categories = [
    ...new Map(
      items
        .filter((product) => product.category)
        .map((product) => [
          product.categoryId || product.category,
          {
            id: product.categoryId || product.category,
            name: product.category,
            slug: product.categoryId || product.category,
            itemCount: items.filter(
              (item) =>
                (item.categoryId || item.category) ===
                (product.categoryId || product.category)
            ).length,
          },
        ])
    ).values(),
  ];

  const isFiltering = query.trim().length > 0 || category !== 'all';

  const filtered = items.filter(
    (product) =>
      (category === 'all' || (product.categoryId || product.category) === category) &&
      (product.name + ' ' + (product.description || ''))
        .toLocaleLowerCase()
        .includes(query.toLocaleLowerCase())
  );

  const capacityTitle = capacityError
    ? 'No pudimos consultar los horarios'
    : slots == null
    ? 'Horarios de pedido no disponibles'
    : 'No hay horarios disponibles';

  const capacityMessage = capacityError
    ? 'Recarga la página para volver a consultar la disponibilidad. No se ha reservado ningún cupo.'
    : slots == null
    ? `Aún no podemos consultar los cupos de ${storeName}. No es posible reservar un horario desde esta página.`
    : 'La tienda no tiene horarios disponibles para seleccionar. Vuelve a consultar más adelante.';

  const deliveryFee = fulfillment === 'DELIVERY' ? (store?.deliveryFeeAmount ?? 0) : 0;
  const totalAmount = subtotal + deliveryFee;

  const handleInitiateCheckout = async () => {
    if (!store) return;

    if (!authenticated) {
      const targetUrl = window.location.pathname + '#entrega';
      window.location.href = `/login?redirect=${encodeURIComponent(targetUrl)}`;
      return;
    }

    if (!selectedSlot) {
      toast.error('Por favor selecciona un horario de entrega o retiro.');
      return;
    }

    if (fulfillment === 'DELIVERY') {
      if (!store.deliveryEnabled) {
        toast.error('La tienda no realiza envíos a domicilio en este momento.');
        return;
      }
      if (!isDeliveryValid || !deliveryData) {
        toast.error('Por favor completa todos los campos obligatorios de la dirección de entrega.');
        return;
      }

      // 3. Guardado opcional de dirección desacoplado
      if (deliveryData.saveToAddresses && deliveryData.mode === 'manual') {
        try {
          await customerAddressService.createAddress({
            alias: deliveryData.alias || deliveryData.district || 'Entrega',
            recipientName: deliveryData.recipientName,
            recipientPhone: deliveryData.recipientPhone,
            department: deliveryData.department,
            province: deliveryData.province,
            district: deliveryData.district,
            ubigeoCode: deliveryData.ubigeoCode,
            addressLine: deliveryData.addressLine,
            reference: deliveryData.reference,
          });
          toast.success('Dirección guardada en Mis Direcciones.');
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : 'No se pudo guardar la dirección en tu libreta.';
          toast.info(`${msg} Continuaremos con tu pedido.`);
        }
      }
    }

    try {
      setIsSubmittingCheckout(true);

      const parsedTimes = parseSlotTimeRange(selectedSlot.timeRange);
      const serviceDate = new Date().toISOString().split('T')[0];

      let deliveryFields: {
        deliveryAddress?: string | null;
        deliveryReference?: string | null;
        customerName?: string | null;
        customerPhone?: string | null;
        deliveryLatitude?: number | null;
        deliveryLongitude?: number | null;
      } = {};

      if (fulfillment === 'DELIVERY' && deliveryData) {
        deliveryFields = mapDeliveryFormDataToCheckoutFields(deliveryData);
      } else {
        deliveryFields = {
          deliveryAddress: null,
          deliveryReference: null,
          customerName: null,
          customerPhone: null,
          deliveryLatitude: null,
          deliveryLongitude: null,
        };
      }

      const payload: InitiateCheckoutPayload = {
        storeId: store.id,
        fulfillmentType: fulfillment,
        serviceDate,
        startTime: parsedTimes.startTime || null,
        endTime: parsedTimes.endTime || null,
        customerName: deliveryFields.customerName || null,
        customerPhone: deliveryFields.customerPhone || null,
        customerEmail: null,
        deliveryAddress: deliveryFields.deliveryAddress || null,
        deliveryReference: deliveryFields.deliveryReference || null,
        deliveryLatitude: deliveryFields.deliveryLatitude ?? null,
        deliveryLongitude: deliveryFields.deliveryLongitude ?? null,
      };

      const result = await checkoutService.initiateCheckout(payload);
      setCheckoutResponse(result);
      setIsPaymentModalOpen(true);
      toast.success('¡Horario reservado por 10 minutos! Realiza tu pago para completar la compra.');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Ocurrió un error al iniciar la compra.';
      toast.error(msg);
    } finally {
      setIsSubmittingCheckout(false);
    }
  };

  return (
    <div className="space-y-8 font-sans">
      <section id="catalogo" className="scroll-mt-40">
        {/* Barra Superior con Título, Contador, Búsqueda y Botón de Carrito */}
        <CatalogFilterBar
          storeName={storeName}
          query={query}
          onQueryChange={setQuery}
          totalProductsCount={items.length}
        />

        {catalogError ? (
          <CatalogEmptyState
            type="error"
            storeName={storeName}
            onReset={() => window.location.reload()}
          />
        ) : products == null ? (
          <CatalogEmptyState type="unavailable" storeName={storeName} />
        ) : items.length === 0 ? (
          <CatalogEmptyState type="no-products" storeName={storeName} />
        ) : (
          <>
            {/* Navegación por Categorías de la Tienda */}
            {categories.length > 0 && (
              <StorefrontNav
                categories={[
                  {
                    id: 'all',
                    name: 'Todos los productos',
                    slug: 'todos',
                    itemCount: items.length,
                  },
                  ...categories,
                ]}
                activeCategoryId={category}
                onSelectCategory={setCategory}
              />
            )}
            {items.some((product) => product.categoryId && !product.category) && (
              <p role="status" className="mt-3 text-sm text-stone-600">
                Algunas categorías no están disponibles. Puedes explorar todos los productos.
              </p>
            )}

            {/* Cuadrícula de Productos a Ancho Completo */}
            <div className="mt-6">
              <ProductGrid
                products={filtered}
                onAdd={(p) => changeQuantity(p, 1)}
                cart={cart}
                storeName={storeName}
                onResetFilters={() => {
                  setQuery('');
                  setCategory('all');
                }}
                isFiltering={isFiltering}
                pending={pending || loading || cartStore?.id !== store?.id}
              />
            </div>
          </>
        )}
      </section>

      {(catalogError || items.length === 0) && contactUrl && (
        <div className="-mt-4 text-center">
          <a
            href={contactUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex rounded-full bg-[#005141] px-6 py-3 text-sm font-bold text-white shadow-xs transition hover:bg-[#00382d] active:scale-95"
          >
            Consultar productos por WhatsApp
          </a>
        </div>
      )}

      {/* Selector de Horarios y Capacidad Operativa */}
      {totalCartCount > 0 && (
        <div id="entrega" className="space-y-4 scroll-mt-28">
          <h2 className="font-display text-2xl font-bold text-stone-900">Entrega y horarios</h2>
          {store && <StorefrontBadges store={store} />}
          {store?.pickupEnabled && store.address && (
            <p className="text-sm text-stone-600">
              Recojo en {store.address}.{' '}
              <a href="#ubicacion" className="font-semibold text-[#005141] hover:underline">
                Ver cómo llegar
              </a>
            </p>
          )}

          {fulfillment === 'DELIVERY' && store && (
            <DeliveryAddressSelector
              store={store}
              value={deliveryData}
              onChange={handleDeliveryChange}
            />
          )}

          {capacitySlot ??
            (slots && slots.length > 0 && !capacityError ? (
              <CapacitySlotPicker
                slots={slots}
                selectedSlotId={selectedSlot?.id}
                onSelectSlot={setSelectedSlot}
                fulfillmentType={fulfillment}
                onFulfillmentTypeChange={setFulfillment}
              />
            ) : (
              <section className="rounded-[12px] border border-stone-200/90 bg-white p-6 sm:p-8 shadow-2xs">
                <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-stone-900">
                  <Clock aria-hidden="true" className="h-5 w-5 text-[#005141]" />
                  {capacityTitle}
                </h2>
                <p className="mt-2.5 max-w-2xl text-sm leading-relaxed text-stone-600">
                  {capacityMessage}
                </p>
              </section>
            ))}

          {/* Panel de Resumen y Botón de Inicio de Checkout */}
          {selectedSlot && (
            <div className="bg-white rounded-[12px] p-5 sm:p-6 border border-stone-200/90 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-stone-100">
                <div className="space-y-1">
                  <span className="text-xs font-bold text-stone-500 uppercase tracking-wider block">
                    Resumen de tu pedido
                  </span>
                  <p className="text-sm font-bold text-stone-900">
                    {fulfillment === 'DELIVERY' ? 'Envío a Domicilio' : 'Retiro en Tienda'} • Franja:{' '}
                    <span className="font-mono text-[#005141]">{selectedSlot.timeRange}</span>
                  </p>
                  {fulfillment === 'DELIVERY' && deliveryData && isDeliveryValid && (
                    <p className="text-xs text-stone-600 truncate max-w-md">
                      Destino: {deliveryData.addressLine}, {deliveryData.district} ({deliveryData.recipientName})
                    </p>
                  )}
                  {fulfillment === 'DELIVERY' && (!isDeliveryValid || !deliveryData) && (
                    <p className="text-xs text-rose-600 font-medium">
                      Completa los datos obligatorios de tu dirección de entrega para continuar.
                    </p>
                  )}
                </div>

                {/* Desglose Económico */}
                <div className="text-right sm:border-l sm:border-stone-100 sm:pl-6 space-y-0.5">
                  <div className="text-xs text-stone-500 flex items-center justify-between sm:justify-end gap-3">
                    <span>Subtotal productos:</span>
                    <span className="font-mono font-medium text-stone-800">
                      {formatCurrency(subtotal, currency)}
                    </span>
                  </div>
                  <div className="text-xs text-stone-500 flex items-center justify-between sm:justify-end gap-3">
                    <span>Costo de envío:</span>
                    <span className="font-mono font-medium text-stone-800">
                      {fulfillment === 'DELIVERY'
                        ? formatCurrency(store?.deliveryFeeAmount ?? 0, currency)
                        : 'Gratis'}
                    </span>
                  </div>
                  <div className="text-sm font-bold text-stone-900 flex items-center justify-between sm:justify-end gap-3 pt-1 border-t border-stone-100">
                    <span>Total estimado:</span>
                    <span className="font-display font-black text-base text-[#005141]">
                      {formatCurrency(totalAmount, currency)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Botón Principal de Checkout */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-1">
                <p className="text-[11px] text-stone-500">
                  Al continuar se reservará tu franja horaria durante 10 minutos para coordinar y pagar.
                </p>
                <Button
                  type="button"
                  variant="primary"
                  size="lg"
                  onClick={handleInitiateCheckout}
                  isLoading={isSubmittingCheckout}
                  disabled={
                    isSubmittingCheckout ||
                    (fulfillment === 'DELIVERY' &&
                      (!isDeliveryValid || !deliveryData || !store?.deliveryEnabled))
                  }
                  className="w-full sm:w-auto rounded-full font-bold px-8 shadow-md shadow-[#005141]/20 text-sm"
                >
                  Continuar al pago ({formatCurrency(totalAmount, currency)})
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal de Pago de Reserva (10 min) */}
      <PaymentHubModal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        total={checkoutResponse?.pricing?.totalAmount ?? totalAmount}
        storeName={store?.name || storeName}
        slotTime={selectedSlot?.timeRange}
        countdownSeconds={Math.max(
          60,
          checkoutResponse?.reservationExpiresAt
            ? Math.floor((new Date(checkoutResponse.reservationExpiresAt).getTime() - Date.now()) / 1000)
            : 600
        )}
        onProceedToWhatsApp={() => {
          setIsPaymentModalOpen(false);
          setIsWhatsAppModalOpen(true);
        }}
      />

      {/* Modal de Puente a WhatsApp */}
      <WhatsAppBridgeModal
        isOpen={isWhatsAppModalOpen}
        onClose={() => setIsWhatsAppModalOpen(false)}
        cart={cart}
        selectedSlot={selectedSlot}
        fulfillmentType={fulfillment}
        storeName={store?.name || storeName}
        storePhone={store?.whatsappNumber || store?.phone || undefined}
        deliveryFee={deliveryFee}
      />
    </div>
  );
}
