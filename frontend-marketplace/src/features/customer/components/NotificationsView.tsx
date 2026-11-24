'use client';

import React, { useState } from 'react';
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
} from 'lucide-react';
import { useCustomerNotifications } from '../hooks/useCustomerNotifications';
import { CustomerNotification, NotificationType } from '../types/customer.types';
import { Button } from '@/shared/components/ui/Button';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { cn } from '@/shared/utils/cn';

type NotificationFilter = 'ALL' | 'UNREAD';

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

  const {
    notifications,
    isLoading,
    error,
    reload,
    markAsRead,
    isMarkingAsRead,
  } = useCustomerNotifications(page, PAGE_SIZE);

  const visibleNotifications =
    filter === 'UNREAD'
      ? notifications.filter((n) => n.status === 'UNREAD')
      : notifications;

  const unreadInPage = notifications.filter((n) => n.status === 'UNREAD').length;

  const handleMarkAsRead = async (notification: CustomerNotification) => {
    if (notification.status === 'READ') return;
    await markAsRead(notification.id);
  };

  if (isLoading && notifications.length === 0) {
    return (
      <div className="space-y-4 w-full" role="status" aria-busy="true" aria-label="Cargando notificaciones">
        <div className="pb-5 border-b border-stone-200/80 space-y-2">
          <Skeleton className="h-7 w-48 bg-stone-200 rounded-lg" />
          <Skeleton className="h-4 w-72 bg-stone-200 rounded-md" />
        </div>
        {Array.from({ length: 4 }).map((_, idx) => (
          <Skeleton key={idx} className="h-24 w-full bg-stone-200 rounded-2xl" />
        ))}
      </div>
    );
  }

  if (error && notifications.length === 0) {
    return (
      <div
        role="alert"
        className="rounded-2xl border border-rose-200 bg-rose-50 p-8 text-center space-y-3"
      >
        <AlertCircle className="w-8 h-8 text-rose-600 mx-auto" />
        <h3 className="text-base font-bold text-rose-900">
          No pudimos cargar tus notificaciones
        </h3>
        <p className="text-sm text-rose-700 max-w-md mx-auto">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()}>
          Reintentar
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 w-full">
      {/* Header & Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-stone-200/80">
        <div>
          <h2 className="font-display text-2xl font-bold text-stone-900 tracking-tight">
            Notificaciones
          </h2>
          <p className="text-xs sm:text-sm text-stone-600 mt-0.5 leading-relaxed">
            Avisos sobre el estado de tus pedidos y novedades de tu cuenta.
          </p>
        </div>

        <div className="flex items-center gap-2" role="group" aria-label="Filtrar notificaciones">
          <button
            type="button"
            onClick={() => setFilter('ALL')}
            className={cn(
              'min-h-[44px] px-4 rounded-xl text-xs font-semibold border transition-colors duration-150 cursor-pointer',
              filter === 'ALL'
                ? 'bg-[#072720] text-white border-[#072720]'
                : 'bg-white text-stone-700 border-stone-200 hover:bg-stone-50'
            )}
          >
            Todas ({notifications.length})
          </button>
          <button
            type="button"
            onClick={() => setFilter('UNREAD')}
            className={cn(
              'min-h-[44px] px-4 rounded-xl text-xs font-semibold border transition-colors duration-150 cursor-pointer',
              filter === 'UNREAD'
                ? 'bg-[#072720] text-white border-[#072720]'
                : 'bg-white text-stone-700 border-stone-200 hover:bg-stone-50'
            )}
          >
            No leídas ({unreadInPage})
          </button>
        </div>
      </div>

      {/* Notifications List or Empty State */}
      {visibleNotifications.length === 0 ? (
        <div
          role="status"
          className="rounded-2xl border border-dashed border-stone-300 bg-white p-10 text-center space-y-3"
        >
          <div className="w-12 h-12 rounded-2xl bg-stone-100 text-stone-500 flex items-center justify-center mx-auto">
            <Bell className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-stone-900">
            {filter === 'UNREAD'
              ? 'No tienes notificaciones sin leer'
              : 'Aún no tienes notificaciones'}
          </h3>
          <p className="text-sm text-stone-600 max-w-md mx-auto">
            Cuando realices un pedido o cambie su estado, recibirás las actualizaciones aquí.
          </p>
        </div>
      ) : (
        <ul role="list" className="space-y-3">
          {visibleNotifications.map((item) => {
            const Icon = getNotificationIcon(item.type);
            const isUnread = item.status === 'UNREAD';

            return (
              <li
                key={item.id}
                className={cn(
                  'rounded-2xl border p-4 sm:p-5 transition-colors duration-150 flex flex-col sm:flex-row sm:items-center justify-between gap-4',
                  isUnread
                    ? 'bg-emerald-50/50 border-emerald-200 shadow-2xs'
                    : 'bg-white border-stone-200/90'
                )}
              >
                <div className="flex items-start gap-3.5 min-w-0">
                  <div
                    className={cn(
                      'w-10 h-10 rounded-xl flex items-center justify-center shrink-0 mt-0.5',
                      isUnread
                        ? 'bg-[#072720] text-[#36f4a4]'
                        : 'bg-stone-100 text-stone-500'
                    )}
                  >
                    <Icon className="w-5 h-5" />
                  </div>

                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-sm font-bold text-stone-900">{item.title}</h3>
                      {isUnread && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-[#005141] text-white">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#36f4a4]" aria-hidden="true" />
                          Nueva
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-stone-700 leading-relaxed">{item.message}</p>
                    <p className="text-xs text-stone-500">
                      {formatNotificationDate(item.createdAt)}
                    </p>
                  </div>
                </div>

                {isUnread && (
                  <button
                    type="button"
                    disabled={isMarkingAsRead}
                    onClick={() => handleMarkAsRead(item)}
                    className="min-h-[44px] inline-flex items-center justify-center gap-1.5 px-4 rounded-xl border border-emerald-300 bg-white hover:bg-emerald-50 text-xs font-semibold text-[#005141] transition-colors duration-150 shrink-0 cursor-pointer disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
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

      {/* Pagination Controls */}
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
          <span className="text-xs font-semibold text-stone-600">Página {page + 1}</span>
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
