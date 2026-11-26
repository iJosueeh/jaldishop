'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { User, MapPin } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface AccountSubNavProps {
  addressCount?: number;
}

export function AccountSubNav({ addressCount }: AccountSubNavProps) {
  const pathname = usePathname();

  const isProfileActive = pathname === '/mi-jaldishop/perfil';
  const isAddressesActive = pathname.startsWith('/mi-jaldishop/direcciones');

  return (
    <nav
      aria-label="Subnavegación de cuenta"
      className="flex items-center gap-2 border-b border-[#e7e0d6] pb-px mb-6 overflow-x-auto"
    >
      <Link
        href="/mi-jaldishop/perfil"
        aria-current={isProfileActive ? 'page' : undefined}
        className={cn(
          'relative min-h-[44px] inline-flex items-center gap-2 py-2 px-4 text-xs sm:text-sm font-semibold transition-colors whitespace-nowrap focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
          isProfileActive
            ? 'text-[#005141] bg-[#f0fdfa]'
            : 'text-[#57534e] hover:text-[#1c1917] hover:bg-[#f7f3ec]'
        )}
      >
        <User className={cn('w-4 h-4', isProfileActive ? 'text-[#005141]' : 'text-[#78716c]')} />
        <span>Datos Personales</span>
        {isProfileActive && (
          <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" aria-hidden="true" />
        )}
      </Link>

      <Link
        href="/mi-jaldishop/direcciones"
        aria-current={isAddressesActive ? 'page' : undefined}
        className={cn(
          'relative min-h-[44px] inline-flex items-center gap-2 py-2 px-4 text-xs sm:text-sm font-semibold transition-colors whitespace-nowrap focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
          isAddressesActive
            ? 'text-[#005141] bg-[#f0fdfa]'
            : 'text-[#57534e] hover:text-[#1c1917] hover:bg-[#f7f3ec]'
        )}
      >
        <MapPin className={cn('w-4 h-4', isAddressesActive ? 'text-[#005141]' : 'text-[#78716c]')} />
        <span>Mis Direcciones</span>
        {typeof addressCount === 'number' && (
          <span
            className={cn(
              'px-2 py-0.5 text-[11px] font-bold rounded-full tabular-nums',
              isAddressesActive
                ? 'bg-[#005141] text-white'
                : 'bg-[#f0eae0] text-[#1c1917]'
            )}
          >
            {addressCount}/5
          </span>
        )}
        {isAddressesActive && (
          <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#005141]" aria-hidden="true" />
        )}
      </Link>
    </nav>
  );
}
