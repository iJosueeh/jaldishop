'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { MessageSquarePlus, Pencil, ShieldCheck, Star, Trash2 } from 'lucide-react';
import { notify, notifyKeys } from '@/shared/utils/notify';
import { ProductReviewsSummary } from '../../types/storefront.types';
import { useReviewEligibility } from '@/features/customer/hooks/useReviewEligibility';
import { useCustomerReviews } from '@/features/customer/hooks/useCustomerReviews';
import { ReviewModal } from '@/features/customer/components/ReviewModal';
import { cn } from '@/shared/utils/cn';

interface ProductReviewsSectionProps {
  productId: string;
  productName: string;
  reviewsSummary?: ProductReviewsSummary | null;
  onReviewsChanged?: () => void;
}

type SortOrder = 'recent' | 'highest' | 'lowest';
const PAGE_SIZE = 5;

export function ProductReviewsSection({
  productId,
  productName,
  reviewsSummary,
  onReviewsChanged,
}: ProductReviewsSectionProps) {
  const pathname = usePathname();
  const [sortBy, setSortBy] = useState<SortOrder>('recent');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [modalOpen, setModalOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const eligibilityQuery = useReviewEligibility(productId);
  const { deleteReview, isDeleting } = useCustomerReviews();

  const eligibility = eligibilityQuery.data;
  const isUnauthenticated =
    eligibilityQuery.isError && eligibilityQuery.error?.message === 'No autenticado.';

  const reviews = reviewsSummary?.reviews ?? [];
  const totalReviews = reviewsSummary?.totalReviews ?? 0;
  const averageRating = reviewsSummary?.averageRating ?? 0;

  const distribution = useMemo(() => {
    const counts: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    for (const rev of reviews) {
      const r = Math.min(5, Math.max(1, Math.round(rev.rating)));
      counts[r] = (counts[r] || 0) + 1;
    }
    return [5, 4, 3, 2, 1].map((star) => ({
      star,
      count: counts[star] || 0,
      percent: totalReviews > 0 ? Math.round(((counts[star] || 0) / totalReviews) * 100) : 0,
    }));
  }, [reviews, totalReviews]);

  const sortedReviews = useMemo(() => {
    const copy = [...reviews];
    if (sortBy === 'highest') {
      copy.sort((a, b) => b.rating - a.rating);
    } else if (sortBy === 'lowest') {
      copy.sort((a, b) => a.rating - b.rating);
    } else {
      copy.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }
    return copy;
  }, [reviews, sortBy]);

  const visibleReviews = sortedReviews.slice(0, visibleCount);
  const hasMore = sortedReviews.length > visibleCount;

  const handleDeleteReview = async () => {
    try {
      await deleteReview(productId);
      setConfirmDelete(false);
      notify.success('Tu reseña ha sido eliminada.', {
        id: notifyKeys.review(productId),
      });
      onReviewsChanged?.();
    } catch (err) {
      notify.error(err instanceof Error ? err.message : 'No pudimos eliminar tu reseña.', {
        id: notifyKeys.review(productId),
      });
    }
  };

  return (
    <section
      aria-labelledby="product-reviews-heading"
      className="rounded-[12px] border border-stone-200/90 bg-white p-5 sm:p-6 shadow-2xs space-y-6"
    >
      {/* Header + Resumen */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 pb-4">
        <div>
          <h2 id="product-reviews-heading" className="font-display text-lg font-bold text-stone-950">
            Opiniones del producto
          </h2>
          <p className="text-xs text-stone-500 mt-0.5">
            Solo compradores con pedido completado pueden publicar 1 reseña por producto.
          </p>
        </div>
        {totalReviews > 0 && (
          <div className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs font-bold text-amber-800">
            <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-500" />
            <span>{averageRating.toFixed(1)}</span>
            <span className="font-normal text-amber-700">
              ({totalReviews} {totalReviews === 1 ? 'opinión' : 'opiniones'})
            </span>
          </div>
        )}
      </div>

      {/* Distribución 5 -> 1 */}
      {totalReviews > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-5 items-center rounded-xl bg-stone-50/80 border border-stone-200/70 p-4">
          <div className="sm:col-span-4 text-center sm:border-r sm:border-stone-200/80 sm:pr-4">
            <p className="font-display text-4xl font-black text-stone-950 tabular-nums">
              {averageRating.toFixed(1)}
            </p>
            <div className="flex items-center justify-center gap-0.5 my-1.5" aria-hidden="true">
              {[1, 2, 3, 4, 5].map((star) => (
                <Star
                  key={star}
                  className={cn(
                    'h-4 w-4',
                    star <= Math.round(averageRating)
                      ? 'fill-amber-400 text-amber-500'
                      : 'text-stone-300'
                  )}
                />
              ))}
            </div>
            <p className="text-xs text-stone-500">
              Basado en {totalReviews} {totalReviews === 1 ? 'compra verificada' : 'compras verificadas'}
            </p>
          </div>

          <div className="sm:col-span-8 space-y-1.5">
            {distribution.map((row) => (
              <div key={row.star} className="flex items-center gap-2.5 text-xs">
                <span className="w-10 font-semibold text-stone-700 tabular-nums">{row.star} ★</span>
                <div className="flex-1 h-2 rounded-full bg-stone-200 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-amber-400 transition-all duration-300"
                    style={{ width: `${row.percent}%` }}
                  />
                </div>
                <span className="w-8 text-right text-stone-500 tabular-nums">{row.count}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Bloque de acción del cliente conectado a eligibility */}
      <div className="rounded-xl border border-stone-200 bg-stone-50/60 p-4">
        {eligibilityQuery.isLoading ? (
          <p className="text-xs text-stone-500">Verificando tu elegibilidad para opinar...</p>
        ) : isUnauthenticated ? (
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="space-y-0.5">
              <p className="text-xs font-bold text-stone-800">¿Compraste este producto?</p>
              <p className="text-xs text-stone-600">
                Inicia sesión para verificar tu compra completada y dejar tu opinión.
              </p>
            </div>
            <Link
              href={`/login?redirect=${encodeURIComponent(pathname || '/')}`}
              className="inline-flex min-h-[40px] items-center justify-center rounded-full bg-[#005141] px-4 py-2 text-xs font-bold text-white hover:bg-[#00382d] transition-colors"
            >
              Iniciar sesión para opinar
            </Link>
          </div>
        ) : eligibility?.alreadyReviewed && eligibility.currentReview ? (
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-[#005141]">
                <ShieldCheck className="h-4 w-4" />
                <span>Tu opinión publicada (1 por producto)</span>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setModalOpen(true)}
                  className="inline-flex items-center gap-1 rounded-full border border-stone-200 bg-white px-3 py-1.5 text-xs font-semibold text-stone-700 hover:border-[#005141] hover:text-[#005141] cursor-pointer"
                >
                  <Pencil className="h-3.5 w-3.5" />
                  <span>Editar</span>
                </button>
                {!confirmDelete ? (
                  <button
                    type="button"
                    onClick={() => setConfirmDelete(true)}
                    className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-white px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 cursor-pointer"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    <span>Eliminar</span>
                  </button>
                ) : (
                  <div className="inline-flex items-center gap-1.5">
                    <button
                      type="button"
                      disabled={isDeleting}
                      onClick={handleDeleteReview}
                      className="rounded-full bg-rose-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-rose-700 disabled:opacity-50 cursor-pointer"
                    >
                      {isDeleting ? 'Eliminando...' : 'Confirmar'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmDelete(false)}
                      className="rounded-full border border-stone-200 bg-white px-3 py-1.5 text-xs font-semibold text-stone-600 cursor-pointer"
                    >
                      Cancelar
                    </button>
                  </div>
                )}
              </div>
            </div>

            <div className="rounded-lg border border-stone-200/80 bg-white p-3 space-y-1">
              <div className="flex items-center gap-0.5" aria-label={`Tu calificación: ${eligibility.currentReview.rating} de 5`}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <Star
                    key={star}
                    className={cn(
                      'h-3.5 w-3.5',
                      star <= eligibility.currentReview!.rating
                        ? 'fill-amber-400 text-amber-500'
                        : 'text-stone-300'
                    )}
                  />
                ))}
              </div>
              {eligibility.currentReview.comment && (
                <p className="text-xs sm:text-sm text-stone-700">{eligibility.currentReview.comment}</p>
              )}
            </div>
          </div>
        ) : eligibility?.eligible ? (
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="space-y-0.5">
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-800">
                <ShieldCheck className="h-4 w-4 text-emerald-600" />
                <span>Compra verificada habilitada</span>
              </div>
              <p className="text-xs text-stone-600">
                Ya completaste un pedido con este producto. Comparte tu experiencia.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="inline-flex min-h-[40px] items-center gap-1.5 rounded-full bg-[#005141] px-4 py-2 text-xs font-bold text-white hover:bg-[#00382d] transition-colors cursor-pointer"
            >
              <MessageSquarePlus className="h-4 w-4 text-[#36f4a4]" />
              <span>Escribir mi reseña</span>
            </button>
          </div>
        ) : eligibility?.hasInProgressOrder ? (
          <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-stone-700">
            <div className="flex items-start gap-2.5">
              <ShieldCheck className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
              <p>
                Tienes un <strong>pedido en curso</strong> con este producto. Podrás publicar tu reseña en cuanto sea{' '}
                <strong>entregado</strong>.
              </p>
            </div>
            <Link
              href="/mi-jaldishop/pedidos"
              className="text-xs font-semibold text-[#005141] hover:underline shrink-0"
            >
              Ver mis pedidos
            </Link>
          </div>
        ) : (
          <div className="flex items-start gap-2.5 text-xs text-stone-600">
            <ShieldCheck className="h-4 w-4 text-stone-400 shrink-0 mt-0.5" />
            <p>
              Solo puedes publicar <strong>1 reseña por producto</strong> una vez que tu pedido haya sido{' '}
              <strong>completado exitosamente</strong>.
            </p>
          </div>
        )}
      </div>

      {/* Ordenamiento y listado */}
      {sortedReviews.length > 0 ? (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-semibold text-stone-500">
              Mostrando {visibleReviews.length} de {sortedReviews.length}
            </span>
            <label className="inline-flex items-center gap-2 text-xs font-semibold text-stone-700">
              <span>Ordenar:</span>
              <select
                aria-label="Ordenar opiniones"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortOrder)}
                className="rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-xs font-medium text-stone-800 focus:border-[#005141] focus:outline-none"
              >
                <option value="recent">Más recientes</option>
                <option value="highest">Mayor calificación</option>
                <option value="lowest">Menor calificación</option>
              </select>
            </label>
          </div>

          <div className="space-y-3">
            {visibleReviews.map((review) => (
              <article
                key={review.id}
                className="rounded-xl border border-stone-100 bg-stone-50/70 p-4 space-y-1.5"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-bold text-stone-900">{review.customerName}</span>
                  <span className="text-[11px] text-stone-400">
                    {new Date(review.createdAt).toLocaleDateString('es-PE')}
                  </span>
                </div>
                <div className="flex items-center gap-0.5" aria-label={`Calificación ${review.rating} de 5`}>
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={cn(
                        'h-3.5 w-3.5',
                        star <= review.rating ? 'fill-amber-400 text-amber-500' : 'text-stone-300'
                      )}
                    />
                  ))}
                </div>
                {review.comment && <p className="text-xs sm:text-sm text-stone-700 leading-relaxed">{review.comment}</p>}
              </article>
            ))}
          </div>

          {hasMore && (
            <div className="pt-1 text-center">
              <button
                type="button"
                onClick={() => setVisibleCount((prev) => prev + PAGE_SIZE)}
                className="inline-flex min-h-[40px] items-center justify-center rounded-full border border-stone-200 bg-white px-5 py-2 text-xs font-bold text-stone-700 hover:border-[#005141] hover:text-[#005141] transition-colors cursor-pointer"
              >
                Mostrar más opiniones
              </button>
            </div>
          )}
        </div>
      ) : (
        <p className="text-sm text-stone-500">
          Aún no hay opiniones publicadas para este producto.
        </p>
      )}

      {/* Modal para crear o editar */}
      <ReviewModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        productId={productId}
        productName={productName}
        initialRating={eligibility?.currentReview?.rating ?? 5}
        initialComment={eligibility?.currentReview?.comment ?? ''}
        isEditing={Boolean(eligibility?.alreadyReviewed && eligibility.currentReview)}
        onSuccess={() => {
          onReviewsChanged?.();
        }}
      />
    </section>
  );
}
