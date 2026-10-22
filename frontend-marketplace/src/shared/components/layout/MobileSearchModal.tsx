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
          className="fixed inset-0 z-50 md:hidden bg-[#fcfbfa] flex flex-col text-stone-900 will-change-transform"
        >
          {/* Top Search Bar Header */}
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ delay: 0.04, duration: 0.2 }}
            className="p-3 sm:p-4 border-b border-stone-200/90 flex items-center gap-2.5 shrink-0 bg-white shadow-2xs"
          >
            {/* Back Button */}
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-full text-stone-600 hover:text-stone-900 hover:bg-stone-100 transition-colors cursor-pointer"
              aria-label="Cerrar búsqueda"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>

            {/* Search Input Box */}
            <form onSubmit={handleSubmit} className="relative flex-1 flex items-center">
              {isLoading ? (
                <Loader2 className="w-4 h-4 text-[#ea580c] animate-spin absolute left-3.5 pointer-events-none shrink-0" />
              ) : (
                <Search className="w-4 h-4 text-stone-400 absolute left-3.5 pointer-events-none shrink-0" />
              )}
              <input
                ref={inputRef}
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar tiendas o productos..."
                className="w-full pl-9 pr-9 py-2.5 bg-stone-50 focus:bg-white border border-stone-200/90 focus:border-[#005141] rounded-full text-sm text-stone-900 placeholder-stone-400 outline-none transition-all shadow-2xs"
              />
              {query && (
                <button
                  type="button"
                  onClick={() => {
                    setQuery('');
                    inputRef.current?.focus();
                  }}
                  className="absolute right-2.5 p-1 text-stone-400 hover:text-stone-700 rounded-full transition-colors cursor-pointer"
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
              className="text-xs font-bold text-stone-600 hover:text-stone-900 px-1.5 py-2 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
          </motion.div>

          {/* Results Header */}
          <div className="px-5 py-2.5 flex items-center justify-between text-xs font-semibold text-stone-500 border-b border-stone-200/80 shrink-0 bg-stone-50/70">
            <span className="flex items-center gap-1.5 text-stone-700 font-bold">
              <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
              {query ? 'Resultados de búsqueda' : 'Comercios y Emprendimientos MYPE'}
            </span>
            <span className="text-[11px] bg-stone-100 px-2.5 py-0.5 rounded-full text-stone-600 font-bold">
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
                    className="w-full flex items-center justify-between p-3.5 rounded-[12px] bg-white border border-stone-200/90 shadow-2xs hover:border-stone-300 active:scale-[0.99] transition-all text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div
                        className={`w-10 h-10 rounded-[8px] flex items-center justify-center shrink-0 shadow-2xs ${
                          isJade
                            ? 'bg-emerald-50 text-[#005141] border border-emerald-200/80'
                            : isTerracotta
                            ? 'bg-orange-50 text-[#ea580c] border border-orange-200/80'
                            : 'bg-amber-50 text-amber-800 border border-amber-200/80'
                        }`}
                      >
                        <Store className="w-5 h-5" />
                      </div>
                      <div className="min-w-0 text-left">
                        <div className="text-sm font-bold text-stone-900 truncate">
                          {store.name}
                        </div>
                        <div className="text-xs text-stone-500 truncate mt-0.5">
                          {store.category || store.tagline}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 ml-2">
                      {store.badge && (
                        <div
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold ${
                            isJade
                              ? 'bg-emerald-50 text-[#005141] border border-emerald-200/80'
                              : isTerracotta
                              ? 'bg-orange-50 text-[#ea580c] border border-orange-200/80'
                              : 'bg-amber-50 text-amber-800 border border-amber-200/80'
                          }`}
                        >
                          <span
                            className={`w-1.5 h-1.5 rounded-full ${
                              isJade
                                ? 'bg-[#005141]'
                                : isTerracotta
                                ? 'bg-[#ea580c]'
                                : 'bg-amber-500'
                            }`}
                          />
                          {store.badge}
                        </div>
                      )}
                      <ArrowRight className="w-4 h-4 text-stone-400" />
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
                <div className="w-12 h-12 rounded-[12px] bg-stone-100 border border-stone-200 text-stone-400 flex items-center justify-center mx-auto">
                  <Search className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-bold text-stone-900">
                    {query ? `No encontramos comercios para "${query}"` : 'Aún no hay tiendas activas'}
                  </p>
                  <p className="text-xs text-stone-500 max-w-xs mx-auto leading-relaxed">
                    Prueba buscando por rubro o selecciona una de las opciones sugeridas:
                  </p>
                </div>
                <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
                  {['Regalos & Detalles', 'Panadería & Pastelería', 'Florerías', 'Talleres'].map((chip) => (
                    <button
                      key={chip}
                      type="button"
                      onClick={() => {
                        setQuery(chip);
                        inputRef.current?.focus();
                      }}
                      className="text-xs px-3.5 py-1.5 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-700 font-medium border border-stone-200/80 transition-colors cursor-pointer"
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
