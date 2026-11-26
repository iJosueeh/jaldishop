'use client';

import React, { useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  MessageSquareQuote,
  MessageSquarePlus,
  ChevronLeft,
  ChevronRight,
  Star,
  Store as StoreIcon,
  Pencil,
  Trash2,
  AlertTriangle,
  Loader2,
  ShieldCheck,
  Package,
  Sparkles,
  Search,
} from 'lucide-react';
import { notify, notifyKeys } from '@/shared/utils/notify';
import { useCustomerReviews } from '../hooks/useCustomerReviews';
import { ReviewModal } from './ReviewModal';
import { AccountPageHeader } from './AccountPageHeader';
import { AccountEmptyState } from './AccountEmptyState';
import { AccountPagination } from './AccountPagination';
import type { CustomerReview, PendingReviewProduct } from '../types/customer.types';
import { cn } from '@/shared/utils/cn';

type RatingFilter = 'ALL' | '5' | '4_PLUS' | '3_OR_LESS';

const PAGE_SIZE = 6;

export function ReviewsView() {
  const { reviews, pendingReviews = [], isLoading, isError, refetch, deleteReview, isDeleting } =
    useCustomerReviews();
  const [editingReview, setEditingReview] = useState<CustomerReview | null>(null);
  const [creatingPending, setCreatingPending] = useState<PendingReviewProduct | null>(null);
  const [reviewToDelete, setReviewToDelete] = useState<CustomerReview | null>(null);
  const carouselRef = useRef<HTMLDivElement>(null);

  const scrollPendingCarousel = (direction: 'prev' | 'next') => {
    carouselRef.current?.scrollBy({
      left: direction === 'next' ? 280 : -280,
      behavior: 'smooth',
    });
  };
  const [search, setSearch] = useState('');
  const [ratingFilter, setRatingFilter] = useState<RatingFilter>('ALL');
  const [page, setPage] = useState(1);

  const filteredReviews = useMemo(() => {
    const query = search.trim().toLowerCase();
    return reviews.filter((review) => {
      if (ratingFilter === '5' && review.rating !== 5) return false;
      if (ratingFilter === '4_PLUS' && review.rating < 4) return false;
      if (ratingFilter === '3_OR_LESS' && review.rating > 3) return false;

      if (!query) return true;
      return (
        review.productName.toLowerCase().includes(query) ||
        review.storeName.toLowerCase().includes(query) ||
        (review.comment || '').toLowerCase().includes(query)
      );
    });
  }, [reviews, ratingFilter, search]);

  const totalPages = Math.max(1, Math.ceil(filteredReviews.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const paginatedReviews = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return filteredReviews.slice(start, start + PAGE_SIZE);
  }, [filteredReviews, safePage]);

  const resetFilters = () => {
    setSearch('');
    setRatingFilter('ALL');
    setPage(1);
  };

  const handleConfirmDelete = async () => {
    if (!reviewToDelete) return;
    try {
      await deleteReview(reviewToDelete.productId);
      notify.success('Reseña eliminada correctamente', {
        id: notifyKeys.review(reviewToDelete.productId),
      });
      setReviewToDelete(null);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar la reseña';
      notify.error(msg, {
        id: notifyKeys.review(reviewToDelete.productId),
      });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6" data-testid="reviews-loading" role="status" aria-busy="true" aria-label="Cargando reseñas">
        <div className="h-10 w-48 bg-[#f0eae0] rounded-lg animate-pulse" />
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-32 bg-white rounded-xl border border-[#e7e0d6] animate-pulse"
            />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div
        role="alert"
        className="bg-white rounded-xl border border-[#e7e0d6] p-8 text-center space-y-4"
      >
        <p className="text-sm text-[#57534e]">
          Ocurrió un inconveniente al cargar tus opiniones.
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          className="min-h-[44px] inline-flex items-center gap-2 px-4 rounded-lg bg-[#1c1917] text-white text-xs font-semibold hover:bg-[#292524] transition-colors cursor-pointer"
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-5 w-full">
      <AccountPageHeader
        title="Mis Reseñas"
        description="Opiniones y valoraciones que has compartido sobre tus compras verificadas en JaldiShop."
        badge={
          <span className="inline-flex items-center justify-center px-2.5 py-0.5 text-xs font-bold tabular-nums rounded-full bg-[#f7f3ec] text-[#1c1917] border border-[#e7e0d6]">
            {reviews.length}
          </span>
        }
      />

      {pendingReviews.length > 0 && (
        <section
          aria-label="Productos pendientes por opinar"
          className="rounded-xl border border-[#e7e0d6] bg-[#f7f3ec]/60 p-4 space-y-3"
        >
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-[#1c1917]">Pendientes por opinar</h2>
              <span className="inline-flex items-center justify-center px-2 py-0.5 text-[11px] font-bold tabular-nums rounded-full bg-[#005141] text-white">
                {pendingReviews.length}
              </span>
            </div>
            <div className="hidden sm:flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => scrollPendingCarousel('prev')}
                aria-label="Desplazar pendientes a la izquierda"
                className="w-8 h-8 rounded-full border border-[#e7e0d6] bg-white text-[#1c1917] hover:bg-[#faf7f2] inline-flex items-center justify-center transition-colors cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => scrollPendingCarousel('next')}
                aria-label="Desplazar pendientes a la derecha"
                className="w-8 h-8 rounded-full border border-[#e7e0d6] bg-white text-[#1c1917] hover:bg-[#faf7f2] inline-flex items-center justify-center transition-colors cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div
            ref={carouselRef}
            className="flex gap-3 overflow-x-auto snap-x snap-mandatory pb-1"
          >
            {pendingReviews.map((item) => (
              <article
                key={item.productId}
                className="min-w-[240px] max-w-[260px] shrink-0 snap-start rounded-xl border border-[#e7e0d6] bg-white p-3.5 flex flex-col justify-between gap-3"
              >
                <div className="flex items-start gap-3">
                  {item.productImageUrl ? (
                    <Image
                      src={item.productImageUrl}
                      alt={item.productName}
                      width={44}
                      height={44}
                      className="w-11 h-11 rounded-lg object-cover border border-[#e7e0d6] shrink-0"
                    />
                  ) : (
                    <div className="w-11 h-11 rounded-lg bg-[#faf7f2] border border-[#e7e0d6] flex items-center justify-center text-[#78716c] shrink-0">
                      <Package className="w-4 h-4" />
                    </div>
                  )}
                  <div className="min-w-0">
                    <h3 className="text-xs font-bold text-[#1c1917] line-clamp-2 leading-snug">
                      {item.productName}
                    </h3>
                    <p className="text-[11px] text-[#57534e] truncate mt-0.5">
                      {item.storeName}
                    </p>
                    {item.lastOrderNumber && (
                      <p className="text-[10px] font-mono text-[#78716c] mt-0.5">
                        Pedido #{item.lastOrderNumber}
                      </p>
                    )}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setCreatingPending(item)}
                  className="w-full min-h-[38px] inline-flex items-center justify-center gap-1.5 rounded-lg bg-[#005141] px-3 py-1.5 text-xs font-semibold text-white hover:bg-[#003d31] transition-colors cursor-pointer"
                >
                  <MessageSquarePlus className="w-3.5 h-3.5 text-[#36f4a4]" />
                  <span>Opinar</span>
                </button>
              </article>
            ))}
          </div>
        </section>
      )}

      {reviews.length > 0 && (
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
              aria-label="Buscar reseñas"
              placeholder="Buscar por producto, comercio o comentario..."
              className="w-full min-h-[44px] pl-9 pr-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2]/50 focus:bg-white text-sm text-[#1c1917] placeholder-[#78716c] focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div
            role="group"
            aria-label="Filtrar reseñas por calificación"
            className="flex flex-wrap items-center gap-1.5"
          >
            {(
              [
                { key: 'ALL', label: 'Todas' },
                { key: '5', label: '5 estrellas' },
                { key: '4_PLUS', label: '4+ estrellas' },
                { key: '3_OR_LESS', label: '3 o menos' },
              ] as const
            ).map((opt) => {
              const isActive = ratingFilter === opt.key;
              return (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => {
                    setRatingFilter(opt.key);
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

      {reviews.length === 0 ? (
        <AccountEmptyState
          dataTestId="reviews-empty"
          icon={<MessageSquareQuote className="w-6 h-6" />}
          title="Aún no has compartido reseñas"
          description="Al completar un pedido, puedes calificar los productos adquiridos. Tus valoraciones aportan confianza y reconocen la labor de los negocios locales."
          action={
            <>
              <Link
                href="/mi-jaldishop/pedidos"
                className="min-h-[44px] inline-flex items-center gap-2 px-5 rounded-full bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors"
              >
                <Package className="w-4 h-4" />
                <span>Ver mis pedidos completados</span>
              </Link>
              <Link
                href="/"
                className="min-h-[44px] inline-flex items-center gap-2 px-5 rounded-full border border-[#e7e0d6] bg-white text-[#1c1917] text-xs font-semibold hover:bg-[#f7f3ec] transition-colors"
              >
                <Sparkles className="w-4 h-4 text-[#005141]" />
                <span>Explorar comercios</span>
              </Link>
            </>
          }
        />
      ) : filteredReviews.length === 0 ? (
        <AccountEmptyState
          icon={<Search className="w-6 h-6" />}
          title="No hay reseñas que coincidan con tu búsqueda"
          description="Prueba otro término o limpia los filtros para ver todas tus opiniones."
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
      ) : (
        <>
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4" data-testid="reviews-list">
            {paginatedReviews.map((review) => (
              <article
                key={review.id}
                className="bg-white rounded-xl border border-[#e7e0d6] p-5 space-y-4 hover:border-[#005141]/40 transition-colors"
              >
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                  <div className="flex items-start gap-3.5">
                    <div className="relative w-14 h-14 sm:w-16 sm:h-16 rounded-lg bg-[#faf7f2] border border-[#e7e0d6] overflow-hidden shrink-0 flex items-center justify-center">
                      {review.imageUrl ? (
                        <Image
                          src={review.imageUrl}
                          alt={review.productName}
                          fill
                          className="object-cover"
                          sizes="64px"
                        />
                      ) : (
                        <Package className="w-6 h-6 text-[#78716c]" />
                      )}
                    </div>

                    <div className="space-y-1">
                      <h2 className="text-sm sm:text-base font-bold text-[#1c1917] leading-snug">
                        {review.productName}
                      </h2>
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[#57534e]">
                        <span className="inline-flex items-center gap-1">
                          <StoreIcon className="w-3.5 h-3.5 text-[#78716c]" />
                          <span>{review.storeName}</span>
                        </span>
                        <span aria-hidden="true">&bull;</span>
                        <span className="inline-flex items-center gap-1 text-[#005141] font-medium">
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>Compra verificada</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setEditingReview(review)}
                      className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 rounded-lg border border-[#e7e0d6] text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                    >
                      <Pencil className="w-3.5 h-3.5 text-[#57534e]" />
                      <span>Editar opinión</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setReviewToDelete(review)}
                      aria-label={`Eliminar opinión de ${review.productName}`}
                      className="min-h-[44px] inline-flex items-center gap-1.5 px-3 rounded-lg border border-rose-200 text-xs font-semibold text-rose-700 hover:bg-rose-50 transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-600"
                    >
                      <Trash2 className="w-3.5 h-3.5 text-rose-600" />
                      <span className="hidden sm:inline">Eliminar</span>
                    </button>
                  </div>
                </div>

                <div className="flex items-center gap-3 pt-3 border-t border-[#e7e0d6]/70">
                  <div className="flex items-center gap-0.5" aria-label={`${review.rating} de 5 estrellas`}>
                    {[1, 2, 3, 4, 5].map((s) => (
                      <Star
                        key={s}
                        className={cn(
                          'w-4 h-4',
                          s <= review.rating
                            ? 'fill-amber-400 text-amber-400'
                            : 'fill-[#f7f3ec] text-[#e7e0d6]'
                        )}
                      />
                    ))}
                  </div>
                  <span className="text-xs text-[#57534e] tabular-nums">
                    {new Date(review.updatedAt || review.createdAt).toLocaleDateString('es-PE', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </span>
                  {review.status === 'HIDDEN' && (
                    <span className="text-[11px] font-medium text-[#92400e] bg-[#fffbeb] px-2 py-0.5 rounded-md border border-[#fde68a]">
                      En moderación
                    </span>
                  )}
                </div>

                {review.comment ? (
                  <p className="text-xs sm:text-sm text-[#1c1917] leading-relaxed pt-2 border-t border-[#e7e0d6]/50">
                    {review.comment}
                  </p>
                ) : (
                  <p className="text-xs text-[#78716c] italic pt-2 border-t border-[#e7e0d6]/50">
                    Calificaste este producto sin añadir comentario escrito.
                  </p>
                )}
              </article>
            ))}
          </div>

          <AccountPagination
            page={safePage}
            pageSize={PAGE_SIZE}
            totalItems={filteredReviews.length}
            onPageChange={setPage}
            itemLabel="reseñas"
          />
        </>
      )}

      {creatingPending && (
        <ReviewModal
          isOpen={Boolean(creatingPending)}
          onClose={() => setCreatingPending(null)}
          productId={creatingPending.productId}
          productName={creatingPending.productName}
          isEditing={false}
          onSuccess={() => {
            setCreatingPending(null);
            refetch();
          }}
        />
      )}

      {editingReview && (
        <ReviewModal
          isOpen={Boolean(editingReview)}
          onClose={() => setEditingReview(null)}
          productId={editingReview.productId}
          productName={editingReview.productName}
          initialRating={editingReview.rating}
          initialComment={editingReview.comment}
          isEditing={true}
          onSuccess={() => refetch()}
        />
      )}

      {reviewToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-review-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#02090a]/50 backdrop-blur-xs"
        >
          <div className="bg-white rounded-xl border border-[#e7e0d6] shadow-xl max-w-sm w-full p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-rose-50 border border-rose-200 flex items-center justify-center text-rose-600 shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h2 id="delete-review-dialog-title" className="text-base font-bold text-[#1c1917]">
                  ¿Eliminar opinión?
                </h2>
                <p className="text-xs text-[#57534e]">
                  Esta acción no se puede deshacer.
                </p>
              </div>
            </div>

            <p className="text-xs text-[#57534e] leading-relaxed">
              ¿Estás seguro de que deseas eliminar tu valoración sobre <strong className="text-[#1c1917]">{reviewToDelete.productName}</strong>?
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setReviewToDelete(null)}
                disabled={isDeleting}
                className="min-h-[44px] px-4 rounded-lg border border-[#e7e0d6] text-xs font-semibold text-[#1c1917] hover:bg-[#f7f3ec] transition-colors cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="min-h-[44px] inline-flex items-center gap-1.5 px-4 rounded-lg bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition-colors disabled:opacity-50 cursor-pointer"
              >
                {isDeleting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>{isDeleting ? 'Eliminando...' : 'Sí, eliminar'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
