'use client';

import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { storeService } from '../services/storeService';
import { ProductItem, ProductReviewsSummary } from '../types/storefront.types';

export const STOCK_SYNC_CHANNEL = 'jaldishop-stock-sync';
export const ORDER_COMPLETED_EVENT = 'jaldishop:order-completed';

export function emitOrderCompletedSync(payload?: { storeSlug?: string; productId?: string }) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(ORDER_COMPLETED_EVENT, { detail: payload }));
  if (typeof BroadcastChannel !== 'undefined') {
    try {
      const channel = new BroadcastChannel(STOCK_SYNC_CHANNEL);
      channel.postMessage({ type: 'ORDER_COMPLETED', ...payload });
      channel.close();
    } catch {
      // BroadcastChannel is optional in restricted environments
    }
  }
}

interface UseProductLiveSyncParams {
  storeSlug: string;
  initialProduct: ProductItem;
  initialReviewsSummary?: ProductReviewsSummary | null;
}

export function useProductLiveSync({
  storeSlug,
  initialProduct,
  initialReviewsSummary = null,
}: UseProductLiveSyncParams) {
  const queryClient = useQueryClient();
  const productSlug = initialProduct.slug;

  const productQueryKey = ['product-live', storeSlug, productSlug ?? initialProduct.id] as const;
  const reviewsQueryKey = ['product-reviews-public', initialProduct.id] as const;

  const productQuery = useQuery<ProductItem | null>({
    queryKey: productQueryKey,
    queryFn: () =>
      productSlug
        ? storeService.getStoreProductBySlugLive(storeSlug, productSlug)
        : Promise.resolve(initialProduct),
    initialData: initialProduct,
    enabled: Boolean(storeSlug && productSlug),
    staleTime: 0,
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    retry: false,
  });

  const reviewsQuery = useQuery<ProductReviewsSummary | null>({
    queryKey: reviewsQueryKey,
    queryFn: () => storeService.getPublicProductReviewsLive(initialProduct.id),
    initialData: initialReviewsSummary,
    staleTime: 0,
    refetchOnWindowFocus: false,
    retry: false,
  });

  useEffect(() => {
    const syncNow = () => {
      void queryClient.invalidateQueries({ queryKey: productQueryKey });
      void queryClient.invalidateQueries({ queryKey: ['customer', 'reviews', 'eligibility', initialProduct.id] });
    };

    const handleCustomEvent = () => syncNow();
    window.addEventListener(ORDER_COMPLETED_EVENT, handleCustomEvent);

    let channel: BroadcastChannel | null = null;
    if (typeof BroadcastChannel !== 'undefined') {
      try {
        channel = new BroadcastChannel(STOCK_SYNC_CHANNEL);
        channel.onmessage = (event) => {
          if (event.data?.type === 'ORDER_COMPLETED') {
            syncNow();
          }
        };
      } catch {
        channel = null;
      }
    }

    return () => {
      window.removeEventListener(ORDER_COMPLETED_EVENT, handleCustomEvent);
      channel?.close();
    };
  }, [initialProduct.id, productQueryKey, queryClient]);

  const refreshReviews = () => {
    void queryClient.invalidateQueries({ queryKey: reviewsQueryKey });
    void queryClient.invalidateQueries({ queryKey: ['customer', 'reviews', 'eligibility', initialProduct.id] });
  };

  return {
    product: productQuery.data ?? initialProduct,
    isSyncingProduct: productQuery.isFetching,
    reviewsSummary: reviewsQuery.data ?? initialReviewsSummary,
    refreshReviews,
  };
}
