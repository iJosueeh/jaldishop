'use client';

import { createContext, useContext, useEffect, useState, ReactNode, useRef, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ApiException } from '@/core/api/apiClient';
import { CartItem, ProductItem } from '../types/storefront.types';
import { cartService, CartResponse } from './services/cartService';
import { customerSessionService } from './services/customerSessionService';

interface ActiveStore { id: string; name: string; slug: string }
interface StorefrontCartState {
  items: CartItem[]; count: number; subtotal: number; currency: string; storeName: string;
  store: ActiveStore | null; authenticated: boolean; loading: boolean; pending: boolean;
  error: string | null; loginRequired: boolean;
  selectStore: (store: ActiveStore) => void;
  changeQuantity: (product: ProductItem, delta: number) => Promise<void>;
  clear: () => Promise<void>; retry: () => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>; dismissLogin: () => void;
}
const CartContext = createContext<StorefrontCartState | null>(null);
const STORAGE_KEY = 'jaldishop-active-cart-store';
const cartKey = (id?: string) => ['customer-cart', id];

export function StorefrontCartScope({ children }: { children: ReactNode }) {
  return <StorefrontCartProvider>{children}</StorefrontCartProvider>;
}

export function StorefrontCartProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const [store, setStore] = useState<ActiveStore | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loginRequired, setLoginRequired] = useState(false);
  const busy = useRef(false);
  const activeStore = useRef<ActiveStore | null>(null);
  const pendingAdd = useRef<{ storeId: string; variantId: string; quantity: number } | null>(null);
  const session = useQuery({ queryKey: ['customer-session'], queryFn: customerSessionService.get, retry: false, staleTime: 0 });
  const authenticated = session.data?.authenticated === true;
  const cart = useQuery({ queryKey: cartKey(store?.id), queryFn: () => cartService.get(store!.id), enabled: authenticated && !!store, retry: false, staleTime: 0 });

  const selectStore = useCallback((next: ActiveStore) => {
    if (activeStore.current?.id === next.id) return;
    activeStore.current = next;
    pendingAdd.current = null;
    setStore(next);
    setError(null);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* Storage is optional; the cart lives on the server. */ }
  }, []);

  useEffect(() => {
    if (activeStore.current) return;
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (saved && typeof saved.id === 'string' && typeof saved.name === 'string' && typeof saved.slug === 'string') selectStore(saved);
    } catch { /* Ignore invalid store metadata. Never restore local product data. */ }
  }, [selectStore]);

  useEffect(() => {
    if (cart.error instanceof ApiException && cart.error.status === 401) {
      client.setQueryData(['customer-session'], { authenticated: false });
      client.removeQueries({ queryKey: ['customer-cart'] });
      setLoginRequired(true);
    }
  }, [cart.error, client]);

  const reportError = (reason: unknown, loginAttempt = false) => {
    const message = !loginAttempt && reason instanceof ApiException && reason.status === 401
      ? 'Tu sesión terminó. Inicia sesión de nuevo para consultar tu carrito.'
      : reason instanceof Error ? reason.message : 'No pudimos actualizar tu carrito.';
    if (!loginAttempt && reason instanceof ApiException && reason.status === 401) {
      client.setQueryData(['customer-session'], { authenticated: false });
      client.removeQueries({ queryKey: ['customer-cart'] });
      setLoginRequired(true);
    }
    setError(message);
    toast.error(message);
  };

  const mutate = async (operation: (id: string) => Promise<CartResponse>) => {
    if (!authenticated) { setLoginRequired(true); return; }
    if (!store || busy.current) return;
    const storeId = store.id;
    busy.current = true; setPending(true); setError(null);
    try {
      await client.cancelQueries({ queryKey: cartKey(storeId) });
      const result = await operation(storeId);
      client.setQueryData(cartKey(storeId), result);
    } catch (reason) { reportError(reason); }
    finally { busy.current = false; setPending(false); }
  };

  const changeQuantity = async (product: ProductItem, delta: number) => {
    const variantId = product.variantId || (product.variants?.length === 1 ? product.variants[0].id : undefined);
    if (!variantId) { setError('Selecciona una presentación disponible para añadir este producto.'); return; }
    if (!authenticated && store && delta > 0) pendingAdd.current = { storeId: store.id, variantId, quantity: delta };
    await mutate((id) => {
      const item = cart.data?.items.find((entry) => entry.variantId === variantId);
      if (!item) return delta > 0 ? cartService.add(id, variantId, delta) : cartService.get(id);
      const quantity = item.quantity + delta;
      return quantity <= 0 ? cartService.remove(id, variantId) : cartService.update(id, variantId, quantity);
    });
  };

  const login = async (email: string, password: string) => {
    if (busy.current) return;
    let signedIn = false;
    busy.current = true; setPending(true); setError(null);
    try {
      await customerSessionService.login(email, password);
      signedIn = true;
      setLoginRequired(false);
      client.removeQueries({ queryKey: ['customer-cart'] });
      const intent = pendingAdd.current;
      pendingAdd.current = null;
      if (intent && activeStore.current?.id === intent.storeId) {
        const savedCart = await cartService.add(intent.storeId, intent.variantId, intent.quantity);
        client.setQueryData(cartKey(intent.storeId), savedCart);
      }
    } catch (reason) {
      reportError(reason, !signedIn);
      if (reason instanceof ApiException && reason.status === 401 && signedIn) signedIn = false;
    }
    finally {
      if (signedIn) client.setQueryData(['customer-session'], { authenticated: true });
      busy.current = false; setPending(false);
    }
  };

  const data = authenticated ? cart.data : undefined;
  const items: CartItem[] = data?.items.map((item) => ({ quantity: item.quantity, available: item.available,
    product: { id: item.productId, variantId: item.variantId,
      name: item.productName + (item.presentationName ? ' — ' + item.presentationName : ''),
      description: '', category: '', price: Number(item.unitPriceAmount), priceCurrency: item.unitPriceCurrency, imageUrl: item.imageUrl },
  })) || [];
  const requestError = cart.error || session.error;
  return <CartContext.Provider value={{ items, count: data?.totalItems || 0, subtotal: Number(data?.totalAmount || 0), currency: data?.currency || 'PEN',
    storeName: store?.name || 'esta tienda', store, authenticated, pending,
    loading: session.isPending || (authenticated && !!store && cart.isFetching),
    error: error || (requestError instanceof ApiException && requestError.status === 401 ? 'Tu sesión terminó. Inicia sesión de nuevo.' : requestError ? 'No pudimos consultar tu carrito. Vuelve a intentarlo.' : null),
    loginRequired, selectStore, changeQuantity, clear: () => mutate(cartService.clear),
    retry: () => { setError(null); void session.refetch(); if (authenticated && store) void cart.refetch(); },
    login, logout: async () => {
      if (busy.current) return;
      busy.current = true; setPending(true);
      try { await customerSessionService.logout(); await client.cancelQueries({ queryKey: ['customer-cart'] }); client.setQueryData(['customer-session'], { authenticated: false }); client.removeQueries({ queryKey: ['customer-cart'] }); }
      catch (reason) { reportError(reason); }
      finally { busy.current = false; setPending(false); }
    }, dismissLogin: () => { setLoginRequired(false); pendingAdd.current = null; },
  }}>{children}</CartContext.Provider>;
}

export function useStorefrontCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error('Storefront cart requires StorefrontCartProvider');
  return context;
}
