'use client';

import React, { useState, useRef, useEffect, useCallback, useDeferredValue } from 'react';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Search, X, Loader2, Store, ArrowRight, Sparkles } from 'lucide-react';
import { storeService } from '@/features/storefront/services/storeService';
import { PublicStore } from '@/features/storefront/types/storefront.types';

interface MobileSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function MobileSearchModal({ isOpen, onClose }: MobileSearchModalProps) {
  const [query, setQuery] = useState('');
  const [stores, setStores] = useState<PublicStore[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const router = useRouter();

  const deferredQuery = useDeferredValue(query);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      const timer = setTimeout(() => {
        inputRef.current?.focus();
      }, 150);
      return () => {
        clearTimeout(timer);
        document.body.style.overflow = 'unset';
      };
    } else {
      document.body.style.overflow = 'unset';
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, onClose]);

  useEffect(() => {
    let isMounted = true;
    if (!isOpen) return;

    setIsLoading(true);
    storeService
      .searchStores(deferredQuery, 6)
      .then((data) => {
        if (isMounted) {
          setStores(data);
        }
      })
      .catch(() => {
        if (isMounted) {
          setStores([]);
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [deferredQuery, isOpen]);

  const handleSelectStore = useCallback(
    (slug: string) => {
      onClose();
      setQuery('');
      router.push(`/tienda/${slug}`);
    },
    [router, onClose]
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (stores.length > 0 && stores[0]) {
      handleSelectStore(stores[0].slug);
    } else if (query.trim()) {
      onClose();
      router.push('/tienda/panaderia-don-pepe');
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, y: 15, scale: 0.985 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{
            opacity: 0,
            y: 20,
            scale: 0.985,
            transition: { duration: 0.22, ease: [0.32, 0, 0.67, 0] },
          }}
          transition={{
            type: 'spring',
            damping: 32,
            stiffness: 380,
            mass: 0.7,
          }}
          className="fixed inset-0 z-50 md:hidden bg-[#0a0a09]/98 backdrop-blur-2xl flex flex-col text-white will-change-transform"
        >
          {/* Top Brand Ambient Line */}
          <div className="h-[2px] w-full bg-gradient-to-r from-[#d9532f] via-[#feae2c] to-[#005141] shrink-0" />

          {/* Top Search Bar Header */}
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ delay: 0.04, duration: 0.2 }}
            className="p-3 sm:p-4 border-b border-white/10 flex items-center gap-2.5 shrink-0 bg-black/40"
          >
            {/* Back Button */}
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-xl text-stone-300 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
              aria-label="Cerrar búsqueda"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>

            {/* Search Input Box */}
            <form onSubmit={handleSubmit} className="relative flex-1 flex items-center">
              {isLoading ? (
                <Loader2 className="w-4 h-4 text-[#feae2c] animate-spin absolute left-3.5 pointer-events-none shrink-0" />
              ) : (
                <Search className="w-4 h-4 text-stone-400 absolute left-3.5 pointer-events-none shrink-0" />
              )}
              <input
                ref={inputRef}
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar tiendas o productos..."
                className="w-full pl-9 pr-9 py-2.5 bg-white/10 focus:bg-white/15 border border-white/15 focus:border-[#d9532f]/60 rounded-2xl text-sm text-white placeholder-stone-400 outline-none transition-all shadow-inner"
              />
              {query && (
                <button
                  type="button"
                  onClick={() => {
                    setQuery('');
                    inputRef.current?.focus();
                  }}
                  className="absolute right-2.5 p-1 text-stone-400 hover:text-white rounded-lg transition-colors cursor-pointer"
                  aria-label="Limpiar texto"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </form>

            {/* Cancel Button */}
            <button
              type="button"
              onClick={onClose}
              className="text-xs font-semibold text-stone-300 hover:text-white px-1.5 py-2 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
          </motion.div>

          {/* Results Header */}
          <div className="px-5 py-2.5 flex items-center justify-between text-xs font-semibold text-stone-400 border-b border-white/5 shrink-0 bg-white/[0.01]">
            <span className="flex items-center gap-1.5 text-stone-300">
              <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
              {query ? 'Resultados encontrados' : 'Tiendas recomendadas'}
            </span>
            <span className="text-[11px] bg-white/10 px-2 py-0.5 rounded-full text-stone-300">
              {stores.length} disponibles
            </span>
          </div>

          {/* Scrollable Results List with Stagger Animation */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2.5">
            {stores.length > 0 ? (
              stores.map((store, idx) => {
                const isJade = store.badgeVariant === 'jade';
                const isTerracotta = store.badgeVariant === 'terracotta';

                return (
                  <motion.button
                    key={store.slug}
                    type="button"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{
                      delay: 0.06 + idx * 0.035,
                      duration: 0.22,
                      ease: 'easeOut',
                    }}
                    onClick={() => handleSelectStore(store.slug)}
                    className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.04] active:bg-white/15 border border-white/8 hover:border-white/15 transition-all text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div
                        className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 shadow-sm ${
                          isJade
                            ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/25'
                            : isTerracotta
                            ? 'bg-[#d9532f]/15 text-[#feae2c] border border-[#d9532f]/25'
                            : 'bg-amber-500/15 text-amber-400 border border-amber-500/25'
                        }`}
                      >
                        <Store className="w-5 h-5" />
                      </div>
                      <div className="min-w-0 text-left">
                        <div className="text-sm font-bold text-white truncate">
                          {store.name}
                        </div>
                        <div className="text-xs text-stone-400 truncate mt-0.5">
                          {store.category || store.tagline}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 ml-2">
                      {store.badge && (
                        <div
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold ${
                            isJade
                              ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                              : isTerracotta
                              ? 'bg-[#d9532f]/15 text-orange-300 border border-[#d9532f]/30'
                              : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                          }`}
                        >
                          <span
                            className={`w-1.5 h-1.5 rounded-full ${
                              isJade
                                ? 'bg-emerald-400 animate-pulse'
                                : isTerracotta
                                ? 'bg-[#d9532f]'
                                : 'bg-amber-400'
                            }`}
                          />
                          {store.badge}
                        </div>
                      )}
                      <ArrowRight className="w-4 h-4 text-stone-500" />
                    </div>
                  </motion.button>
                );
              })
            ) : (
              <motion.div
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.25, ease: 'easeOut' }}
                className="py-12 px-4 text-center space-y-4"
              >
                <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 text-stone-400 flex items-center justify-center mx-auto">
                  <Search className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-bold text-stone-200">
                    {query ? `No encontramos tiendas para "${query}"` : 'Aún no hay tiendas activas'}
                  </p>
                  <p className="text-xs text-stone-400 max-w-xs mx-auto">
                    Prueba buscando por rubro o selecciona una de las sugerencias populares:
                  </p>
                </div>
                <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
                  {['Panadería', 'Tortas', 'Dark Kitchen', 'Regalos'].map((chip) => (
                    <button
                      key={chip}
                      type="button"
                      onClick={() => {
                        setQuery(chip);
                        inputRef.current?.focus();
                      }}
                      className="text-xs px-3.5 py-1.5 rounded-xl bg-white/10 active:bg-white/20 text-stone-200 border border-white/10 transition-colors cursor-pointer"
                    >
                      {chip}
                    </button>
                  ))}
                </div>
              </motion.div>
            )}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
