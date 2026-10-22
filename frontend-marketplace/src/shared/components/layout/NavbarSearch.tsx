"use client";

import React, {
  useState,
  useRef,
  useEffect,
  useCallback,
  useDeferredValue,
} from "react";
import { useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Store, X, ArrowRight, Sparkles, Loader2 } from "lucide-react";

import { storeService } from "@/features/storefront/services/storeService";
import { PublicStore } from "@/features/storefront/types/storefront.types";

interface NavbarSearchProps {
  className?: string;
  onNavigate?: () => void;
}

export function NavbarSearch({
  className = "",
  onNavigate,
}: NavbarSearchProps) {
  const [query, setQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [filteredStores, setFilteredStores] = useState<PublicStore[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const router = useRouter();

  const deferredQuery = useDeferredValue(query);

  useEffect(() => {
    let isMounted = true;
    if (!isOpen) return;

    setIsLoading(true);
    storeService
      .searchStores(deferredQuery, 5)
      .then((stores) => {
        if (isMounted) {
          setFilteredStores(stores);
          setSelectedIndex(0);
        }
      })
      .catch(() => {
        if (isMounted) {
          setFilteredStores([]);
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
      setIsOpen(false);
      setQuery("");
      onNavigate?.();
      router.push(`/tienda/${slug}`);
    },
    [router, onNavigate],
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (
      filteredStores.length > 0 &&
      selectedIndex >= 0 &&
      filteredStores[selectedIndex]
    ) {
      handleSelectStore(filteredStores[selectedIndex].slug);
    } else if (query.trim()) {
      setIsOpen(false);
      onNavigate?.();
      // Ready for catalog / stores page integration
      router.push(`/tienda/panaderia-don-pepe`);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      setIsOpen(false);
      inputRef.current?.blur();
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((prev) =>
        filteredStores.length > 0 ? (prev + 1) % filteredStores.length : 0,
      );
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((prev) =>
        filteredStores.length > 0
          ? (prev - 1 + filteredStores.length) % filteredStores.length
          : 0,
      );
    }
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <form onSubmit={handleSubmit} className="relative flex items-center">
        {isLoading ? (
          <Loader2 className="w-4 h-4 text-[#feae2c] animate-spin absolute left-3.5 pointer-events-none shrink-0" />
        ) : (
          <Search className="w-4 h-4 text-stone-300 absolute left-3.5 pointer-events-none shrink-0" />
        )}
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="Buscar tiendas o productos..."
          className="w-full pl-9 pr-8 py-2 bg-white/10 hover:bg-white/15 focus:bg-white border border-white/20 focus:border-stone-200 rounded-full text-xs sm:text-sm text-white focus:text-stone-900 placeholder-stone-300 focus:placeholder-stone-400 backdrop-blur-md outline-none transition-all shadow-xs text-left"
        />
        {query && (
          <button
            type="button"
            onClick={() => {
              setQuery("");
              inputRef.current?.focus();
            }}
            className="absolute right-2.5 p-1 text-stone-400 hover:text-white rounded-md transition-colors cursor-pointer"
            aria-label="Limpiar búsqueda"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </form>

      {/* Inline Suggestions Popover estilo Boutique MYPE */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.98 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            className="absolute top-full left-0 right-0 sm:min-w-[360px] mt-2 bg-white rounded-[12px] shadow-2xl shadow-stone-900/15 border border-stone-200/90 overflow-hidden z-50 text-stone-900"
          >
            <div className="p-3">
              {/* Header */}
              <div className="px-2 py-1 flex items-center justify-between text-[11px] font-semibold text-stone-500 border-b border-stone-100 mb-2 pb-2">
                <span className="flex items-center gap-1.5 text-stone-700 font-bold">
                  <Sparkles className="w-3.5 h-3.5 text-[#ea580c]" />
                  {query ? "Resultados de búsqueda" : "Comercios y Emprendimientos MYPE"}
                </span>
                <span className="text-[10px] bg-stone-100 px-2 py-0.5 rounded-full text-stone-600 font-bold">
                  {filteredStores.length} disponibles
                </span>
              </div>

              {/* Stores List */}
              <div className="space-y-1">
                {filteredStores.length > 0 ? (
                  filteredStores.map((store, idx) => {
                    const isHighlighted = selectedIndex === idx;
                    const isJade = store.badgeVariant === "jade";
                    const isTerracotta = store.badgeVariant === "terracotta";

                    return (
                      <button
                        key={`${store.slug}-${idx}`}
                        type="button"
                        onClick={() => handleSelectStore(store.slug)}
                        onMouseEnter={() => setSelectedIndex(idx)}
                        className={`w-full flex items-center justify-between p-2.5 rounded-[10px] transition-all text-left cursor-pointer border ${
                          isHighlighted
                            ? "bg-stone-50 border-stone-200 text-stone-900 shadow-2xs"
                            : "bg-transparent border-transparent text-stone-700 hover:bg-stone-50/80"
                        }`}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div
                            className={`w-8 h-8 rounded-[8px] flex items-center justify-center shrink-0 transition-colors ${
                              isJade
                                ? "bg-emerald-50 text-[#005141] border border-emerald-200/80"
                                : isTerracotta
                                  ? "bg-orange-50 text-[#ea580c] border border-orange-200/80"
                                  : "bg-amber-50 text-amber-800 border border-amber-200/80"
                            }`}
                          >
                            <Store className="w-4 h-4" />
                          </div>
                          <div className="min-w-0 text-left">
                            <div className="text-xs font-bold text-stone-900 truncate">
                              {store.name}
                            </div>
                            <div className="text-[11px] text-stone-500 truncate">
                              {store.category || store.tagline}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0 ml-2">
                          {store.badge && (
                            <div
                              className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                isJade
                                  ? "bg-emerald-50 text-[#005141] border border-emerald-200/80"
                                  : isTerracotta
                                    ? "bg-orange-50 text-[#ea580c] border border-orange-200/80"
                                    : "bg-amber-50 text-amber-800 border border-amber-200/80"
                              }`}
                            >
                              <span
                                className={`w-1.5 h-1.5 rounded-full ${
                                  isJade
                                    ? "bg-[#005141]"
                                    : isTerracotta
                                      ? "bg-[#ea580c]"
                                      : "bg-amber-500"
                                }`}
                              />
                              {store.badge}
                            </div>
                          )}
                          <ArrowRight
                            className={`w-3.5 h-3.5 transition-all ${
                              isHighlighted
                                ? "text-[#005141] translate-x-0.5"
                                : "text-stone-400"
                            }`}
                          />
                        </div>
                      </button>
                    );
                  })
                ) : (
                  <div className="p-4 text-center space-y-3">
                    <p className="text-xs text-stone-600">
                      {query
                        ? `No encontramos comercios con "${query}"`
                        : 'Explora por los rubros más solicitados:'}
                    </p>
                    <div className="flex flex-wrap items-center justify-center gap-1.5 pt-0.5">
                      {["Regalos & Detalles", "Panadería & Pastelería", "Florerías", "Talleres"].map((chip) => (
                        <button
                          key={chip}
                          type="button"
                          onClick={() => {
                            setQuery(chip);
                            inputRef.current?.focus();
                          }}
                          className="text-[11px] px-3 py-1 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-700 font-medium border border-stone-200/80 transition-colors cursor-pointer"
                        >
                          {chip}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Bottom Quick Action */}
              <div className="mt-2 pt-2 border-t border-stone-100 px-1 flex items-center justify-between text-xs">
                <span className="text-[11px] text-stone-500">
                  ¿Buscas un comercio local?
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setIsOpen(false);
                    onNavigate?.();
                    router.push("/#tiendas-destacadas");
                  }}
                  className="text-[11px] font-bold text-[#005141] hover:underline flex items-center gap-1 transition-colors cursor-pointer"
                >
                  <span>Ver comercios destacados</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
