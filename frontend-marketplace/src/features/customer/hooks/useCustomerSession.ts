import { useQuery, useQueryClient } from '@tanstack/react-query';
import { customerAuthService } from '@/features/auth/services/customerAuthService';
import { customerProfileService } from '../services/customerProfileService';
import { CUSTOMER_PROFILE_QUERY_KEY } from './useCustomerProfile';

export const CUSTOMER_SESSION_QUERY_KEY = ['customer-session'];

export function useCustomerSession() {
  const queryClient = useQueryClient();

  const sessionQuery = useQuery({
    queryKey: CUSTOMER_SESSION_QUERY_KEY,
    queryFn: customerAuthService.getSession,
    staleTime: 1000 * 60 * 2,
  });

  const isAuthenticated = Boolean(sessionQuery.data?.authenticated);

  const profileQuery = useQuery({
    queryKey: CUSTOMER_PROFILE_QUERY_KEY,
    queryFn: customerProfileService.getProfile,
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 5,
    retry: false,
  });

  const logout = async () => {
    try {
      await customerAuthService.logout();
    } finally {
      queryClient.setQueryData(CUSTOMER_SESSION_QUERY_KEY, { authenticated: false });
      queryClient.removeQueries({ queryKey: CUSTOMER_PROFILE_QUERY_KEY });
      window.location.href = '/';
    }
  };

  return {
    isAuthenticated,
    isLoading: sessionQuery.isLoading || (isAuthenticated && profileQuery.isLoading),
    user: profileQuery.data,
    logout,
    refetch: () => {
      sessionQuery.refetch();
      if (isAuthenticated) profileQuery.refetch();
    },
  };
}
