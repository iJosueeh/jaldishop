import React from 'react';
import Image from 'next/image';
import { cn } from '@/shared/utils/cn';

interface JaldiShopLogoProps {
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
  className?: string;
}

export function JaldiShopLogo({
  size = 'md',
  showText = true,
  className,
}: JaldiShopLogoProps) {
  const sizes = {
    sm: { icon: 28, text: 'text-base', subtext: 'text-[9px]' },
    md: { icon: 38, text: 'text-xl', subtext: 'text-[10px]' },
    lg: { icon: 48, text: 'text-2xl', subtext: 'text-xs' },
  };

  const { icon, text, subtext } = sizes[size];

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
        <div className="flex flex-col">
          <span className={cn('font-extrabold tracking-tight text-[#1c1917] leading-none', text)}>
            Jaldi<span className="text-[#ea580c]">Shop</span>
          </span>
          <span className={cn('font-bold text-[#57534e] tracking-wider uppercase mt-0.5', subtext)}>
            Marketplace
          </span>
        </div>
      )}
    </div>
  );
}
