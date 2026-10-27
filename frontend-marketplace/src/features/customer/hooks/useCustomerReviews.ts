'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getCustomerReviews,
  createReview,
  updateReview,
} from '../services/customerReviewService';
import type {
  CustomerReview,
  CreateReviewPayload,
  UpdateReviewPayload,
} from '../types/customer.types';

export const CUSTOMER_REVIEWS_KEY = ['customer', 'reviews'];

export function useCustomerReviews() {
  const queryClient = useQueryClient();

  const query = useQuery<CustomerReview[], Error>({
    queryKey: CUSTOMER_REVIEWS_KEY,
    queryFn: getCustomerReviews,
    staleTime: 1000 * 60 * 2,
    gcTime: 1000 * 60 * 10,
    refetchOnWindowFocus: false,
  });

  const createMutation = useMutation({
    mutationFn: ({
      productId,
      payload,
    }: {
      productId: string;
      payload: CreateReviewPayload;
    }) => createReview(productId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_REVIEWS_KEY });
      queryClient.invalidateQueries({
        queryKey: ['customer', 'reviews', 'eligibility', variables.productId],
      });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      productId,
      payload,
    }: {
      productId: string;
      payload: UpdateReviewPayload;
    }) => updateReview(productId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_REVIEWS_KEY });
      queryClient.invalidateQueries({
        queryKey: ['customer', 'reviews', 'eligibility', variables.productId],
      });
    },
  });

  return {
    reviews: query.data ?? [],
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
    createReview: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    updateReview: updateMutation.mutateAsync,
    isUpdating: updateMutation.isPending,
  };
}
