'use client';

import { useEffect, useRef, useState } from 'react';
import { ShoppingCart, X } from 'lucide-react';
import { useStorefrontCart } from './StorefrontCartProvider';
import { CartSummarySidebar } from '../components/catalog/CartSummarySidebar';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';

export function StorefrontCart() {
  const { items, count, subtotal, currency, storeName, store, authenticated, loading, pending, error, loginRequired, dismissLogin, login, logout, clear, retry, changeQuantity } = useStorefrontCart();
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
      className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-white/20 text-white transition hover:bg-white/10">
      <ShoppingCart className="h-5 w-5" aria-hidden="true" />
      {count > 0 && <span aria-hidden="true" className="absolute -right-1.5 -top-1.5 flex min-w-5 h-5 items-center justify-center rounded-full bg-[#feae2c] px-1 text-[10px] font-bold text-stone-900">{count}</span>}
    </button>
    {open && <div id="storefront-cart" role="dialog" aria-label="Carrito de la tienda"
      className="fixed inset-x-4 top-20 max-h-[calc(100dvh-6rem)] overflow-y-auto rounded-3xl border border-stone-200 bg-white p-4 text-stone-900 shadow-2xl sm:absolute sm:inset-x-auto sm:top-14 sm:right-0 sm:w-96">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-display font-bold">Tu carrito{count > 0 ? ` en ${storeName}` : ''}</h2>
        <button ref={closeRef} type="button" onClick={close} aria-label="Cerrar carrito" className="rounded-lg p-2 hover:bg-stone-100"><X className="h-4 w-4" /></button>
      </div>
      {error && <div role="alert" className="mb-4 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}<button type="button" onClick={retry} disabled={pending} className="mt-2 block font-semibold underline">Volver a intentar</button></div>}
      {loading ? <p role="status" className="py-4 text-sm text-stone-600">Consultando tu carrito...</p> : !authenticated || loginRequired ? <form onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        void login(String(data.get('email')), String(data.get('password')));
      }} className="space-y-3">
        <p className="text-sm text-stone-600">Inicia sesión como cliente para guardar tu carrito y recuperarlo cuando vuelvas.</p>
        <label className="block text-sm font-semibold">Correo<input required type="email" name="email" autoComplete="username" className="mt-1 w-full rounded-xl border border-stone-300 p-2.5 font-normal" /></label>
        <label className="block text-sm font-semibold">Contraseña<input required type="password" name="password" autoComplete="current-password" className="mt-1 w-full rounded-xl border border-stone-300 p-2.5 font-normal" /></label>
        <button disabled={pending} className="w-full rounded-xl bg-[#005141] p-3 font-semibold text-white disabled:opacity-50">{pending ? 'Iniciando sesión...' : 'Ingresar como cliente'}</button>
      </form> : !store ? <p className="py-4 text-sm text-stone-600">Explora una tienda para consultar su carrito y elegir productos.</p> : <>
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
