'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  MessageSquareQuote,
  Star,
  Store as StoreIcon,
  Pencil,
  Trash2,
  AlertTriangle,
  Loader2,
  ShieldCheck,
  Package,
  Sparkles,
} from 'lucide-react';
import { toast } from 'sonner';
import { useCustomerReviews } from '../hooks/useCustomerReviews';
import { ReviewModal } from './ReviewModal';
import type { CustomerReview } from '../types/customer.types';
import { cn } from '@/shared/utils/cn';

export function ReviewsView() {
  const { reviews, isLoading, isError, refetch, deleteReview, isDeleting } = useCustomerReviews();
  const [editingReview, setEditingReview] = useState<CustomerReview | null>(null);
  const [reviewToDelete, setReviewToDelete] = useState<CustomerReview | null>(null);

  const handleConfirmDelete = async () => {
    if (!reviewToDelete) return;
    try {
      await deleteReview(reviewToDelete.productId);
      toast.success('Reseña eliminada correctamente');
      setReviewToDelete(null);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar la reseña';
      toast.error(msg);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6" data-testid="reviews-loading">
        <div className="h-10 w-48 bg-stone-200/70 rounded-lg animate-pulse" />
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-32 bg-white rounded-2xl border border-stone-200/80 animate-pulse"
            />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-2xl border border-stone-200/90 p-8 text-center space-y-4 shadow-xs">
        <p className="text-sm text-stone-600">
          Ocurrió un inconveniente al cargar tus opiniones.
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-stone-900 text-white text-xs font-semibold hover:bg-stone-800 transition-colors"
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-stone-200/80">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-stone-900">
              Mis Reseñas
            </h1>
            <span className="inline-flex items-center justify-center px-2 py-0.5 text-xs font-semibold rounded-full bg-stone-100 text-stone-600 border border-stone-200">
              {reviews.length}
            </span>
          </div>
          <p className="text-xs sm:text-sm text-stone-500">
            Opiniones y valoraciones que has compartido sobre tus compras verificadas en JaldiShop.
          </p>
        </div>
      </div>

      {/* Reviews list or empty state */}
      {reviews.length === 0 ? (
        <div
          data-testid="reviews-empty"
          className="bg-white rounded-2xl border border-dashed border-stone-300 p-8 sm:p-12 text-center space-y-6"
        >
          <div className="w-12 h-12 mx-auto rounded-full bg-stone-100 text-stone-400 flex items-center justify-center">
            <MessageSquareQuote className="w-6 h-6 text-stone-500" />
          </div>

          <div className="max-w-md mx-auto space-y-2">
            <h3 className="text-base font-bold text-stone-900">
              Aún no has compartido reseñas
            </h3>
            <p className="text-xs sm:text-sm text-stone-500 leading-relaxed">
              Al completar un pedido, puedes calificar los productos adquiridos. Tus valoraciones aportan confianza y reconocen la labor de los negocios locales.
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <Link
              href="/mi-jaldishop/pedidos"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#005141] text-white text-xs font-semibold hover:bg-[#004033] transition-colors shadow-xs"
            >
              <Package className="w-4 h-4" />
              <span>Ver mis pedidos completados</span>
            </Link>
            <Link
              href="/"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-stone-200 bg-white text-stone-700 text-xs font-semibold hover:bg-stone-50 transition-colors"
            >
              <Sparkles className="w-4 h-4 text-stone-400" />
              <span>Explorar comercios</span>
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-4" data-testid="reviews-list">
          {reviews.map((review) => (
            <div
              key={review.id}
              className="bg-white rounded-2xl border border-stone-200/90 shadow-xs p-5 sm:p-6 space-y-4 hover:border-stone-300 transition-colors"
            >
              {/* Product and Store Header */}
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div className="flex items-start gap-3.5">
                  <div className="relative w-14 h-14 sm:w-16 sm:h-16 rounded-xl bg-stone-100 border border-stone-200/80 overflow-hidden shrink-0 flex items-center justify-center">
                    {review.imageUrl ? (
                      <Image
                        src={review.imageUrl}
                        alt={review.productName}
                        fill
                        className="object-cover"
                        sizes="64px"
                      />
                    ) : (
                      <Package className="w-6 h-6 text-stone-400" />
                    )}
                  </div>

                  <div className="space-y-1">
                    <h3 className="text-sm sm:text-base font-bold text-stone-900 leading-snug">
                      {review.productName}
                    </h3>
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-stone-500">
                      <span className="inline-flex items-center gap-1 text-stone-600">
                        <StoreIcon className="w-3.5 h-3.5 text-stone-400" />
                        <span>{review.storeName}</span>
                      </span>
                      <span>&bull;</span>
                      <span className="inline-flex items-center gap-1 text-emerald-700 font-medium">
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                        <span>Compra verificada</span>
                      </span>
                    </div>
                  </div>
                </div>

                {/* Action Buttons: Edit and Delete */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditingReview(review)}
                    className="self-start inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-stone-200 text-xs font-semibold text-stone-700 hover:bg-stone-50 hover:text-stone-900 transition-colors cursor-pointer"
                  >
                    <Pencil className="w-3.5 h-3.5 text-stone-400" />
                    <span>Editar opinión</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setReviewToDelete(review)}
                    aria-label={`Eliminar opinión de ${review.productName}`}
                    className="self-start inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-rose-200 text-xs font-semibold text-rose-700 hover:bg-rose-50 hover:text-rose-900 transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5 text-rose-500" />
                    <span className="hidden sm:inline">Eliminar</span>
                  </button>
                </div>
              </div>

              {/* Rating & Date */}
              <div className="flex items-center gap-3 pt-1 border-t border-stone-100">
                <div className="flex items-center gap-0.5" aria-label={`${review.rating} de 5 estrellas`}>
                  {[1, 2, 3, 4, 5].map((s) => (
                    <Star
                      key={s}
                      className={cn(
                        'w-4 h-4',
                        s <= review.rating
                          ? 'fill-amber-400 text-amber-400'
                          : 'fill-stone-100 text-stone-300'
                      )}
                    />
                  ))}
                </div>
                <span className="text-xs text-stone-400">
                  {new Date(review.updatedAt || review.createdAt).toLocaleDateString('es-PE', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </span>
                {review.status === 'HIDDEN' && (
                  <span className="text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                    En moderación
                  </span>
                )}
              </div>

              {/* Comment text */}
              {review.comment ? (
                <p className="text-xs sm:text-sm text-stone-700 leading-relaxed bg-stone-50/70 p-3.5 rounded-xl border border-stone-100">
                  {review.comment}
                </p>
              ) : (
                <p className="text-xs text-stone-400 italic">
                  Calificaste este producto sin añadir comentario escrito.
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Review Modal for Editing */}
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

      {/* Delete Review Confirmation Dialog */}
      {reviewToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-review-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/40 backdrop-blur-xs animate-in fade-in duration-150"
        >
          <div className="bg-white rounded-2xl border border-stone-200 shadow-xl max-w-sm w-full p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 id="delete-review-dialog-title" className="text-base font-bold text-stone-900">
                  ¿Eliminar opinión?
                </h3>
                <p className="text-xs text-stone-500">
                  Esta acción no se puede deshacer.
                </p>
              </div>
            </div>

            <p className="text-xs text-stone-600 leading-relaxed">
              ¿Estás seguro de que deseas eliminar tu valoración sobre <strong>{reviewToDelete.productName}</strong>?
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setReviewToDelete(null)}
                disabled={isDeleting}
                className="px-3.5 py-2 rounded-xl border border-stone-200 text-xs font-semibold text-stone-700 hover:bg-stone-50 transition-colors cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold shadow-xs transition-colors disabled:opacity-50 cursor-pointer"
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
