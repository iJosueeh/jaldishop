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
    <div className="flex items-center gap-2 border-b border-stone-200/90 pb-px mb-6 overflow-x-auto">
      <Link
        href="/mi-jaldishop/perfil"
        className={cn(
          'inline-flex items-center gap-2 py-2.5 px-4 rounded-t-xl text-xs sm:text-sm font-semibold border-b-2 transition-all whitespace-nowrap',
          isProfileActive
            ? 'border-[#005141] text-[#005141] bg-[#005141]/5'
            : 'border-transparent text-stone-600 hover:text-stone-900 hover:bg-stone-100/60'
        )}
      >
        <User className={cn('w-4 h-4', isProfileActive ? 'text-[#005141]' : 'text-stone-400')} />
        <span>Datos Personales</span>
      </Link>

      <Link
        href="/mi-jaldishop/direcciones"
        className={cn(
          'inline-flex items-center gap-2 py-2.5 px-4 rounded-t-xl text-xs sm:text-sm font-semibold border-b-2 transition-all whitespace-nowrap',
          isAddressesActive
            ? 'border-[#005141] text-[#005141] bg-[#005141]/5'
            : 'border-transparent text-stone-600 hover:text-stone-900 hover:bg-stone-100/60'
        )}
      >
        <MapPin className={cn('w-4 h-4', isAddressesActive ? 'text-[#005141]' : 'text-stone-400')} />
        <span>Mis Direcciones</span>
        {typeof addressCount === 'number' && (
          <span
            className={cn(
              'px-2 py-0.5 text-[11px] font-bold rounded-full',
              isAddressesActive
                ? 'bg-[#005141] text-white'
                : 'bg-stone-200 text-stone-700'
            )}
          >
            {addressCount}/5
          </span>
        )}
      </Link>
    </div>
  );
}
