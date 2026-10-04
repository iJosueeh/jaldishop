'use client';

import React, { useState } from 'react';
import { ShoppingBag, Search, Plus, Minus, Clock, PackageOpen } from 'lucide-react';
import { motion } from 'framer-motion';
import { Button } from '@/shared/components/ui/Button';
import { Card } from '@/shared/components/ui/Card';
import { ProductVisual } from './ProductVisual';
import { CapacitySlotPicker, CapacitySlot } from './CapacitySlotPicker';
import { StorefrontNav } from './StorefrontNav';
import { formatCurrency } from '@/shared/utils/formatters';

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
  imageUrl?: string;
  prepTimeMinutes?: number;
}

export interface CartItem {
  product: ProductItem;
  quantity: number;
  notes?: string;
}

interface CatalogContainerProps {
  storeSlug?: string;
  storeName: string;
  products?: ProductItem[] | null;
  slots?: CapacitySlot[] | null;
  catalogError?: boolean;
  capacityError?: boolean;
  capacitySlot?: React.ReactNode;
}

export function CatalogContainer({ storeName, products, slots, catalogError = false, capacityError = false, capacitySlot }: CatalogContainerProps) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<CapacitySlot | null>(null);
  const [fulfillment, setFulfillment] = useState<'DELIVERY' | 'PICKUP'>('DELIVERY');
  const items = products ?? [];
  const categories = [...new Set(items.map((product) => product.category))];
  const filtered = items.filter((product) =>
    (category === 'all' || product.category === category) &&
    (product.name + ' ' + product.description).toLocaleLowerCase().includes(query.toLocaleLowerCase())
  );
  const changeQuantity = (product: ProductItem, delta: number) => setCart((previous) => {
    const existing = previous.find((item) => item.product.id === product.id);
    if (!existing) return delta > 0 ? [...previous, { product, quantity: delta }] : previous;
    return previous.map((item) => item.product.id === product.id ? { ...item, quantity: item.quantity + delta } : item).filter((item) => item.quantity > 0);
  });
  const subtotal = cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  const catalogTitle = catalogError ? 'No pudimos cargar los productos' : products == null ? 'El catálogo todavía no está disponible' : 'Esta tienda aún no tiene productos publicados';
  const catalogMessage = catalogError
    ? `No pudimos consultar el catálogo de ${storeName}. Recarga la página para volver a intentarlo.`
    : products == null
    ? `No podemos mostrar los productos ni los precios de ${storeName} en este momento. Cuando su catálogo esté disponible, aparecerá aquí.`
    : `${storeName} todavía no ha publicado productos para comprar desde esta página.`;
  const capacityTitle = capacityError ? 'No pudimos consultar los horarios' : slots == null ? 'Horarios de pedido no disponibles' : 'No hay horarios disponibles';
  const capacityMessage = capacityError
    ? 'Recarga la página para volver a consultar la disponibilidad. No se ha reservado ningún cupo.'
    : slots == null
    ? `Aún no podemos consultar los cupos de ${storeName}. No es posible reservar un horario desde esta página.`
    : 'La tienda no tiene horarios disponibles para seleccionar. Vuelve a consultar más adelante.';

  return (
    <div className="space-y-8">
      <section id="catalogo" className="scroll-mt-40">
        <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-2xl font-bold text-stone-900">Productos de {storeName}</h2>
            <p className="mt-2 text-sm text-stone-600">El catálogo y los precios publicados por este negocio.</p>
          </div>
          {items.length > 0 && <label className="relative block">
            <span className="sr-only">Buscar productos en el catálogo</span>
            <Search aria-hidden="true" className="absolute left-3 top-3 h-4 w-4 text-stone-400" />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar un producto…" className="w-full rounded-xl border border-stone-200 bg-white py-2.5 pl-9 pr-3 text-sm focus:outline-2 focus:outline-[#005141]" />
          </label>}
        </div>
        {items.length === 0 || catalogError ? (
          <Card className="rounded-3xl border border-stone-200 bg-white p-8 sm:p-12 text-center">
            <PackageOpen aria-hidden="true" className="mx-auto mb-5 h-10 w-10 text-[#005141]" />
            <h3 className="font-display text-xl font-semibold text-stone-900">{catalogTitle}</h3>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-relaxed text-stone-600">{catalogMessage}</p>
            {catalogError && <Button className="mt-5" onClick={() => window.location.reload()}>Volver a intentar</Button>}
          </Card>
        ) : (
          <>
            <StorefrontNav categories={[{ id: 'all', name: 'Todos los productos', slug: 'todos', itemCount: items.length }, ...categories.map((name) => ({ id: name, name, slug: name, itemCount: items.filter((product) => product.category === name).length }))]} activeCategoryId={category} onSelectCategory={setCategory} />
            <div className="mt-6 grid gap-8 lg:grid-cols-[2fr_1fr] items-start">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                {filtered.map((product) => (
                  <motion.article key={product.id} whileHover={{ y: -3 }} className="group overflow-hidden rounded-3xl border border-stone-200 bg-white shadow-sm hover:shadow-xl hover:border-[#005141] transition-shadow">
                    <ProductVisual name={product.name} imageUrl={product.imageUrl} imageBg={product.imageBg} iconText={product.iconText} />
                    <div className="p-5">
                      <h3 className="min-h-12 text-lg font-bold text-stone-900">{product.name}</h3>
                      <p className="mt-2 min-h-10 text-sm text-stone-600 line-clamp-2">{product.description}</p>
                      <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-stone-100 pt-4">
                        <span className="font-bold text-[#005141]">{formatCurrency(product.price)}</span>
                        <Button size="sm" onClick={() => changeQuantity(product, 1)} leftIcon={<Plus className="h-4 w-4" />}>Añadir</Button>
                      </div>
                    </div>
                  </motion.article>
                ))}
                {filtered.length === 0 && <div className="sm:col-span-2 rounded-2xl bg-white p-8 text-center border border-stone-200">
                  <h3 className="font-semibold">No hay productos que coincidan con tu búsqueda</h3>
                  <p className="mt-2 text-sm text-stone-600">Prueba otra palabra o selecciona otra categoría de {storeName}.</p>
                  <Button variant="outline" className="mt-4" onClick={() => { setQuery(''); setCategory('all'); }}>Ver todos los productos</Button>
                </div>}
              </div>
              <aside className="rounded-3xl border border-stone-200 bg-white p-6 lg:sticky lg:top-28">
                <h3 className="flex items-center gap-2 font-bold"><ShoppingBag className="h-5 w-5 text-[#005141]" />Tu selección</h3>
                {cart.length === 0 ? <p className="mt-4 text-sm text-stone-600">Añade productos de {storeName} para ver tu selección.</p> : cart.map(({ product, quantity }) => <div key={product.id} className="mt-4 border-t border-stone-100 pt-4">
                  <p className="text-sm font-semibold">{product.name}</p>
                  <div className="mt-2 flex items-center justify-between gap-2">
                    <span className="text-sm">{formatCurrency(product.price * quantity)}</span>
                    <div className="flex items-center gap-3">
                      <button aria-label={`Quitar una unidad de ${product.name}`} onClick={() => changeQuantity(product, -1)} className="rounded-lg bg-stone-100 p-2"><Minus className="h-4 w-4" /></button>
                      <span>{quantity}</span>
                      <button aria-label={`Añadir una unidad de ${product.name}`} onClick={() => changeQuantity(product, 1)} className="rounded-lg bg-stone-100 p-2"><Plus className="h-4 w-4" /></button>
                    </div>
                  </div>
                </div>)}
                {cart.length > 0 && <p className="mt-5 flex justify-between border-t pt-4 font-bold"><span>Subtotal</span><span>{formatCurrency(subtotal)}</span></p>}
                <p className="mt-5 text-sm leading-relaxed text-stone-600">La compra en línea todavía no está disponible. Esta selección no crea un pedido ni reserva un cupo.</p>
                <Button disabled className="mt-4 w-full">Compra en línea no disponible</Button>
              </aside>
            </div>
          </>
        )}
      </section>
      {capacitySlot ?? (slots && slots.length > 0 && !capacityError ? (
        <CapacitySlotPicker slots={slots} selectedSlotId={selectedSlot?.id} onSelectSlot={setSelectedSlot} fulfillmentType={fulfillment} onFulfillmentTypeChange={setFulfillment} />
      ) : (
        <section className="rounded-3xl border border-stone-200 bg-white p-6 sm:p-8">
          <h2 className="flex items-center gap-2 text-lg font-semibold text-stone-900"><Clock aria-hidden="true" className="h-5 w-5 text-[#005141]" />{capacityTitle}</h2>
          <p className="mt-3 max-w-2xl text-sm leading-relaxed text-stone-600">{capacityMessage}</p>
        </section>
      ))}
    </div>
  );
}
