'use client';

import { QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { customerNotificationService } from '../services/customerNotificationService';
import { CustomerNotification, UnreadNotificationsCount } from '../types/customer.types';

export const CUSTOMER_NOTIFICATIONS_QUERY_KEY = ['customer', 'notifications'] as const;
export const UNREAD_NOTIFICATIONS_COUNT_KEY = ['customer', 'notifications', 'unread-count'] as const;

export function invalidateCustomerNotifications(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: CUSTOMER_NOTIFICATIONS_QUERY_KEY }),
    queryClient.invalidateQueries({ queryKey: UNREAD_NOTIFICATIONS_COUNT_KEY }),
  ]);
}

export function useUnreadNotificationsCount(enabled = true) {
  const query = useQuery({
    queryKey: UNREAD_NOTIFICATIONS_COUNT_KEY,
    queryFn: customerNotificationService.getUnreadCount,
    enabled,
    staleTime: 30_000,
    refetchInterval: enabled ? 60_000 : false,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: enabled,
    retry: 1,
  });

  return {
    unreadCount: query.data?.unreadCount ?? 0,
    isLoading: query.isLoading,
    reload: query.refetch,
  };
}

export function useCustomerNotifications(page = 0, size = 20) {
  const queryClient = useQueryClient();
  const listKey = [...CUSTOMER_NOTIFICATIONS_QUERY_KEY, page, size];

  const query = useQuery({
    queryKey: listKey,
    queryFn: () => customerNotificationService.listNotifications({ page, size }),
    staleTime: 30_000,
    refetchOnWindowFocus: true,
    retry: 1,
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id: string) => customerNotificationService.markAsRead(id),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: listKey });
      await queryClient.cancelQueries({ queryKey: UNREAD_NOTIFICATIONS_COUNT_KEY });

      const previousList = queryClient.getQueryData<CustomerNotification[]>(listKey);
      const previousCount = queryClient.getQueryData<UnreadNotificationsCount>(
        UNREAD_NOTIFICATIONS_COUNT_KEY
      );

      let wasUnread = false;
      if (previousList) {
        queryClient.setQueryData<CustomerNotification[]>(
          listKey,
          previousList.map((item) => {
            if (item.id === id && item.status === 'UNREAD') {
              wasUnread = true;
              return { ...item, status: 'READ', readAt: new Date().toISOString() };
            }
            return item;
          })
        );
      }

      if (wasUnread && previousCount) {
        queryClient.setQueryData<UnreadNotificationsCount>(UNREAD_NOTIFICATIONS_COUNT_KEY, {
          unreadCount: Math.max(0, previousCount.unreadCount - 1),
        });
      }

      return { previousList, previousCount };
    },
    onError: (_err, _id, context) => {
      if (context?.previousList) {
        queryClient.setQueryData(listKey, context.previousList);
      }
      if (context?.previousCount) {
        queryClient.setQueryData(UNREAD_NOTIFICATIONS_COUNT_KEY, context.previousCount);
      }
    },
    onSettled: () => {
      void invalidateCustomerNotifications(queryClient);
    },
  });

  return {
    notifications: query.data ?? [],
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error:
      query.error instanceof Error
        ? query.error.message
        : query.error
          ? 'Error al cargar notificaciones'
          : null,
    reload: query.refetch,
    markAsRead: markAsReadMutation.mutateAsync,
    isMarkingAsRead: markAsReadMutation.isPending,
  };
}
