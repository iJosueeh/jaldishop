'use client';

import React, { useState, useEffect } from 'react';
import { ShoppingBag, Plus, Minus, Sparkles, X, MessageSquare, Trash2, Search, ArrowRight, Clock, Bike, Store } from 'lucide-react';
import { Card } from '@/shared/components/ui/Card';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';
import { motion, AnimatePresence } from 'framer-motion';
import { CapacitySlotPicker, DEFAULT_SLOTS, CapacitySlot } from './CapacitySlotPicker';
import { CartDrawer } from './CartDrawer';
import { PaymentHubModal, PaymentMethod } from './PaymentHubModal';
import { WhatsAppBridgeModal } from './WhatsAppBridgeModal';
import { OrderTrackerModal } from './OrderTrackerModal';

export interface ProductItem {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  badge?: string;
  badgeVariant?: 'jade' | 'terracotta' | 'amber';
  imageBg: string;
  iconText: string;
  prepTimeMinutes?: number;
}

export interface CartItem {
  product: ProductItem;
  quantity: number;
  notes?: string;
}

const SAMPLE_PRODUCTS: ProductItem[] = [
  {
    id: 'prod-01',
    name: 'Croissant Artesanal de Mantequilla',
    description: 'Hojaldrado tradicional con 100% mantequilla francesa, corteza dorada crujiente y miga aireada.',
    price: 8.5,
    category: 'panaderia',
    badge: 'Más Vendido',
    badgeVariant: 'amber',
    imageBg: 'bg-[#fef3c7] text-[#92400e] border border-[#fde68a]',
    iconText: '🥐',
    prepTimeMinutes: 15,
  },
  {
    id: 'prod-02',
    name: 'Pan de Masa Madre Rústico (800g)',
    description: 'Fermentación lenta de 24 horas, harina de trigo y centeno integral con masa madre viva.',
    price: 14.0,
    category: 'panaderia',
    badge: 'Masa Madre',
    badgeVariant: 'jade',
    imageBg: 'bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]',
    iconText: '🥖',
    prepTimeMinutes: 20,
  },
  {
    id: 'prod-03',
    name: 'Empanada Criolla de Lomo Saltado',
    description: 'Relleno jugoso de lomo de res saltado al wok con cebolla morada, tomate y ají amarillo.',
    price: 9.0,
    category: 'destacados',
    badge: 'Horneada Hoy',
    badgeVariant: 'terracotta',
    imageBg: 'bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa]',
    iconText: '🥟',
    prepTimeMinutes: 15,
  },
  {
    id: 'prod-04',
    name: 'Cheesecake Horneado de Frutos Rojos',
    description: 'Base crocante de galleta de mantequilla, queso crema suave y coulis artesanal de frambuesas.',
    price: 16.5,
    category: 'pasteleria',
    badge: 'Favorito',
    badgeVariant: 'terracotta',
    imageBg: 'bg-pink-50 text-pink-700 border border-pink-200',
    iconText: '🍰',
    prepTimeMinutes: 25,
  },
  {
    id: 'prod-05',
    name: 'Caja de 6 Alfajores con Manjar de Olla',
    description: 'Masa delicada que se deshace en el paladar, abundante manjar blanco casero y coco fino.',
    price: 18.0,
    category: 'pasteleria',
    badge: 'Para Compartir',
    badgeVariant: 'amber',
    imageBg: 'bg-[#fffbeb] text-[#92400e] border border-[#fef3c7]',
    iconText: '🍪',
    prepTimeMinutes: 10,
  },
  {
    id: 'prod-06',
    name: 'Café Espresso Doble Blend Especial',
    description: 'Granos arábica seleccionados de Villa Rica con notas achocolatadas y acidez equilibrada.',
    price: 7.5,
    category: 'bebidas',
    badge: '100% Arábica',
    badgeVariant: 'jade',
    imageBg: 'bg-[#faf7f2] text-[#57534e] border border-[#e7e0d6]',
    iconText: '☕',
    prepTimeMinutes: 5,
  },
];

interface CatalogContainerProps {
  storeSlug?: string;
  storeName?: string;
  capacitySlot?: React.ReactNode;
}

export function CatalogContainer({
  storeName = 'Panadería Don Pepe',
  capacitySlot,
}: CatalogContainerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedProduct, setSelectedProduct] = useState<ProductItem | null>(null);
  const [modalQuantity, setModalQuantity] = useState(1);
  const [modalNotes, setModalNotes] = useState('');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [isCartDrawerOpen, setIsCartDrawerOpen] = useState(false);
  const [fulfillmentType, setFulfillmentType] = useState<'DELIVERY' | 'PICKUP'>('DELIVERY');
  const [selectedSlot, setSelectedSlot] = useState<CapacitySlot>(DEFAULT_SLOTS[1]); // 10:00 - 11:00 AM
  const [holdCountdown, setHoldCountdown] = useState(600);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [isWhatsAppModalOpen, setIsWhatsAppModalOpen] = useState(false);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState<PaymentMethod>('yape');
  const [hasUploadedReceipt, setHasUploadedReceipt] = useState(false);
  const [isTrackerModalOpen, setIsTrackerModalOpen] = useState(false);
  const [currentOrderId, setCurrentOrderId] = useState('JALDI-1048');

  // Ticks when cart has items, whether drawer is open or closed, so it never restarts on open/close
  useEffect(() => {
    if (cart.length === 0) {
      setHoldCountdown(600);
      return;
    }
    const tick = () => {
      if (document.visibilityState === 'hidden') return;
      setHoldCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    };
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [cart.length]);

  // Filter products by search query
  const filteredProducts = SAMPLE_PRODUCTS.filter((prod) =>
    prod.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    prod.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleOpenProductModal = (product: ProductItem) => {
    setSelectedProduct(product);
    setModalQuantity(1);
    setModalNotes('');
  };

  const handleQuickAdd = (product: ProductItem, e: React.MouseEvent) => {
    e.stopPropagation();
    addToCart(product, 1, '');
  };

  const addToCart = (product: ProductItem, quantity: number, notes: string) => {
    setCart((prev) => {
      const existingIdx = prev.findIndex((item) => item.product.id === product.id);
      if (existingIdx > -1) {
        const updated = [...prev];
        updated[existingIdx].quantity += quantity;
        if (notes) updated[existingIdx].notes = notes;
        return updated;
      }
      return [...prev, { product, quantity, notes }];
    });

    toast.success(`¡${product.name} añadido!`, {
      description: `Cantidad: ${quantity} • Cupo de franja ${selectedSlot.timeRange} asegurado.`,
      action: {
        label: 'Ver Carrito',
        onClick: () => setIsCartDrawerOpen(true),
      },
    });

    setSelectedProduct(null);
  };

  const updateCartQuantity = (productId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.product.id === productId) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : null;
          }
          return item;
        })
        .filter(Boolean) as CartItem[]
    );
  };

  const removeCartItem = (productId: string) => {
    setCart((prev) => prev.filter((item) => item.product.id !== productId));
  };

  const subtotal = cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
  const deliveryFee = fulfillmentType === 'PICKUP' ? 0 : (subtotal > 0 ? 5.0 : 0);
  const total = subtotal + deliveryFee;
  const totalItemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <div className="py-4 space-y-8">
      {/* 1. Visual Capacity Slot Picker (Timeline) */}
      {capacitySlot || (
        <CapacitySlotPicker
          slots={DEFAULT_SLOTS}
          selectedSlotId={selectedSlot.id}
          onSelectSlot={(slot) => {
            setSelectedSlot(slot);
            toast.info(`Franja seleccionada: ${slot.timeRange}`, {
              description: 'Tu cupo de preparación queda apartado para este horario.',
            });
          }}
          fulfillmentType={fulfillmentType}
          onFulfillmentTypeChange={setFulfillmentType}
        />
      )}

      {/* 2. Main Catalog Grid & Sticky Cart Sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Main Catalog Area */}
        <div className="lg:col-span-8 space-y-6">
          {/* Header with Search Input */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-stone-200">
            <div>
              <h2 className="text-xl sm:text-2xl font-black text-[#1c1917] tracking-tight">
                Catálogo de Productos Frescos
              </h2>
              <p className="text-xs text-[#57534e] mt-0.5">
                Elige tus productos para preparar bajo pedido y apartar tu cupo en la franja elegida
              </p>
            </div>

            {/* Quick Search */}
            <div className="relative w-full sm:w-64">
              <Search className="w-4 h-4 text-[#a8a29e] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="Buscar en el menú..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-stone-200 rounded-xl text-xs text-[#1c1917] placeholder:text-[#a8a29e] focus:outline-none focus:ring-2 focus:ring-[#005141] shadow-xs"
              />
            </div>
          </div>

          {/* Products Grid */}
          {filteredProducts.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-3xl border border-stone-200 p-8 space-y-3">
              <div className="text-3xl">🔍</div>
              <h3 className="text-base font-bold text-[#1c1917]">No se encontraron productos</h3>
              <p className="text-xs text-[#57534e]">Intenta buscar con otra palabra clave.</p>
              <Button variant="outline" size="sm" onClick={() => setSearchQuery('')}>
                Limpiar búsqueda
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              {filteredProducts.map((prod) => (
                <motion.div
                  key={prod.id}
                  whileHover={{ y: -3 }}
                  transition={{ type: 'spring', stiffness: 300, damping: 20 }}
                  onClick={() => handleOpenProductModal(prod)}
                  className="cursor-pointer"
                >
                  <Card className="p-5 sm:p-6 h-full flex flex-col justify-between hover:shadow-xl transition-all duration-300 bg-white border border-stone-200/90 hover:border-[#005141] group rounded-3xl">
                    <div className="space-y-3.5">
                      {/* Visual Emoji Badge & Specialty Tag */}
                      <div className="flex items-start justify-between">
                        <div
                          className={`w-14 h-14 rounded-2xl ${prod.imageBg} flex items-center justify-center text-3xl shadow-sm group-hover:scale-105 transition-transform`}
                        >
                          {prod.iconText}
                        </div>
                        {prod.badge && (
                          <Badge variant={prod.badgeVariant || 'jade'} size="sm" className="shadow-xs font-bold">
                            {prod.badge}
                          </Badge>
                        )}
                      </div>

                      <div>
                        <h3 className="text-base font-bold text-[#1c1917] group-hover:text-[#005141] transition-colors leading-snug">
                          {prod.name}
                        </h3>
                        <p className="text-xs text-[#57534e] mt-1.5 line-clamp-2 leading-relaxed">
                          {prod.description}
                        </p>
                      </div>
                    </div>

                    <div className="mt-5 pt-3.5 border-t border-stone-100 flex items-center justify-between">
                      <div>
                        <span className="text-lg font-black text-[#1c1917] font-mono">
                          {formatCurrency(prod.price)}
                        </span>
                        {prod.prepTimeMinutes && (
                          <span className="block text-[10px] font-medium text-[#78716c]">
                            ~{prod.prepTimeMinutes} min preparación
                          </span>
                        )}
                      </div>
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={(e) => handleQuickAdd(prod, e)}
                        className="bg-[#005141] hover:bg-[#00382d] text-white font-bold rounded-xl shadow-sm border-0 cursor-pointer"
                        leftIcon={<Plus className="w-3.5 h-3.5" />}
                      >
                        Añadir
                      </Button>
                    </div>
                  </Card>
                </motion.div>
              ))}
            </div>
          )}
        </div>

        {/* Sticky Cart Sidebar */}
        <div className="lg:col-span-4 sticky top-28 space-y-4">
          <Card className="p-6 bg-white border border-stone-200/90 shadow-xl shadow-stone-900/5 rounded-3xl space-y-5">
            <div className="flex items-center justify-between pb-3.5 border-b border-stone-100">
              <div className="flex items-center gap-2 text-sm font-black text-[#1c1917]">
                <ShoppingBag className="w-4 h-4 text-[#ea580c]" />
                Resumen de tu Pedido
              </div>
              <span className="text-xs font-bold text-[#005141] bg-[#f0fdfa] px-2.5 py-0.5 rounded-full border border-teal-100 font-mono">
                {totalItemsCount} items
              </span>
            </div>

            {/* Selected Slot Indicator in Cart */}
            <div className="p-3 bg-[#f0fdfa] rounded-2xl border border-teal-200/80 flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-[#005141] shrink-0" />
                <div>
                  <div className="font-bold text-[#005141] flex items-center gap-1.5">
                    {selectedSlot.timeRange}
                  </div>
                  <div className="text-[10px] text-[#0f766e] font-medium flex items-center gap-1">
                    {fulfillmentType === 'DELIVERY' ? (
                      <>
                        <Bike className="w-3 h-3 text-[#ea580c]" /> Delivery
                      </>
                    ) : (
                      <>
                        <Store className="w-3 h-3 text-[#005141]" /> Retiro en tienda
                      </>
                    )}
                  </div>
                </div>
              </div>
              <span className="text-[10px] font-bold text-[#005141] bg-white px-2 py-0.5 rounded-full border border-teal-100 shadow-2xs">
                Apartado
              </span>
            </div>

            {/* 10-Min Reservation Guarantee Ribbon */}
            <div className="p-3 bg-[#fffbeb] rounded-2xl border border-amber-200/70 flex items-start gap-2.5 text-xs text-[#92400e]">
              <Sparkles className="w-4 h-4 text-[#ea580c] shrink-0 mt-0.5" />
              <div className="leading-snug">
                <strong className="font-bold text-[#78350f] block">10 min de reserva garantizada</strong>
                Al continuar al checkout, tu cupo de preparación queda asegurado.
              </div>
            </div>

            {/* Cart Items List */}
            {cart.length === 0 ? (
              <div className="py-8 text-center text-[#57534e] space-y-2 bg-[#faf7f2] rounded-2xl p-4 border border-stone-200/70">
                <ShoppingBag className="w-8 h-8 text-[#a8a29e] mx-auto opacity-50" />
                <p className="text-xs font-bold text-[#1c1917]">Tu carrito está vacío</p>
                <p className="text-[11px] text-[#78716c] leading-relaxed">
                  Haz clic en cualquier producto del menú para comenzar tu pedido.
                </p>
              </div>
            ) : (
              <div className="space-y-3 max-h-60 overflow-y-auto pr-1">
                {cart.map((item) => (
                  <div
                    key={item.product.id}
                    className="p-3 rounded-2xl bg-[#faf7f2] border border-stone-200/80 flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="font-bold text-[#1c1917] truncate">{item.product.name}</div>
                      <div className="font-mono text-[#005141] font-bold">
                        {formatCurrency(item.product.price * item.quantity)}
                      </div>
                      {item.notes && (
                        <div className="text-[10px] text-stone-500 italic truncate">
                          Nota: {item.notes}
                        </div>
                      )}
                    </div>

                    {/* Quantity Stepper */}
                    <div className="flex items-center gap-1.5 bg-white p-1 rounded-xl border border-stone-200 shadow-2xs">
                      <button
                        type="button"
                        onClick={() => {
                          if (item.quantity === 1) {
                            removeCartItem(item.product.id);
                          } else {
                            updateCartQuantity(item.product.id, -1);
                          }
                        }}
                        className="w-6 h-6 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-700 flex items-center justify-center cursor-pointer transition-colors"
                        aria-label="Restar"
                      >
                        {item.quantity === 1 ? <Trash2 className="w-3 h-3 text-rose-500" /> : <Minus className="w-3 h-3" />}
                      </button>
                      <span className="w-5 text-center font-bold font-mono text-xs text-[#1c1917]">
                        {item.quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => updateCartQuantity(item.product.id, 1)}
                        className="w-6 h-6 rounded-lg bg-[#005141] hover:bg-[#00382d] text-white flex items-center justify-center cursor-pointer transition-colors"
                        aria-label="Sumar"
                      >
                        <Plus className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Calculations & Drawer Open Action */}
            <div className="pt-2 space-y-2 border-t border-stone-100 text-xs text-[#57534e]">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span className="font-mono font-bold text-[#1c1917]">{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span>{fulfillmentType === 'DELIVERY' ? 'Delivery estimado' : 'Retiro en tienda'}</span>
                <span className="font-mono font-bold text-[#1c1917]">
                  {deliveryFee === 0 ? 'Gratis' : formatCurrency(deliveryFee)}
                </span>
              </div>
              <div className="flex justify-between text-sm font-extrabold text-[#1c1917] pt-2 border-t border-stone-100">
                <span>Total</span>
                <span className="font-mono text-base text-[#005141] font-black">{formatCurrency(total)}</span>
              </div>

              {/* Drawer View Button */}
              {cart.length > 0 && (
                <button
                  type="button"
                  onClick={() => setIsCartDrawerOpen(true)}
                  className="w-full py-2.5 px-3 bg-stone-100 hover:bg-stone-200 text-[#1c1917] rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Clock className="w-3.5 h-3.5 text-[#005141]" />
                  <span>Ver Carrito con Cronómetro de Reserva</span>
                </button>
              )}

              <Button
                variant="terracotta"
                size="md"
                disabled={cart.length === 0}
                onClick={() => setIsPaymentModalOpen(true)}
                className="w-full mt-2 py-3.5 rounded-2xl font-bold shadow-lg shadow-[#ea580c]/30 cursor-pointer border-0 disabled:opacity-50"
                rightIcon={<MessageSquare className="w-4 h-4" />}
              >
                Continuar al Pedido por WhatsApp
              </Button>
            </div>
          </Card>
        </div>
      </div>

      {/* Floating Mobile Cart Trigger Pill (visible on small screens when cart has items) */}
      <AnimatePresence>
        {cart.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 30 }}
            className="fixed bottom-6 left-4 right-4 z-40 sm:hidden"
          >
            <button
              type="button"
              onClick={() => setIsCartDrawerOpen(true)}
              className="w-full bg-[#005141] text-white p-4 rounded-2xl shadow-2xl flex items-center justify-between border border-emerald-400/30 cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <span className="w-7 h-7 rounded-lg bg-white/20 flex items-center justify-center font-bold text-xs">
                  {totalItemsCount}
                </span>
                <span className="font-bold text-sm">Ver Carrito</span>
              </div>

              <div className="flex items-center gap-2 font-mono font-black text-sm">
                <span>{formatCurrency(total)}</span>
                <ArrowRight className="w-4 h-4 text-emerald-300" />
              </div>
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Slide-over Cart Drawer with SVG Circular Countdown Ring */}
      <CartDrawer
        isOpen={isCartDrawerOpen}
        onClose={() => setIsCartDrawerOpen(false)}
        cart={cart}
        onUpdateQuantity={updateCartQuantity}
        onRemoveItem={removeCartItem}
        selectedSlot={selectedSlot}
        fulfillmentType={fulfillmentType}
        deliveryFee={deliveryFee}
        storeName={storeName}
        countdownSeconds={holdCountdown}
        onProceedCheckout={() => {
          setIsCartDrawerOpen(false);
          setIsPaymentModalOpen(true);
        }}
      />

      {/* Payment Hub Modal (Yape / Plin / Transferencia) */}
      <PaymentHubModal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        total={total}
        storeName={storeName}
        slotTime={selectedSlot.timeRange}
        countdownSeconds={holdCountdown}
        onProceedToWhatsApp={(details) => {
          setSelectedPaymentMethod(details.method);
          setHasUploadedReceipt(!!details.receiptFile);
          setIsPaymentModalOpen(false);
          setIsWhatsAppModalOpen(true);
        }}
      />

      {/* WhatsApp Bridge Modal */}
      <WhatsAppBridgeModal
        isOpen={isWhatsAppModalOpen}
        onClose={() => setIsWhatsAppModalOpen(false)}
        cart={cart}
        selectedSlot={selectedSlot}
        fulfillmentType={fulfillmentType}
        deliveryFee={deliveryFee}
        storeName={storeName}
        paymentMethod={selectedPaymentMethod}
        hasReceipt={hasUploadedReceipt}
        onTrackOrder={(id) => {
          setCurrentOrderId(id);
          setIsWhatsAppModalOpen(false);
          setIsTrackerModalOpen(true);
        }}
      />

      {/* Live Order Tracker & Digital Boarding Pass Modal (Paso 4) */}
      <OrderTrackerModal
        isOpen={isTrackerModalOpen}
        onClose={() => setIsTrackerModalOpen(false)}
        orderId={currentOrderId}
        storeName={storeName}
        cart={cart}
        selectedSlot={selectedSlot}
        fulfillmentType={fulfillmentType}
        deliveryFee={deliveryFee}
        paymentMethod={selectedPaymentMethod}
      />

      {/* Product Customization Modal */}
      <AnimatePresence>
        {selectedProduct && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 15 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 15 }}
              className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-5 relative"
            >
              {/* Close Button */}
              <button
                type="button"
                onClick={() => setSelectedProduct(null)}
                className="absolute top-4 right-4 p-2 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-500 hover:text-[#1c1917] cursor-pointer transition-colors"
                aria-label="Cerrar modal"
              >
                <X className="w-4 h-4" />
              </button>

              {/* Product Header in Modal */}
              <div className="flex items-start gap-4 pr-8">
                <div className={`w-16 h-16 rounded-2xl ${selectedProduct.imageBg} flex items-center justify-center text-4xl shadow-sm shrink-0`}>
                  {selectedProduct.iconText}
                </div>
                <div>
                  <h3 className="text-lg font-black text-[#1c1917] leading-snug">
                    {selectedProduct.name}
                  </h3>
                  <div className="text-base font-black text-[#005141] font-mono mt-1">
                    {formatCurrency(selectedProduct.price)}
                  </div>
                </div>
              </div>

              <p className="text-xs text-[#57534e] leading-relaxed bg-[#faf7f2] p-3 rounded-2xl border border-stone-200/80">
                {selectedProduct.description}
              </p>

              {/* Quantity Selector */}
              <div className="flex items-center justify-between p-3.5 bg-[#faf7f2] rounded-2xl border border-stone-200/80">
                <span className="text-xs font-bold text-[#1c1917]">Cantidad:</span>
                <div className="flex items-center gap-3 bg-white px-3 py-1.5 rounded-xl border border-stone-200 shadow-2xs">
                  <button
                    type="button"
                    onClick={() => setModalQuantity((q) => Math.max(1, q - 1))}
                    className="w-7 h-7 rounded-lg bg-stone-100 hover:bg-stone-200 text-stone-700 flex items-center justify-center cursor-pointer transition-colors"
                    aria-label="Restar"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="font-mono font-black text-sm text-[#1c1917] w-6 text-center">
                    {modalQuantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setModalQuantity((q) => q + 1)}
                    className="w-7 h-7 rounded-lg bg-[#005141] hover:bg-[#00382d] text-white flex items-center justify-center cursor-pointer transition-colors"
                    aria-label="Sumar"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Special Notes / Instructions */}
              <div>
                <label className="block text-xs font-bold text-[#1c1917] mb-1.5">
                  Instrucciones especiales para el local (opcional):
                </label>
                <textarea
                  rows={2}
                  value={modalNotes}
                  onChange={(e) => setModalNotes(e.target.value)}
                  placeholder="Ej: Empacar para regalo, sin azúcar, rebanado fino..."
                  className="w-full p-3 bg-white border border-stone-200 rounded-2xl text-xs text-[#1c1917] placeholder:text-[#a8a29e] focus:outline-none focus:ring-2 focus:ring-[#005141]"
                />
              </div>

              {/* Action Button */}
              <Button
                variant="primary"
                size="lg"
                onClick={() => addToCart(selectedProduct, modalQuantity, modalNotes)}
                className="w-full bg-[#005141] hover:bg-[#00382d] text-white font-extrabold py-3.5 rounded-2xl shadow-xl shadow-[#005141]/20 cursor-pointer border-0"
              >
                Añadir al Pedido • {formatCurrency(selectedProduct.price * modalQuantity)}
              </Button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
