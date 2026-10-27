'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { customerAddressService } from '../services/customerAddressService';
import {
  CustomerAddress,
  CreateCustomerAddressPayload,
  UpdateCustomerAddressPayload,
} from '../types/customer.types';

export const CUSTOMER_ADDRESSES_QUERY_KEY = ['customer-addresses'];

export function useCustomerAddresses() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: CUSTOMER_ADDRESSES_QUERY_KEY,
    queryFn: customerAddressService.getAddresses,
    staleTime: 1000 * 60 * 2, // 2 minutes fresh cache
    gcTime: 1000 * 60 * 10,
    retry: 1,
  });

  const addresses: CustomerAddress[] = useMemo(
    () => query.data?.addresses ?? [],
    [query.data]
  );

  const total = addresses.length;
  const canAddAddress = total < 5;
  const defaultAddress = useMemo(
    () => addresses.find((a) => a.isDefault) ?? null,
    [addresses]
  );

  const createMutation = useMutation({
    mutationFn: (payload: CreateCustomerAddressPayload) =>
      customerAddressService.createAddress(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_ADDRESSES_QUERY_KEY });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string;
      payload: UpdateCustomerAddressPayload;
    }) => customerAddressService.updateAddress(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_ADDRESSES_QUERY_KEY });
    },
  });

  const setDefaultMutation = useMutation({
    mutationFn: (id: string) => customerAddressService.setDefaultAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_ADDRESSES_QUERY_KEY });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => customerAddressService.deleteAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CUSTOMER_ADDRESSES_QUERY_KEY });
    },
  });

  const reload = useCallback(() => {
    query.refetch();
  }, [query]);

  return {
    addresses,
    total,
    canAddAddress,
    defaultAddress,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error:
      query.error instanceof Error
        ? query.error.message
        : query.error
        ? 'Error al cargar direcciones'
        : null,
    createAddress: createMutation.mutateAsync,
    isCreating: createMutation.isPending,
    updateAddress: updateMutation.mutateAsync,
    isUpdating: updateMutation.isPending,
    setDefaultAddress: setDefaultMutation.mutateAsync,
    isSettingDefault: setDefaultMutation.isPending,
    deleteAddress: deleteMutation.mutateAsync,
    isDeleting: deleteMutation.isPending,
    reload,
  };
}
