import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { customerProfileService } from '../services/customerProfileService';
import { UpdateProfilePayload } from '../types/customer.types';

export const CUSTOMER_PROFILE_QUERY_KEY = ['customer-profile'];

export function useCustomerProfile(enabled: boolean = true) {
  return useQuery({
    queryKey: CUSTOMER_PROFILE_QUERY_KEY,
    queryFn: customerProfileService.getProfile,
    enabled,
    staleTime: 1000 * 60 * 5, // 5 minutes
    retry: (failureCount, error: unknown) => {
      const status = typeof error === 'object' && error !== null && 'status' in error ? (error as { status: number }).status : undefined;
      if (status === 401 || status === 403) return false;
      return failureCount < 2;
    },
  });
}

export function useUpdateCustomerProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateProfilePayload) => customerProfileService.updateProfile(data),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(CUSTOMER_PROFILE_QUERY_KEY, updatedProfile);
    },
  });
}
