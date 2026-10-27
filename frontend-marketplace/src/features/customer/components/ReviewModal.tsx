'use client';

import React, { useState, useEffect } from 'react';
import { Star, X, ShieldCheck, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useCustomerReviews } from '../hooks/useCustomerReviews';
import { cn } from '@/shared/utils/cn';

interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  productId: string;
  productName: string;
  initialRating?: number;
  initialComment?: string | null;
  isEditing?: boolean;
  onSuccess?: () => void;
}

const RATING_LABELS: Record<number, string> = {
  1: 'Muy insatisfecho',
  2: 'Insatisfecho',
  3: 'Aceptable',
  4: 'Bueno',
  5: 'Excelente',
};

export function ReviewModal({
  isOpen,
  onClose,
  productId,
  productName,
  initialRating = 5,
  initialComment = '',
  isEditing = false,
  onSuccess,
}: ReviewModalProps) {
  const [rating, setRating] = useState<number>(initialRating);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [comment, setComment] = useState<string>(initialComment || '');
  const [submitting, setSubmitting] = useState(false);

  const { createReview, updateReview } = useCustomerReviews();

  useEffect(() => {
    if (isOpen) {
      setRating(initialRating || 5);
      setComment(initialComment || '');
      setHoverRating(null);
      setSubmitting(false);
    }
  }, [isOpen, initialRating, initialComment]);

  if (!isOpen) return null;

  const currentDisplayRating = hoverRating ?? rating;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating < 1 || rating > 5) {
      toast.error('Por favor selecciona una calificación entre 1 y 5 estrellas.');
      return;
    }

    try {
      setSubmitting(true);
      if (isEditing) {
        await updateReview({
          productId,
          payload: { rating, comment: comment.trim() || null },
        });
        toast.success('Tu opinión ha sido actualizada.');
      } else {
        await createReview({
          productId,
          payload: { rating, comment: comment.trim() || null },
        });
        toast.success('¡Gracias por calificar tu producto!');
      }

      onSuccess?.();
      onClose();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'No se pudo guardar la calificación.';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="review-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/40 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="bg-white rounded-2xl border border-stone-200/90 shadow-xl max-w-lg w-full overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4.5 border-b border-stone-100 flex items-center justify-between">
          <div className="space-y-0.5">
            <h2 id="review-modal-title" className="text-base font-bold text-stone-900">
              {isEditing ? 'Editar opinión' : 'Calificar producto'}
            </h2>
            <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-700">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              <span>Compra verificada</span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar ventana"
            className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Product context */}
          <div className="p-3 bg-stone-50 rounded-xl border border-stone-100 text-xs text-stone-600">
            <span className="text-stone-400 font-normal">Producto: </span>
            <span className="font-semibold text-stone-800">{productName}</span>
          </div>

          {/* Star selector */}
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-stone-600">
              Tu valoración general
            </label>
            <div className="flex items-center gap-1.5" role="group" aria-label="Seleccionar estrellas">
              {[1, 2, 3, 4, 5].map((starValue) => {
                const isFilled = starValue <= currentDisplayRating;
                return (
                  <button
                    key={starValue}
                    type="button"
                    onClick={() => setRating(starValue)}
                    onMouseEnter={() => setHoverRating(starValue)}
                    onMouseLeave={() => setHoverRating(null)}
                    aria-label={`${starValue} de 5 estrellas`}
                    className="p-1 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-[#005141] rounded-sm transition-transform active:scale-95"
                  >
                    <Star
                      className={cn(
                        'w-7 h-7 transition-colors',
                        isFilled
                          ? 'fill-amber-400 text-amber-400'
                          : 'fill-stone-100 text-stone-300 hover:text-stone-400'
                      )}
                    />
                  </button>
                );
              })}
              <span className="text-xs font-medium text-stone-500 ml-2">
                {RATING_LABELS[currentDisplayRating] || ''}
              </span>
            </div>
          </div>

          {/* Comment textarea */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label htmlFor="review-comment" className="text-xs font-bold uppercase tracking-wider text-stone-600">
                Tu opinión escrita <span className="font-normal normal-case text-stone-400">(opcional)</span>
              </label>
              <span className="text-[11px] text-stone-400">
                {comment.length}/1000
              </span>
            </div>
            <textarea
              id="review-comment"
              rows={4}
              maxLength={1000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="¿Qué tal fue la calidad, presentación o utilidad de este producto? Tu opinión ayuda a otros compradores y fortalece a este comercio local."
              className="w-full text-sm rounded-xl border border-stone-200 p-3 text-stone-800 placeholder-stone-400 focus:outline-hidden focus:border-[#005141] focus:ring-1 focus:ring-[#005141] transition-all resize-none"
            />
          </div>

          {/* Actions */}
          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2 text-xs font-semibold text-stone-600 hover:text-stone-900 hover:bg-stone-100 rounded-lg transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#005141] text-white text-xs font-semibold hover:bg-[#004033] active:scale-[0.99] transition-all shadow-xs disabled:opacity-50"
            >
              {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              <span>{isEditing ? 'Guardar cambios' : 'Publicar opinión'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
