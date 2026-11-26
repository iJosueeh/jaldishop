'use client';

import React, { useMemo, useState } from 'react';
import {
  Bell,
  Package,
  ShoppingBag,
  AlertTriangle,
  Info,
  Check,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Search,
  RefreshCw,
} from 'lucide-react';
import { useCustomerNotifications } from '../hooks/useCustomerNotifications';
import { CustomerNotification, NotificationType } from '../types/customer.types';
import { AccountPageHeader } from './AccountPageHeader';
import { AccountEmptyState } from './AccountEmptyState';
import { Button } from '@/shared/components/ui/Button';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { cn } from '@/shared/utils/cn';

type NotificationFilter = 'ALL' | 'UNREAD';
type NotificationTypeFilter = 'ALL' | 'ORDERS' | 'SYSTEM';

const PAGE_SIZE = 20;

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'NEW_ORDER':
      return ShoppingBag;
    case 'ORDER_STATUS_CHANGED':
      return Package;
    case 'LOW_STOCK':
      return AlertTriangle;
    case 'SYSTEM':
    default:
      return Info;
  }
}

function formatNotificationDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function NotificationsView() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<NotificationFilter>('ALL');
  const [typeFilter, setTypeFilter] = useState<NotificationTypeFilter>('ALL');
  const [search, setSearch] = useState('');
  const [liveStatusMessage, setLiveStatusMessage] = useState('');

  const {
    notifications,
    isLoading,
    isFetching,
    error,
    reload,
    markAsRead,
    isMarkingAsRead,
  } = useCustomerNotifications(page, PAGE_SIZE);

  const visibleNotifications = useMemo(() => {
    const query = search.trim().toLowerCase();
    return notifications.filter((n) => {
      if (filter === 'UNREAD' && n.status !== 'UNREAD') return false;
      if (
        typeFilter === 'ORDERS' &&
        n.type !== 'NEW_ORDER' &&
        n.type !== 'ORDER_STATUS_CHANGED'
      ) {
        return false;
      }
      if (typeFilter === 'SYSTEM' && n.type !== 'SYSTEM' && n.type !== 'LOW_STOCK') {
        return false;
      }
      if (!query) return true;
      return (
        n.title.toLowerCase().includes(query) ||
        n.message.toLowerCase().includes(query)
      );
    });
  }, [notifications, filter, typeFilter, search]);

  const unreadInPage = notifications.filter((n) => n.status === 'UNREAD').length;

  const handleMarkAsRead = async (notification: CustomerNotification) => {
    if (notification.status === 'READ') return;
    await markAsRead(notification.id);
    setLiveStatusMessage(`Notificación "${notification.title}" marcada como leída.`);
  };

  const resetFilters = () => {
    setFilter('ALL');
    setTypeFilter('ALL');
    setSearch('');
  };

  if (isLoading && notifications.length === 0) {
    return (
      <div className="space-y-4 w-full" role="status" aria-busy="true" aria-label="Cargando notificaciones">
        <div className="pb-5 border-b border-[#e7e0d6] space-y-2">
          <Skeleton className="h-7 w-48 bg-[#f0eae0] rounded-lg" />
          <Skeleton className="h-4 w-72 bg-[#f0eae0] rounded-md" />
        </div>
        {Array.from({ length: 4 }).map((_, idx) => (
          <Skeleton key={idx} className="h-24 w-full bg-[#f0eae0] rounded-xl" />
        ))}
      </div>
    );
  }

  if (error && notifications.length === 0) {
    return (
      <div
        role="alert"
        className="rounded-xl border border-rose-200 bg-rose-50/70 p-8 text-center space-y-3"
      >
        <AlertCircle className="w-8 h-8 text-rose-600 mx-auto" />
        <h2 className="text-base font-bold text-rose-950">
          No pudimos cargar tus notificaciones
        </h2>
        <p className="text-sm text-rose-800 max-w-md mx-auto">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()} className="min-h-[44px]">
          Reintentar
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-5 w-full">
      <p className="sr-only" role="status" aria-live="polite" aria-atomic="true">
        {liveStatusMessage}
      </p>
      <AccountPageHeader
        title="Notificaciones"
        description="Avisos sobre el estado de tus pedidos y novedades de tu cuenta."
        actions={
          <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Filtrar y actualizar notificaciones">
            <button
              type="button"
              onClick={() => void reload()}
              disabled={isFetching}
              aria-label="Actualizar notificaciones"
              className="min-h-[44px] inline-flex items-center gap-1.5 px-3.5 rounded-lg text-xs font-semibold border border-[#e7e0d6] bg-white text-[#1c1917] hover:bg-[#f7f3ec] transition-colors duration-150 cursor-pointer disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
            >
              <RefreshCw className={cn('w-3.5 h-3.5', isFetching && 'animate-spin')} aria-hidden="true" />
              <span>{isFetching ? 'Actualizando' : 'Actualizar'}</span>
            </button>
            <button
              type="button"
              onClick={() => setFilter('ALL')}
              className={cn(
                'min-h-[44px] px-4 rounded-lg text-xs font-semibold border transition-colors duration-150 cursor-pointer tabular-nums focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                filter === 'ALL'
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-white text-[#1c1917] border-[#e7e0d6] hover:bg-[#f7f3ec]'
              )}
            >
              Todas ({notifications.length})
            </button>
            <button
              type="button"
              onClick={() => setFilter('UNREAD')}
              className={cn(
                'min-h-[44px] px-4 rounded-lg text-xs font-semibold border transition-colors duration-150 cursor-pointer tabular-nums focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                filter === 'UNREAD'
                  ? 'bg-[#072720] text-white border-[#072720]'
                  : 'bg-white text-[#1c1917] border-[#e7e0d6] hover:bg-[#f7f3ec]'
              )}
            >
              No leídas ({unreadInPage})
            </button>
          </div>
        }
      />

      {notifications.length > 0 && (
        <div className="rounded-xl border border-[#e7e0d6] bg-white p-3.5 sm:p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search
              className="w-4 h-4 text-[#78716c] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              aria-hidden="true"
            />
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Buscar notificaciones"
              placeholder="Buscar avisos en esta página..."
              className="w-full min-h-[44px] pl-9 pr-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2]/50 focus:bg-white text-sm text-[#1c1917] placeholder-[#78716c] focus:outline-none focus:border-[#005141] focus:ring-1 focus:ring-[#005141]"
            />
          </div>

          <div
            role="group"
            aria-label="Filtrar por tipo de notificación"
            className="flex flex-wrap items-center gap-1.5"
          >
            {(
              [
                { key: 'ALL', label: 'Todos los tipos' },
                { key: 'ORDERS', label: 'Pedidos' },
                { key: 'SYSTEM', label: 'Sistema' },
              ] as const
            ).map((opt) => {
              const isActive = typeFilter === opt.key;
              return (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => setTypeFilter(opt.key)}
                  className={cn(
                    'min-h-[44px] px-3.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                    isActive
                      ? 'bg-[#072720] text-white border-[#072720]'
                      : 'bg-[#faf7f2] text-[#1c1917] border-[#e7e0d6] hover:bg-[#f0eae0]'
                  )}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {notifications.length === 0 || (visibleNotifications.length === 0 && !search && typeFilter === 'ALL') ? (
        <AccountEmptyState
          icon={<Bell className="w-6 h-6" />}
          title={
            filter === 'UNREAD'
              ? 'No tienes notificaciones sin leer'
              : 'Aún no tienes notificaciones'
          }
          description="Cuando realices un pedido o cambie su estado, recibirás las actualizaciones aquí."
        />
      ) : visibleNotifications.length === 0 ? (
        <AccountEmptyState
          icon={<Search className="w-6 h-6" />}
          title="No hay notificaciones con esos filtros"
          description="Prueba buscando otro texto o limpia los filtros para ver todos los avisos."
          action={
            <button
              type="button"
              onClick={resetFilters}
              className="min-h-[44px] px-5 rounded-full bg-[#005141] text-white text-xs font-semibold hover:bg-[#003d31] transition-colors cursor-pointer"
            >
              Limpiar filtros
            </button>
          }
        />
      ) : (
        <ul
          role="list"
          className="rounded-xl border border-[#e7e0d6] bg-white divide-y divide-[#e7e0d6]/70 overflow-hidden"
        >
          {visibleNotifications.map((item) => {
            const Icon = getNotificationIcon(item.type);
            const isUnread = item.status === 'UNREAD';

            return (
              <li
                key={item.id}
                className={cn(
                  'p-4 sm:p-5 transition-colors duration-150 flex flex-col sm:flex-row sm:items-center justify-between gap-4',
                  isUnread ? 'bg-[#f0fdfa]/60' : 'bg-white'
                )}
              >
                <div className="flex items-start gap-3.5 min-w-0">
                  <div
                    className={cn(
                      'w-10 h-10 rounded-lg flex items-center justify-center shrink-0 mt-0.5',
                      isUnread
                        ? 'bg-[#072720] text-[#36f4a4]'
                        : 'bg-[#f7f3ec] text-[#57534e]'
                    )}
                  >
                    <Icon className="w-5 h-5" />
                  </div>

                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-sm font-bold text-[#1c1917]">{item.title}</h2>
                      {isUnread && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#005141] text-white">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#36f4a4]" aria-hidden="true" />
                          Nueva
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-[#57534e] leading-relaxed">{item.message}</p>
                    <p className="text-xs text-[#78716c] tabular-nums">
                      {formatNotificationDate(item.createdAt)}
                    </p>
                  </div>
                </div>

                {isUnread && (
                  <button
                    type="button"
                    disabled={isMarkingAsRead}
                    onClick={() => handleMarkAsRead(item)}
                    className="min-h-[44px] inline-flex items-center justify-center gap-1.5 px-4 rounded-lg border border-[#ccfbf1] bg-white hover:bg-[#f0fdfa] text-xs font-semibold text-[#005141] transition-colors duration-150 shrink-0 cursor-pointer disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
                  >
                    <Check className="w-4 h-4" />
                    <span>Marcar como leída</span>
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {(page > 0 || notifications.length === PAGE_SIZE) && (
        <div className="flex items-center justify-between pt-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            leftIcon={<ChevronLeft className="w-4 h-4" />}
            className="min-h-[44px]"
          >
            Anterior
          </Button>
          <span className="text-xs font-semibold text-[#57534e] tabular-nums">
            Página {page + 1}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={notifications.length < PAGE_SIZE}
            onClick={() => setPage((p) => p + 1)}
            rightIcon={<ChevronRight className="w-4 h-4" />}
            className="min-h-[44px]"
          >
            Siguiente
          </Button>
        </div>
      )}
    </div>
  );
}
