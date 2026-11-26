'use client';

import React, { useMemo, useState } from 'react';
import Link from 'next/link';
import {
  Heart,
  Store,
  Trash2,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  Clock,
  Compass,
  Search,
} from 'lucide-react';
import { useCustomerFavorites } from '../hooks/useCustomerFavorites';
import { AccountPageHeader } from './AccountPageHeader';
import { AccountEmptyState } from './AccountEmptyState';
import { AccountPagination } from './AccountPagination';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';
import { cn } from '@/shared/utils/cn';

type AvailabilityFilter = 'ALL' | 'AVAILABLE' | 'UNAVAILABLE';

const PAGE_SIZE = 9;

export function FavoritesView() {
  const [search, setSearch] = useState('');
  const [availability, setAvailability] = useState<AvailabilityFilter>('ALL');
  const [page, setPage] = useState(1);

  const { favorites, isLoading, error, removeFavorite, reload } = useCustomerFavorites();

  const filteredFavorites = useMemo(() => {
    const query = search.trim().toLowerCase();
    return favorites.filter((item) => {
      if (availability === 'AVAILABLE' && !item.isAvailable) return false;
      if (availability === 'UNAVAILABLE' && item.isAvailable) return false;
      if (!query) return true;
      return (
        item.productName.toLowerCase().includes(query) ||
        item.storeName.toLowerCase().includes(query)
      );
    });
  }, [favorites, availability, search]);

  const totalPages = Math.max(1, Math.ceil(filteredFavorites.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const paginatedFavorites = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return filteredFavorites.slice(start, start + PAGE_SIZE);
  }, [filteredFavorites, safePage]);

  const resetFilters = () => {
    setSearch('');
    setAvailability('ALL');
    setPage(1);
  };

  return (
    <div className="space-y-5 w-full">
      <AccountPageHeader
        title="Mis Favoritos"
        description="Tus productos guardados de comercios locales para recompra ágil."
        badge={
          <span className="inline-flex items-center justify-center px-2.5 py-0.5 rounded-full text-xs font-bold tabular-nums bg-rose-50 text-rose-700 border border-rose-200">
            {favorites.length}
          </span>
        }
        actions={
          <Badge variant="jade" size="sm" className="font-bold">
            Capacidad Garantizada
          </Badge>
        }
      />

      {!isLoading && !error && favorites.length > 0 && (
        <div className="rounded-xl border border-[#e7e0d6] bg-white p-3.5 sm:p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search
              className="w-4 h-4 text-[#78716c] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              aria-hidden="true"
            />
            <input
              type="search"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              aria-label="Buscar favoritos"
              placeholder="Buscar por producto o comercio..."
              className="w-full min-h-[44px] pl-9 pr-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2]/50 focus:bg-white text-sm text-[#1c1917] placeholder-[#78716c] focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div
            role="group"
            aria-label="Filtrar favoritos por disponibilidad"
            className="flex flex-wrap items-center gap-1.5"
          >
            {(
              [
                { key: 'ALL', label: 'Todos' },
                { key: 'AVAILABLE', label: 'Disponibles' },
                { key: 'UNAVAILABLE', label: 'Sin cupo' },
              ] as const
            ).map((opt) => {
              const isActive = availability === opt.key;
              return (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => {
                    setAvailability(opt.key);
                    setPage(1);
                  }}
                  className={cn(
                    'min-h-[44px] px-3.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isActive
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {isLoading && (
        <div
          role="status"
          aria-busy="true"
          aria-label="Cargando favoritos"
          className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 animate-pulse"
        >
          {[1, 2, 3].map((n) => (
            <div
              key={n}
              className="h-48 bg-white rounded-xl border border-[#e7e0d6] p-5 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="h-4 w-28 bg-[#f0eae0] rounded-md" />
                <div className="h-5 w-40 bg-[#f0eae0] rounded-md" />
                <div className="h-3 w-20 bg-[#f7f3ec] rounded-full" />
              </div>
              <div className="h-10 w-full bg-[#f7f3ec] rounded-lg" />
            </div>
          ))}
        </div>
      )}

      {!isLoading && error && (
        <div
          role="alert"
          className="rounded-xl border border-rose-200 bg-rose-50/70 p-5 flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left"
        >
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <div>
              <p className="font-semibold text-rose-950 text-xs sm:text-sm">
                No pudimos sincronizar tus favoritos
              </p>
              <p className="text-xs text-rose-800 mt-0.5">{error}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={reload}
            className="min-h-[44px] inline-flex items-center gap-1.5 px-4 text-xs font-bold text-white bg-[#005141] rounded-lg hover:bg-[#003d31] transition-colors shrink-0 cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reintentar</span>
          </button>
        </div>
      )}

      {!isLoading && !error && favorites.length === 0 && (
        <AccountEmptyState
          icon={<Heart className="w-6 h-6 text-rose-600" />}
          title="Aún no tienes productos guardados en favoritos"
          description="Guarda artículos recurrentes o creaciones de tus tiendas favoritas para agregarlos a tu pedido con un solo clic."
          action={
            <Link href="/#tiendas-destacadas">
              <Button
                variant="primary"
                size="md"
                className="min-h-[44px] rounded-full px-6 font-bold text-xs sm:text-sm"
                leftIcon={<Compass className="w-4 h-4 text-white" />}
              >
                Explorar comercios locales
              </Button>
            </Link>
          }
        />
      )}

      {!isLoading && !error && favorites.length > 0 && filteredFavorites.length === 0 && (
        <AccountEmptyState
          icon={<Search className="w-6 h-6" />}
          title="No encontramos favoritos con esos filtros"
          description="Prueba buscando otro producto o limpia los filtros para ver toda tu lista."
          action={
            <button
              type="button"
              onClick={resetFilters}
              className="min-h-[44px] px-5 rounded-full bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors cursor-pointer"
            >
              Limpiar filtros
            </button>
          }
        />
      )}

      {!isLoading && !error && paginatedFavorites.length > 0 && (
        <div className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {paginatedFavorites.map((item) => (
              <article
                key={item.productId}
                className="flex flex-col justify-between rounded-xl bg-white border border-[#e7e0d6] hover:border-[#005141]/40 transition-colors p-5"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <Link
                      href={`/tienda/${item.storeSlug}`}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#faf7f2] hover:bg-[#f0fdfa] border border-[#e7e0d6] text-[#1c1917] hover:text-[#005141] text-xs font-semibold transition-colors truncate max-w-[200px]"
                    >
                      <Store className="w-3.5 h-3.5 text-[#57534e] shrink-0" />
                      <span className="truncate">{item.storeName}</span>
                    </Link>

                    <button
                      type="button"
                      onClick={() => removeFavorite(item.productId)}
                      aria-label={`Quitar ${item.productName} de favoritos`}
                      title="Eliminar de favoritos"
                      className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-lg text-[#57534e] hover:text-rose-700 hover:bg-rose-50 transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-600"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  <h2 className="font-semibold text-[#1c1917] text-base leading-snug hover:text-[#005141] transition-colors">
                    <Link href={`/tienda/${item.storeSlug}`}>{item.productName}</Link>
                  </h2>

                  <div>
                    <Badge variant={item.isAvailable ? 'jade' : 'default'} size="sm">
                      {item.isAvailable ? 'Disponible' : 'Sin cupo temporal'}
                    </Badge>
                  </div>
                </div>

                <div className="pt-4 mt-4 border-t border-[#e7e0d6]/70 flex items-center justify-between gap-3">
                  <div>
                    <span className="text-[11px] font-semibold text-[#78716c] uppercase tracking-wider block">
                      Desde
                    </span>
                    <span className="text-lg font-bold tabular-nums text-[#1c1917]">
                      S/ {Number(item.minPrice).toFixed(2)}
                    </span>
                  </div>

                  <Link
                    href={`/tienda/${item.storeSlug}`}
                    className="min-h-[44px] inline-flex items-center gap-1 px-3.5 rounded-lg text-xs font-semibold text-[#005141] bg-[#f0fdfa] hover:bg-[#ccfbf1] border border-[#ccfbf1] transition-colors"
                  >
                    <span>Ver producto</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </article>
            ))}
          </div>

          <AccountPagination
            page={safePage}
            pageSize={PAGE_SIZE}
            totalItems={filteredFavorites.length}
            onPageChange={setPage}
            itemLabel="favoritos"
          />

          <div className="p-4 rounded-xl bg-white border border-[#e7e0d6] flex items-start gap-3 text-xs text-[#57534e]">
            <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
            <div className="space-y-0.5">
              <p className="font-bold text-[#1c1917]">
                Atención y preparación por turnos
              </p>
              <p className="leading-relaxed">
                Los comercios de JaldiShop gestionan su atención según cupos y franjas horarias. Si un producto favorito aparece temporalmente como &ldquo;Sin cupo temporal&rdquo;, consulta en la tienda los próximos horarios disponibles.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
