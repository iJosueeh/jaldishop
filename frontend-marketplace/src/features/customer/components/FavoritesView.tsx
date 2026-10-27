'use client';

import React from 'react';
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
  Repeat,
  Sparkles,
  ShoppingBag,
} from 'lucide-react';
import { useCustomerFavorites } from '../hooks/useCustomerFavorites';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';

export function FavoritesView() {
  const { favorites, isLoading, error, removeFavorite, reload } = useCustomerFavorites();

  return (
    <div className="space-y-6 w-full">
      {/* 1. Encabezado Minimalista y Limpio */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-stone-200/80">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
              Mis Favoritos
            </h2>
            <span className="inline-flex items-center justify-center px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200/60">
              {favorites.length}
            </span>
          </div>
          <p className="text-xs sm:text-sm text-stone-600 mt-0.5">
            Tus productos guardados de comercios locales para recompra ágil.
          </p>
        </div>
        <Badge variant="jade" size="sm" className="font-bold self-start sm:self-auto shadow-2xs">
          Capacidad Garantizada
        </Badge>
      </div>

      {/* 2. Loading Skeleton */}
      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 animate-pulse">
          {[1, 2, 3].map((n) => (
            <div
              key={n}
              className="h-56 bg-white rounded-2xl border border-stone-200/90 p-5 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="h-4 w-28 bg-stone-200 rounded-md" />
                <div className="h-5 w-40 bg-stone-200 rounded-md" />
                <div className="h-3 w-16 bg-stone-100 rounded-full" />
              </div>
              <div className="h-9 w-full bg-stone-100 rounded-xl" />
            </div>
          ))}
        </div>
      )}

      {/* 3. Error State */}
      {!isLoading && error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50/70 p-5 flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
              <AlertCircle className="w-4 h-4" />
            </div>
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
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-white bg-[#005141] rounded-xl hover:bg-[#003e32] transition-colors shrink-0 shadow-xs cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reintentar</span>
          </button>
        </div>
      )}

      {/* 4. Empty State Minimalista con Iconos (Sin Emojis) */}
      {!isLoading && !error && favorites.length === 0 && (
        <div className="space-y-6">
          <div className="rounded-2xl bg-white border border-stone-200/90 shadow-xs p-6 sm:p-10 text-center max-w-2xl mx-auto">
            {/* Contenedor de icono sobrio */}
            <div className="mx-auto w-12 h-12 rounded-2xl bg-rose-50 border border-rose-200/70 flex items-center justify-center mb-4 text-rose-500 shadow-2xs">
              <Heart className="w-5 h-5 fill-rose-100" />
            </div>

            <h3 className="font-display text-lg sm:text-xl font-bold text-stone-900 tracking-tight">
              Aún no tienes productos guardados en favoritos
            </h3>
            <p className="text-xs sm:text-sm text-stone-600 mt-1.5 max-w-md mx-auto leading-relaxed">
              Guarda artículos recurrentes o creaciones de tus tiendas favoritas para agregarlos a tu pedido con un solo clic.
            </p>

            {/* Pilares MYPE Minimalistas con Iconos Vectoriales */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 my-6 text-left">
              <div className="p-3.5 rounded-xl bg-stone-50/70 border border-stone-200/60 flex flex-col gap-2">
                <div className="w-7 h-7 rounded-lg bg-emerald-50 text-[#005141] flex items-center justify-center shrink-0 border border-emerald-100">
                  <Repeat className="w-3.5 h-3.5" />
                </div>
                <div>
                  <p className="font-bold text-xs text-stone-900">Compras recurrentes</p>
                  <p className="text-[11px] text-stone-500 mt-0.5 leading-snug">
                    Artículos esenciales e insumos de uso diario.
                  </p>
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-stone-50/70 border border-stone-200/60 flex flex-col gap-2">
                <div className="w-7 h-7 rounded-lg bg-emerald-50 text-[#005141] flex items-center justify-center shrink-0 border border-emerald-100">
                  <Sparkles className="w-3.5 h-3.5" />
                </div>
                <div>
                  <p className="font-bold text-xs text-stone-900">Creaciones a medida</p>
                  <p className="text-[11px] text-stone-500 mt-0.5 leading-snug">
                    Piezas artesanales y pedidos personalizados.
                  </p>
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-stone-50/70 border border-stone-200/60 flex flex-col gap-2">
                <div className="w-7 h-7 rounded-lg bg-emerald-50 text-[#005141] flex items-center justify-center shrink-0 border border-emerald-100">
                  <ShoppingBag className="w-3.5 h-3.5" />
                </div>
                <div>
                  <p className="font-bold text-xs text-stone-900">Hogar y bienestar</p>
                  <p className="text-[11px] text-stone-500 mt-0.5 leading-snug">
                    Cuidado personal y diseño para tu espacio.
                  </p>
                </div>
              </div>
            </div>

            {/* Acción Primaria */}
            <div className="flex justify-center pt-1">
              <Link href="/#tiendas-destacadas">
                <Button
                  variant="primary"
                  size="md"
                  className="rounded-full px-6 font-bold shadow-md shadow-[#005141]/10 text-xs sm:text-sm"
                  leftIcon={<Compass className="w-4 h-4 text-white" />}
                >
                  Explorar comercios locales
                </Button>
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* 5. Grid de Tarjetas de Productos Favoritos */}
      {!isLoading && !error && favorites.length > 0 && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {favorites.map((item) => (
              <div
                key={item.productId}
                className="group relative flex flex-col justify-between rounded-2xl bg-white border border-stone-200/90 hover:border-emerald-300 hover:shadow-xs transition-all p-5"
              >
                <div className="space-y-3">
                  {/* Top Bar: Store Chip and Quick Remove */}
                  <div className="flex items-center justify-between gap-2">
                    <Link
                      href={`/tienda/${item.storeSlug}`}
                      className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-lg bg-stone-50 hover:bg-emerald-50 border border-stone-200 text-stone-700 hover:text-[#005141] text-xs font-semibold transition-colors truncate max-w-[200px]"
                    >
                      <Store className="w-3 h-3 text-stone-400 shrink-0" />
                      <span className="truncate">{item.storeName}</span>
                    </Link>

                    <button
                      type="button"
                      onClick={() => removeFavorite(item.productId)}
                      aria-label={`Quitar ${item.productName} de favoritos`}
                      title="Eliminar de favoritos"
                      className="p-1 rounded-md text-stone-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Product Title */}
                  <h3 className="font-semibold text-stone-900 text-base leading-snug hover:text-[#005141] transition-colors">
                    <Link href={`/tienda/${item.storeSlug}`}>{item.productName}</Link>
                  </h3>

                  {/* Availability Pill */}
                  <div>
                    <span
                      className={`inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full ${
                        item.isAvailable
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-stone-100 text-stone-500 border border-stone-200'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          item.isAvailable ? 'bg-emerald-500' : 'bg-stone-400'
                        }`}
                      />
                      <span>{item.isAvailable ? 'Disponible' : 'Sin cupo temporal'}</span>
                    </span>
                  </div>
                </div>

                {/* Bottom Bar: Price and Direct Store Link */}
                <div className="pt-4 mt-3 border-t border-stone-100 flex items-center justify-between gap-3">
                  <div>
                    <span className="text-[10px] font-semibold text-stone-400 uppercase tracking-wider block">
                      Desde
                    </span>
                    <span className="text-lg font-bold tabular-nums text-stone-900">
                      S/ {Number(item.minPrice).toFixed(2)}
                    </span>
                  </div>

                  <Link
                    href={`/tienda/${item.storeSlug}`}
                    className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-semibold text-[#005141] bg-emerald-50 hover:bg-emerald-100 transition-colors"
                  >
                    <span>Ver producto</span>
                    <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            ))}
          </div>

          {/* Información sobre el modelo MYPE y capacidad operativa */}
          <div className="p-4 rounded-xl bg-stone-50 border border-stone-200/80 flex items-start gap-3 text-xs text-stone-600">
            <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
            <div className="space-y-0.5">
              <p className="font-bold text-stone-800">
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
