import React from 'react';
import Image from 'next/image';
import { cn } from '@/shared/utils/cn';

interface JaldiShopLogoProps {
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
  variant?: 'light' | 'dark';
  className?: string;
}

export function JaldiShopLogo({
  size = 'md',
  showText = true,
  variant = 'dark',
  className,
}: JaldiShopLogoProps) {
  const sizes = {
    sm: { icon: 28, text: 'text-base' },
    md: { icon: 34, text: 'text-xl' },
    lg: { icon: 44, text: 'text-2xl' },
  };

  const { icon, text } = sizes[size];
  const isLight = variant === 'light';

  return (
    <div className={cn('flex items-center gap-2.5 select-none', className)}>
      <div className="relative shrink-0 rounded-2xl overflow-hidden shadow-sm hover:scale-105 transition-transform">
        <Image
          src="/favicon.svg"
          alt="JaldiShop Logo"
          width={icon}
          height={icon}
          className="object-contain"
          priority
        />
      </div>

      {showText && (
        <span
          className={cn(
            'font-display font-black tracking-tight leading-none',
            isLight ? 'text-white' : 'text-[#1c1917]',
            text
          )}
        >
          Jaldi<span className="text-[#feae2c]">Shop</span>
        </span>
      )}
    </div>
  );
}


