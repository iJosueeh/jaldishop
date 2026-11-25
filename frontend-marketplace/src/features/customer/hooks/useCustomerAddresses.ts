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
  const profileQuery = useQuery<{ id?: string } | null>({
    queryKey: ['customer-profile'],
    queryFn: () => null,
    enabled: false,
  });
  const scopedKey = useMemo(
    () => [...CUSTOMER_ADDRESSES_QUERY_KEY, profileQuery.data?.id ?? 'active'],
    [profileQuery.data?.id]
  );

  const query = useQuery({
    queryKey: scopedKey,
    queryFn: customerAddressService.getAddresses,
    staleTime: 0,
    gcTime: 1000 * 60 * 5,
    refetchOnMount: 'always',
    retry: 1,
  });

  const addresses: CustomerAddress[] = useMemo(
    () => query.data?.addresses ?? [],
    [query.data]
  );

  const total = addresses.length;
  const maxAllowed = query.data?.maxAllowed ?? 5;
  const canAddAddress = total < maxAllowed;
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
    maxAllowed,
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
