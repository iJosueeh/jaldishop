'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, Package, UserCheck, ArrowLeft, Heart, MessageSquareQuote } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  exact?: boolean;
}

const navItems: NavItem[] = [
  { href: '/mi-jaldishop', label: 'Inicio', icon: LayoutDashboard, exact: true },
  { href: '/mi-jaldishop/pedidos', label: 'Mis Pedidos', icon: Package },
  { href: '/mi-jaldishop/favoritos', label: 'Mis Favoritos', icon: Heart },
  { href: '/mi-jaldishop/resenas', label: 'Mis Reseñas', icon: MessageSquareQuote },
  { href: '/mi-jaldishop/perfil', label: 'Datos de mi Cuenta', icon: UserCheck },
];

export function CustomerNav() {
  const pathname = usePathname();

  return (
    <div className="w-full bg-white border-b border-stone-200/90 shadow-2xs">
      <div className="max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
          {/* Navigation Tabs (Full Width Flow) */}
          <nav
            className="flex items-center gap-1 sm:gap-6 overflow-x-auto scrollbar-none"
            aria-label="Navegación de Mi JaldiShop"
          >
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = item.exact
                ? pathname === item.href
                : item.href === '/mi-jaldishop/perfil'
                ? pathname.startsWith('/mi-jaldishop/perfil') || pathname.startsWith('/mi-jaldishop/direcciones')
                : pathname.startsWith(item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    'inline-flex items-center gap-2 py-3.5 px-3 sm:px-1 text-xs sm:text-sm font-semibold border-b-2 transition-all duration-150 whitespace-nowrap',
                    isActive
                      ? 'border-[#005141] text-[#005141]'
                      : 'border-transparent text-stone-500 hover:text-stone-900 hover:border-stone-300'
                  )}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon className={cn('w-4 h-4', isActive ? 'text-[#005141]' : 'text-stone-400')} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          {/* Quick link back to Marketplace */}
          <div className="hidden sm:flex items-center py-2">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-stone-500 hover:text-[#005141] transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Volver a explorar tiendas</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
