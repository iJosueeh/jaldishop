'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Package,
  CreditCard,
  BarChart3,
  Heart,
  MessageSquareQuote,
  Bell,
  MapPin,
  ShieldCheck,
  ArrowLeft,
  Menu,
  X,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useUnreadNotificationsCount } from '../hooks/useCustomerNotifications';
import { useCustomerProfile } from '../hooks/useCustomerProfile';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  exact?: boolean;
  badgeCount?: number;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

export function CustomerNav() {
  const pathname = usePathname();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  const { unreadCount } = useUnreadNotificationsCount(true);
  const { data: profile } = useCustomerProfile();

  const groups: NavGroup[] = [
    {
      title: 'Mi Actividad',
      items: [
        { href: '/mi-jaldishop', label: 'Inicio', icon: LayoutDashboard, exact: true },
        { href: '/mi-jaldishop/pedidos', label: 'Mis Pedidos', icon: Package },
        { href: '/mi-jaldishop/pagos', label: 'Mis Pagos', icon: CreditCard },
        { href: '/mi-jaldishop/estadisticas', label: 'Mis Estadísticas', icon: BarChart3 },
        { href: '/mi-jaldishop/favoritos', label: 'Mis Favoritos', icon: Heart },
        { href: '/mi-jaldishop/resenas', label: 'Mis Reseñas', icon: MessageSquareQuote },
        {
          href: '/mi-jaldishop/notificaciones',
          label: 'Notificaciones',
          icon: Bell,
          badgeCount: unreadCount,
        },
      ],
    },
    {
      title: 'Mi Cuenta',
      items: [
        { href: '/mi-jaldishop/direcciones', label: 'Mis Direcciones', icon: MapPin },
        { href: '/mi-jaldishop/perfil', label: 'Mi Cuenta / Seguridad', icon: ShieldCheck },
      ],
    },
  ];

  const allItems = groups.flatMap((g) => g.items);
  const activeItem =
    allItems.find((item) =>
      item.exact ? pathname === item.href : pathname.startsWith(item.href)
    ) ?? allItems[0];

  useEffect(() => {
    setIsMobileOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!isMobileOpen) return;

    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMobileOpen(false);
        triggerRef.current?.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = prevOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMobileOpen]);

  const userInitial = profile?.firstName ? profile.firstName.charAt(0).toUpperCase() : 'C';
  const userFullName = profile
    ? `${profile.firstName} ${profile.lastName || ''}`.trim()
    : 'Mi Cuenta';

  const renderNavContent = (onNavigate?: () => void) => (
    <div className="flex flex-col h-full">
      {/* Flat Customer Identity Header (no nested card) */}
      <div className="flex items-center gap-3 pb-4 mb-4 border-b border-[#e7e0d6]">
        <div
          className="w-9 h-9 rounded-full bg-[#072720] text-[#36f4a4] font-bold text-sm flex items-center justify-center shrink-0"
          aria-hidden="true"
        >
          {userInitial}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-xs font-bold text-[#1c1917] truncate">{userFullName}</p>
          <p className="text-[11px] text-[#57534e] truncate">
            {profile?.email || 'Comprador JaldiShop'}
          </p>
        </div>
      </div>

      {/* Navigation Groups */}
      <nav aria-label="Navegación de Mi JaldiShop" className="space-y-5 flex-1">
        {groups.map((group) => (
          <div key={group.title} className="space-y-1">
            <p className="px-3 text-[11px] font-bold uppercase tracking-wider text-[#57534e]">
              {group.title}
            </p>
            <ul role="list" className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = item.exact
                  ? pathname === item.href
                  : pathname.startsWith(item.href);
                const showBadge = typeof item.badgeCount === 'number' && item.badgeCount > 0;

                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      onClick={onNavigate}
                      aria-current={isActive ? 'page' : undefined}
                      className={cn(
                        'min-h-[44px] flex items-center justify-between gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
                        isActive
                          ? 'bg-[#072720] text-white font-semibold'
                          : 'text-[#1c1917] hover:bg-[#f7f3ec]'
                      )}
                    >
                      <span className="flex items-center gap-2.5 min-w-0">
                        <Icon
                          className={cn(
                            'w-4 h-4 shrink-0',
                            isActive ? 'text-[#36f4a4]' : 'text-[#57534e]'
                          )}
                        />
                        <span className="truncate">{item.label}</span>
                      </span>
                      {showBadge && (
                        <>
                          <span
                            aria-hidden="true"
                            className={cn(
                              'px-2 py-0.5 text-[11px] font-bold rounded-full tabular-nums shrink-0 transition-colors duration-150',
                              isActive
                                ? 'bg-[#36f4a4] text-[#02090a]'
                                : 'bg-[#005141] text-white'
                            )}
                          >
                            {item.badgeCount! > 99 ? '99+' : item.badgeCount}
                          </span>
                          <span className="sr-only" role="status" aria-atomic="true">
                            {item.badgeCount === 1
                              ? '1 notificación sin leer'
                              : `${item.badgeCount} notificaciones sin leer`}
                          </span>
                        </>
                      )}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      {/* Back to Marketplace */}
      <div className="pt-4 mt-5 border-t border-[#e7e0d6]">
        <Link
          href="/"
          onClick={onNavigate}
          className="min-h-[44px] flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-semibold text-[#57534e] hover:text-[#005141] hover:bg-[#f7f3ec] transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <ArrowLeft className="w-4 h-4 shrink-0" />
          <span>Explorar comercios locales</span>
        </Link>
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile Top Bar & Drawer Trigger */}
      <div className="lg:hidden mb-5 flex items-center justify-between gap-3 rounded-xl bg-white border border-[#e7e0d6] px-4 py-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <activeItem.icon className="w-4 h-4 text-[#005141] shrink-0" />
          <span className="text-sm font-bold text-[#1c1917] truncate">
            {activeItem.label}
          </span>
          {unreadCount > 0 && (
            <>
              <span
                aria-hidden="true"
                className="px-2 py-0.5 text-[11px] font-bold rounded-full bg-[#005141] text-white tabular-nums"
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
              <span className="sr-only" role="status" aria-atomic="true">
                {unreadCount === 1
                  ? '1 notificación sin leer'
                  : `${unreadCount} notificaciones sin leer`}
              </span>
            </>
          )}
        </div>

        <button
          ref={triggerRef}
          type="button"
          onClick={() => setIsMobileOpen(true)}
          aria-expanded={isMobileOpen}
          aria-controls="customer-sidebar-drawer"
          aria-label="Abrir menú de Mi JaldiShop"
          className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center gap-2 px-3 rounded-lg border border-[#e7e0d6] bg-[#faf7f2] hover:bg-[#f0eae0] text-xs font-semibold text-[#1c1917] transition-colors duration-150 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
        >
          <Menu className="w-4 h-4" />
          <span>Menú</span>
        </button>
      </div>

      {/* Mobile Accessible Drawer */}
      {isMobileOpen && (
        <div
          id="customer-sidebar-drawer"
          role="dialog"
          aria-modal="true"
          aria-label="Menú de navegación de Mi JaldiShop"
          className="fixed inset-0 z-50 lg:hidden flex"
        >
          <div
            className="fixed inset-0 bg-[#02090a]/60 backdrop-blur-xs"
            onClick={() => {
              setIsMobileOpen(false);
              triggerRef.current?.focus();
            }}
            aria-hidden="true"
          />
          <div className="relative z-10 w-[84vw] max-w-[300px] h-full bg-white border-r border-[#e7e0d6] p-5 overflow-y-auto flex flex-col">
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-[#e7e0d6]">
              <span className="font-display text-base font-bold text-[#1c1917]">
                Mi JaldiShop
              </span>
              <button
                ref={closeButtonRef}
                type="button"
                onClick={() => {
                  setIsMobileOpen(false);
                  triggerRef.current?.focus();
                }}
                aria-label="Cerrar menú"
                className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-lg bg-[#faf7f2] hover:bg-[#f0eae0] text-[#1c1917] transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            {renderNavContent(() => setIsMobileOpen(false))}
          </div>
        </div>
      )}

      {/* Desktop Sticky Sidebar Rail */}
      <aside
        aria-label="Barra lateral de Mi JaldiShop"
        className="hidden lg:block sticky top-24 rounded-xl bg-white border border-[#e7e0d6] p-4"
      >
        {renderNavContent()}
      </aside>
    </>
  );
}
