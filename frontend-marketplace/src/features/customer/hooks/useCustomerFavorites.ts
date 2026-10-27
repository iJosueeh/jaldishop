'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { customerFavoriteService } from '../services/customerFavoriteService';
import { FavoriteProductSummary } from '../types/customer.types';

export const CUSTOMER_FAVORITES_QUERY_KEY = ['customer-favorites'];

export function useCustomerFavorites() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: CUSTOMER_FAVORITES_QUERY_KEY,
    queryFn: customerFavoriteService.getFavoriteProducts,
    staleTime: 1000 * 60 * 2, // 2 minutes fresh cache
    gcTime: 1000 * 60 * 10,
    retry: 1,
  });

  const favorites = useMemo(() => query.data ?? [], [query.data]);

  const removeMutation = useMutation({
    mutationFn: (productId: string) => customerFavoriteService.removeFavorite(productId),
    onMutate: async (productId: string) => {
      await queryClient.cancelQueries({ queryKey: CUSTOMER_FAVORITES_QUERY_KEY });
      const previous = queryClient.getQueryData<FavoriteProductSummary[]>(CUSTOMER_FAVORITES_QUERY_KEY);
      if (previous) {
        queryClient.setQueryData<FavoriteProductSummary[]>(
          CUSTOMER_FAVORITES_QUERY_KEY,
          previous.filter((f) => f.productId !== productId)
        );
      }
      return { previous };
    },
    onError: (_err, _productId, context) => {
      if (context?.previous) {
        queryClient.setQueryData(CUSTOMER_FAVORITES_QUERY_KEY, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_FAVORITES_QUERY_KEY });
    },
  });

  const reload = useCallback(() => {
    query.refetch();
  }, [query]);

  return {
    favorites,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error instanceof Error ? query.error.message : query.error ? 'Error al cargar favoritos' : null,
    removeFavorite: removeMutation.mutate,
    isRemoving: removeMutation.isPending,
    reload,
  };
}
