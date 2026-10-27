'use client';

import { useQuery } from '@tanstack/react-query';
import { checkReviewEligibility } from '../services/customerReviewService';
import type { ReviewEligibility } from '../types/customer.types';

export function useReviewEligibility(productId?: string | null) {
  return useQuery<ReviewEligibility, Error>({
    queryKey: ['customer', 'reviews', 'eligibility', productId],
    queryFn: () => {
      if (!productId) {
        throw new Error('Identificador de producto no provisto');
      }
      return checkReviewEligibility(productId);
    },
    enabled: Boolean(productId),
    staleTime: 1000 * 60,
    gcTime: 1000 * 60 * 5,
    refetchOnWindowFocus: false,
  });
}
