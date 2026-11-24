'use client';

import { createContext, useContext, useEffect, useState, ReactNode, useRef, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ApiException } from '@/core/api/apiClient';
import { CartItem, ProductItem } from '../types/storefront.types';
import { cartService, CartResponse } from './services/cartService';
import { customerSessionService } from './services/customerSessionService';

export interface ActiveStore { id: string; name: string; slug: string }
interface StorefrontCartState {
  items: CartItem[]; count: number; subtotal: number; currency: string; storeName: string;
  store: ActiveStore | null; recentStores: ActiveStore[]; authenticated: boolean; loading: boolean; pending: boolean;
  error: string | null; loginRequired: boolean;
  selectStore: (store: ActiveStore) => void;
  removeStoreFromHistory: (storeId: string) => void;
  changeQuantity: (product: ProductItem, delta: number) => Promise<void>;
  removeItem: (variantId: string) => Promise<void>;
  clear: () => Promise<void>; retry: () => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>; dismissLogin: () => void;
}
const CartContext = createContext<StorefrontCartState | null>(null);
const STORAGE_KEY = 'jaldishop-active-cart-store';
const RECENT_STORES_KEY = 'jaldishop-recent-cart-stores';
const MAX_RECENT_STORES = 30;
const cartKey = (id?: string) => ['customer-cart', id];

function isValidStore(value: unknown): value is ActiveStore {
  return Boolean(
    value &&
    typeof value === 'object' &&
    typeof (value as ActiveStore).id === 'string' &&
    typeof (value as ActiveStore).name === 'string' &&
    typeof (value as ActiveStore).slug === 'string'
  );
}

function readRecentStores(): ActiveStore[] {
  try {
    const raw = JSON.parse(localStorage.getItem(RECENT_STORES_KEY) || '[]');
    if (!Array.isArray(raw)) return [];
    return raw.filter(isValidStore).slice(0, MAX_RECENT_STORES);
  } catch {
    return [];
  }
}

export function StorefrontCartScope({ children }: { children: ReactNode }) {
  return <StorefrontCartProvider>{children}</StorefrontCartProvider>;
}

export function StorefrontCartProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const [store, setStore] = useState<ActiveStore | null>(null);
  const [recentStores, setRecentStores] = useState<ActiveStore[]>(() =>
    typeof window !== 'undefined' ? readRecentStores() : []
  );
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
    setRecentStores((prev) => {
      const base = prev.length > 0 ? prev : readRecentStores();
      const updated = [next, ...base.filter((s) => s.id !== next.id)].slice(0, MAX_RECENT_STORES);
      try { localStorage.setItem(RECENT_STORES_KEY, JSON.stringify(updated)); } catch { /* Storage is optional */ }
      return updated;
    });
    if (activeStore.current?.id === next.id) return;
    activeStore.current = next;
    pendingAdd.current = null;
    setStore(next);
    setError(null);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* Storage is optional; the cart lives on the server. */ }
  }, []);

  const removeStoreFromHistory = useCallback((storeId: string) => {
    setRecentStores((prev) => {
      const updated = prev.filter((s) => s.id !== storeId);
      try { localStorage.setItem(RECENT_STORES_KEY, JSON.stringify(updated)); } catch { /* Storage is optional */ }
      return updated;
    });
  }, []);

  useEffect(() => {
    const storedRecent = readRecentStores();
    if (storedRecent.length > 0) setRecentStores(storedRecent);
    if (activeStore.current) return;
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (isValidStore(saved)) selectStore(saved);
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
    if (!authenticated) {
      setLoginRequired(true);
      toast.info('Inicia sesión en tu cuenta de cliente para guardar tu carrito.');
      return;
    }
    const storeId = activeStore.current?.id || store?.id;
    if (!storeId || busy.current) return;
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
      const currentCart = client.getQueryData<CartResponse>(cartKey(id)) ?? cart.data;
      const item = currentCart?.items.find((entry) => entry.variantId === variantId);
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
    storeName: store?.name || 'esta tienda', store, recentStores, authenticated, pending,
    loading: session.isPending || (authenticated && !!store && cart.isFetching),
    error: error || (requestError instanceof ApiException && requestError.status === 401 ? 'Tu sesión terminó. Inicia sesión de nuevo.' : requestError ? 'No pudimos consultar tu carrito. Vuelve a intentarlo.' : null),
    loginRequired, selectStore, removeStoreFromHistory, changeQuantity,
    removeItem: (variantId: string) => mutate((id) => cartService.remove(id, variantId)),
    clear: () => mutate(cartService.clear),
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
