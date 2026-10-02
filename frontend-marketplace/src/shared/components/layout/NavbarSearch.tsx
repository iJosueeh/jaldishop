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
          className="w-full pl-9 pr-8 py-2 bg-white/10 hover:bg-white/15 focus:bg-[#141413] border border-white/10 focus:border-white/25 rounded-2xl text-xs sm:text-sm text-white placeholder-stone-300 backdrop-blur-md outline-none transition-all shadow-xs text-left"
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

      {/* Inline Suggestions Popover */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.98 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            className="absolute top-full left-0 right-0 sm:min-w-[360px] mt-2 bg-[#121211]/98 backdrop-blur-2xl rounded-2xl shadow-2xl shadow-black/80 border border-white/12 overflow-hidden z-50 text-white"
          >
            {/* Top Brand Ambient Line */}
            <div className="h-[2px] w-full bg-gradient-to-r from-[#d9532f] via-[#feae2c] to-[#005141]" />

            <div className="p-2">
              {/* Header */}
              <div className="px-3 py-1.5 flex items-center justify-between text-[11px] font-semibold text-stone-400 border-b border-white/5 mb-1 pb-1.5">
                <span className="flex items-center gap-1.5 text-stone-300">
                  <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
                  {query ? "Resultados encontrados" : "Tiendas recomendadas"}
                </span>
                <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded-full text-stone-300">
                  {filteredStores.length} sugerencias
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
                        className={`w-full flex items-center justify-between p-2.5 rounded-xl transition-all text-left cursor-pointer border ${
                          isHighlighted
                            ? "bg-white/[0.08] border-white/15 text-white"
                            : "bg-transparent border-transparent text-stone-300 hover:bg-white/[0.05]"
                        }`}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div
                            className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition-colors ${
                              isJade
                                ? "bg-emerald-500/15 text-emerald-400 border border-emerald-500/25"
                                : isTerracotta
                                  ? "bg-[#d9532f]/15 text-[#feae2c] border border-[#d9532f]/25"
                                  : "bg-amber-500/15 text-amber-400 border border-amber-500/25"
                            }`}
                          >
                            <Store className="w-4 h-4" />
                          </div>
                          <div className="min-w-0 text-left">
                            <div className="text-xs font-bold text-white truncate group-hover:text-[#feae2c]">
                              {store.name}
                            </div>
                            <div className="text-[11px] text-stone-400 truncate">
                              {store.category}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 shrink-0 ml-2">
                          <div
                            className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                              isJade
                                ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/30"
                                : isTerracotta
                                  ? "bg-[#d9532f]/15 text-orange-300 border border-[#d9532f]/30"
                                  : "bg-amber-500/15 text-amber-300 border border-amber-500/30"
                            }`}
                          >
                            <span
                              className={`w-1.5 h-1.5 rounded-full ${
                                isJade
                                  ? "bg-emerald-400 animate-pulse"
                                  : isTerracotta
                                    ? "bg-[#d9532f]"
                                    : "bg-amber-400"
                              }`}
                            />
                            {store.badge}
                          </div>
                          <ArrowRight
                            className={`w-3.5 h-3.5 transition-all ${
                              isHighlighted
                                ? "text-[#feae2c] translate-x-0.5"
                                : "text-stone-600"
                            }`}
                          />
                        </div>
                      </button>
                    );
                  })
                ) : (
                  <div className="p-4 text-center space-y-2">
                    <p className="text-xs text-stone-400">
                      {query
                        ? `No encontramos tiendas con "${query}"`
                        : 'Aún no hay tiendas activas disponibles'}
                    </p>
                    <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1">
                      {["Panadería", "Tortas", "Dark Kitchen"].map((chip) => (
                        <button
                          key={chip}
                          type="button"
                          onClick={() => {
                            setQuery(chip);
                            inputRef.current?.focus();
                          }}
                          className="text-[11px] px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 text-stone-300 border border-white/10 transition-colors cursor-pointer"
                        >
                          {chip}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Bottom Quick Action */}
              <div className="mt-1 pt-2 border-t border-white/5 px-2 flex items-center justify-between text-xs">
                <span className="text-[11px] text-stone-400">
                  ¿Buscas una categoría específica?
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setIsOpen(false);
                    onNavigate?.();
                    router.push("/tienda/panaderia-don-pepe");
                  }}
                  className="text-[11px] font-semibold text-[#feae2c] hover:text-[#ffd276] flex items-center gap-1 transition-colors cursor-pointer"
                >
                  <span>Explorar todas</span>
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
