'use client';

import Link from 'next/link';
import { useState } from 'react';
import { ArrowLeft, Plus } from 'lucide-react';
import { ProductVisual } from './ProductVisual';
import { ProductItem, PublicStore } from '../types/storefront.types';
import { formatCurrency } from '@/shared/utils/formatters';
import { useStorefrontCart } from '../cart/StorefrontCartProvider';
import { FavoriteButton } from './FavoriteButton';

export function ProductDetailView({ store, product }: { store: PublicStore; product: ProductItem }) {
  const variants = product.variants?.filter((variant) => variant.status === 'ACTIVE') ?? [];
  const [variantId, setVariantId] = useState(variants.length === 1 ? variants[0].id : '');
  const { selectStore, changeQuantity, pending } = useStorefrontCart();
  const selected = variants.find((variant) => variant.id === variantId);

  const addToCart = async () => {
    if (!selected || !store.slug) return;
    selectStore({ id: store.id, name: store.name, slug: store.slug });
    await changeQuantity({ ...product, variantId: selected.id, price: Number(selected.priceAmount) }, 1);
  };

  return (
    <div className="min-h-screen bg-[#fcfbfa] py-8 sm:py-12">
      <main className="mx-auto w-full max-w-5xl px-4 sm:px-6 lg:px-8">
        <Link href={`/tienda/${store.slug}`} className="inline-flex items-center gap-2 text-sm font-semibold text-stone-600 transition-colors hover:text-[#005141]">
          <ArrowLeft className="h-4 w-4" /> Volver a {store.name}
        </Link>
        <article className="mt-5 grid gap-7 rounded-[12px] border border-stone-200/90 bg-white p-4 shadow-xs sm:p-6 lg:grid-cols-2 lg:p-8">
          <div className="overflow-hidden rounded-[12px] bg-stone-100"><ProductVisual name={product.name} imageUrl={product.imageUrl} imageBg={product.imageBg ?? 'bg-stone-100'} iconText={product.iconText ?? 'Producto'} /></div>
          <div className="flex flex-col">
            {product.category && <span className="text-xs font-bold uppercase tracking-wider text-[#ea580c]">{product.category}</span>}
            <div className="mt-2 flex items-start justify-between gap-4"><h1 className="font-display text-3xl font-black leading-tight text-stone-900">{product.name}</h1><FavoriteButton productId={product.id} /></div>
            <p className="mt-4 text-sm leading-relaxed text-stone-600">{product.description || 'La tienda no agregó una descripción para este producto.'}</p>
            <div className="mt-6 space-y-2"><label htmlFor="presentation" className="text-sm font-bold text-stone-800">Presentación</label><select id="presentation" value={variantId} onChange={(event) => setVariantId(event.target.value)} className="w-full rounded-xl border border-stone-200 bg-stone-50 p-3 text-sm font-medium text-stone-900 focus:border-[#005141] focus:outline-none">{variants.length !== 1 && <option value="">Elige una presentación</option>}{variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.presentationName} · {formatCurrency(variant.priceAmount, variant.priceCurrency)}</option>)}</select></div>
            {selected?.attributes?.length ? <dl className="mt-4 grid grid-cols-2 gap-2">{selected.attributes.map((attribute) => <div key={attribute.name} className="rounded-lg bg-stone-50 px-3 py-2 text-xs"><dt className="font-semibold text-stone-500">{attribute.name}</dt><dd className="mt-0.5 text-stone-800">{attribute.value}</dd></div>)}</dl> : null}
            <div className="mt-auto flex items-center justify-between gap-4 border-t border-stone-100 pt-6"><div><p className="text-xs font-bold uppercase tracking-wider text-stone-400">Precio</p><p className="font-display text-2xl font-black text-stone-900">{selected ? formatCurrency(selected.priceAmount, selected.priceCurrency) : formatCurrency(product.price)}</p></div><button type="button" onClick={addToCart} disabled={pending || !selected} className="inline-flex items-center gap-2 rounded-full bg-[#005141] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#00382d] disabled:cursor-not-allowed disabled:opacity-50"><Plus className="h-4 w-4 text-[#feae2c]" />Añadir al carrito</button></div>
          </div>
        </article>
      </main>
    </div>
  );
}
