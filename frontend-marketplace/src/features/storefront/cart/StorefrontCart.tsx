'use client';

import { useEffect, useRef, useState } from 'react';
import { ShoppingCart, X } from 'lucide-react';
import { useStorefrontCart } from './StorefrontCartProvider';
import { CartSummarySidebar } from '../components/catalog/CartSummarySidebar';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';

export function StorefrontCart() {
  const { items, count, subtotal, currency, storeName, store, authenticated, loading, pending, error, loginRequired, dismissLogin, logout, clear, retry, changeQuantity } = useStorefrontCart();
  const pathname = usePathname();
  const router = useRouter();
  const [manuallyOpen, setOpen] = useState(false);
  const open = manuallyOpen || loginRequired;
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    closeRef.current?.focus();
    const dismiss = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) { setOpen(false); dismissLogin(); }
    };
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { setOpen(false); dismissLogin(); triggerRef.current?.focus(); }
    };
    document.addEventListener('pointerdown', dismiss);
    document.addEventListener('keydown', escape);
    return () => {
      document.removeEventListener('pointerdown', dismiss);
      document.removeEventListener('keydown', escape);
    };
  }, [open, dismissLogin]);

  const close = () => { setOpen(false); dismissLogin(); triggerRef.current?.focus(); };

  return <div ref={containerRef} className="relative" onBlur={(event) => {
    if (event.relatedTarget && !event.currentTarget.contains(event.relatedTarget as Node)) { setOpen(false); dismissLogin(); }
  }}>
    <button ref={triggerRef} type="button" onClick={() => open ? close() : setOpen(true)}
      aria-label={`Carrito, ${count} ${count === 1 ? 'producto' : 'productos'}`}
      aria-expanded={open} aria-controls="storefront-cart" aria-haspopup="dialog"
      className="relative flex h-10 w-10 items-center justify-center rounded-full border border-white/20 text-white transition hover:bg-white/10">
      <ShoppingCart className="h-5 w-5" aria-hidden="true" />
      {count > 0 && <span aria-hidden="true" className="absolute -right-1 -top-1 flex min-w-5 h-5 items-center justify-center rounded-full bg-[#feae2c] px-1 text-[10px] font-bold text-stone-900 shadow-xs">{count}</span>}
    </button>
    {open && <div id="storefront-cart" role="dialog" aria-label="Carrito de la tienda"
      className="fixed inset-x-4 top-20 max-h-[calc(100dvh-6rem)] overflow-y-auto rounded-[12px] border border-stone-200/90 bg-white p-5 text-stone-900 shadow-2xl sm:absolute sm:inset-x-auto sm:top-14 sm:right-0 sm:w-96">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-display font-bold">Tu carrito{count > 0 ? ` en ${storeName}` : ''}</h2>
        <button ref={closeRef} type="button" onClick={close} aria-label="Cerrar carrito" className="rounded-full p-2 hover:bg-stone-100 transition-colors"><X className="h-4 w-4" /></button>
      </div>
      {error && <div role="alert" className="mb-4 rounded-[12px] bg-red-50 p-3 text-sm text-red-700">{error}<button type="button" onClick={retry} disabled={pending} className="mt-2 block font-semibold underline">Volver a intentar</button></div>}
      {loading ? (
        <p role="status" className="py-4 text-sm text-stone-600">Consultando tu carrito...</p>
      ) : !authenticated || loginRequired ? (
        <div className="space-y-4 py-3">
          <p className="text-sm text-stone-600 leading-relaxed">
            Inicia sesión o crea tu cuenta de cliente para guardar tu carrito y realizar pedidos.
          </p>
          <div className="flex flex-col gap-2 pt-1">
            <Link
              href={`/login?redirect=${encodeURIComponent(store ? `/tienda/${store.slug}` : pathname || '/')}`}
              onClick={close}
              className="inline-flex items-center justify-center w-full rounded-full bg-[#005141] hover:bg-[#00382d] p-3 text-sm font-bold text-white shadow-xs transition-all active:scale-95 text-center cursor-pointer"
            >
              Ingresar como cliente
            </Link>
            <Link
              href={`/registro?redirect=${encodeURIComponent(store ? `/tienda/${store.slug}` : pathname || '/')}`}
              onClick={close}
              className="inline-flex items-center justify-center w-full rounded-full border border-stone-300 hover:bg-stone-50 p-2.5 text-xs font-semibold text-stone-700 transition-colors text-center cursor-pointer"
            >
              ¿No tienes cuenta? Regístrate aquí
            </Link>
          </div>
        </div>
      ) : !store ? (
        <p className="py-4 text-sm text-stone-600">Explora una tienda para consultar su carrito y elegir productos.</p>
      ) : <>
      <CartSummarySidebar embedded pending={pending} currency={currency} cart={items} storeName={storeName} subtotal={subtotal}
        onChangeQuantity={changeQuantity} onCheckout={count > 0 ? () => {
          close();
          if (pathname === `/tienda/${store?.slug}`) document.getElementById('entrega')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          else router.push(`/tienda/${store?.slug}#entrega`);
        } : undefined} />
      {pathname !== `/tienda/${store.slug}` && <Link href={`/tienda/${store.slug}${count > 0 ? '#entrega' : '#catalogo'}`} onClick={close} className="mt-3 block text-center text-sm font-semibold text-[#005141] underline">Continuar en {storeName}</Link>}
      {count > 0 && <button onClick={() => void clear()} disabled={pending} className="mt-3 text-sm text-stone-600 underline disabled:opacity-50">Vaciar carrito</button>}
      </>}
      {pending && authenticated && <p role="status" className="mt-3 text-xs text-stone-600">Actualizando carrito...</p>}
      {authenticated && <button onClick={() => void logout()} disabled={pending} className="mt-4 block text-xs text-stone-500 underline">Cerrar sesión de cliente</button>}
    </div>}
  </div>;
}
