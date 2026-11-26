'use client';

import { useContext, useEffect, useRef } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import { QueryClientContext } from '@tanstack/react-query';
import { env } from '@/core/config/env';
import { notify, notifyKeys } from '@/shared/utils/notify';
import { invalidateCustomerNotifications } from './useCustomerNotifications';

const CUSTOMER_ORDERS_QUEUE = '/user/queue/orders';
const MAX_DEDUP_EVENTS = 100;
const BASE_RECONNECT_MS = 3000;
const MAX_RECONNECT_MS = 30000;

interface OrderStatusChangedPayload {
  eventId?: string;
  orderId?: string;
  orderNumber?: string;
  previousStatus?: string;
  newStatus?: string;
}

const STATUS_LABELS_ES: Record<string, string> = {
  PENDING: 'pendiente de confirmación',
  CONFIRMED: 'confirmado por el comercio',
  PREPARING: 'en preparación',
  READY: 'listo para entrega o retiro',
  OUT_FOR_DELIVERY: 'en camino',
  DELIVERED: 'entregado',
  CANCELLED: 'cancelado',
};

export function getCustomerWebSocketUrl(apiUrl: string): string {
  const parsed = new URL(apiUrl);
  const protocol = parsed.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${parsed.host}/ws`;
}

/**
 * Conecta al canal privado STOMP `/user/queue/orders` cuando el cliente está
 * autenticado. Si la conexión WebSocket falla o no hay token, cae de forma
 * transparente al polling optimizado de React Query.
 */
export function useCustomerOrderRealtime(enabled: boolean) {
  const queryClient = useContext(QueryClientContext);
  const seenEventsRef = useRef<string[]>([]);

  useEffect(() => {
    if (!enabled || !queryClient || typeof window === 'undefined' || typeof WebSocket === 'undefined') {
      return;
    }

    let isDisposed = false;
    let reconnectAttempt = 0;

    const markEventSeen = (eventId?: string): boolean => {
      if (!eventId) return false;
      const cache = seenEventsRef.current;
      if (cache.includes(eventId)) return true;
      cache.push(eventId);
      if (cache.length > MAX_DEDUP_EVENTS) {
        cache.shift();
      }
      return false;
    };

    const handleOrderMessage = (frame: IMessage) => {
      try {
        const payload = JSON.parse(frame.body) as OrderStatusChangedPayload;
        if (markEventSeen(payload.eventId)) return;

        const orderNumber = payload.orderNumber;
        const newStatus = payload.newStatus;
        if (orderNumber && newStatus) {
          const statusLabel = STATUS_LABELS_ES[newStatus] ?? newStatus.toLowerCase();
          const toastId = notifyKeys.orderStatus(orderNumber);
          if (newStatus === 'DELIVERED' || newStatus === 'READY' || newStatus === 'CONFIRMED') {
            notify.success(`Pedido #${orderNumber}: ${statusLabel}.`, { id: toastId });
          } else if (newStatus === 'CANCELLED') {
            notify.warning(`Pedido #${orderNumber}: ${statusLabel}.`, { id: toastId });
          } else {
            notify.info(`Pedido #${orderNumber}: ${statusLabel}.`, { id: toastId });
          }
        }

        void queryClient.invalidateQueries({ queryKey: ['customer', 'orders'] });
        void invalidateCustomerNotifications(queryClient);
      } catch {
        // Ignorar frames malformados sin interrumpir la suscripción
      }
    };

    const client = new Client({
      brokerURL: getCustomerWebSocketUrl(env.apiUrl),
      reconnectDelay: BASE_RECONNECT_MS,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: async () => {
        try {
          const res = await fetch('/api/customer/ws-token', {
            method: 'GET',
            cache: 'no-store',
          });
          if (!res.ok) {
            void client.deactivate();
            return;
          }
          const data = (await res.json()) as { token?: string };
          if (!data.token || isDisposed) {
            void client.deactivate();
            return;
          }
          client.connectHeaders = {
            Authorization: `Bearer ${data.token}`,
          };
        } catch {
          reconnectAttempt += 1;
          client.reconnectDelay = Math.min(
            MAX_RECONNECT_MS,
            BASE_RECONNECT_MS * Math.pow(2, Math.min(reconnectAttempt, 4))
          );
        }
      },
      onConnect: () => {
        reconnectAttempt = 0;
        client.reconnectDelay = BASE_RECONNECT_MS;
        client.subscribe(CUSTOMER_ORDERS_QUEUE, handleOrderMessage);
      },
      onWebSocketClose: () => {
        reconnectAttempt += 1;
        client.reconnectDelay = Math.min(
          MAX_RECONNECT_MS,
          BASE_RECONNECT_MS * Math.pow(2, Math.min(reconnectAttempt, 4))
        );
      },
    });

    client.activate();

    return () => {
      isDisposed = true;
      void client.deactivate();
    };
  }, [enabled, queryClient]);
}
