'use client';

import { useState } from 'react';
import { MessageSquareQuote } from 'lucide-react';
import { useReviewEligibility } from '../hooks/useReviewEligibility';
import { ReviewModal } from './ReviewModal';

export function OrderReviewAction({ productId, productName }: { productId?: string | null; productName: string }) {
  const [open, setOpen] = useState(false);
  const { data, isLoading } = useReviewEligibility(productId);
  if (!productId || isLoading || !data?.eligible) return null;
  return <><button type="button" onClick={() => setOpen(true)} className="mt-2 inline-flex items-center gap-1.5 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 text-xs font-semibold text-emerald-800 transition hover:bg-emerald-100"><MessageSquareQuote className="h-3.5 w-3.5" />{data.alreadyReviewed ? 'Editar reseña' : 'Calificar producto'}</button><ReviewModal isOpen={open} onClose={() => setOpen(false)} productId={productId} productName={productName} isEditing={data.alreadyReviewed} initialRating={data.currentReview?.rating} initialComment={data.currentReview?.comment} /></>;
}
