'use client';

import React, { useState } from 'react';
import Image from 'next/image';

interface StoreLogoProps {
  logoUrl?: string;
  storeName: string;
  iconEmoji?: string;
}

export function StoreLogo({ logoUrl, storeName, iconEmoji }: StoreLogoProps) {
  const [hasError, setHasError] = useState(false);
  const showImage = logoUrl && !hasError;
  const initials = storeName
    ? storeName
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((w) => w[0].toUpperCase())
        .join('')
    : 'JD';

  return (
    <div className="relative -mt-16 sm:-mt-20 md:-mt-24 shrink-0 z-20">
      {/* 12px border radius logo container as specified in DESIGN.md with crisp boutique ring */}
      <div className="relative w-28 h-28 sm:w-36 sm:h-36 md:w-40 md:h-40 rounded-[12px] bg-white shadow-xl ring-4 ring-white border border-stone-200/90 overflow-hidden flex items-center justify-center transition-transform duration-300 hover:scale-[1.02]">
        {showImage ? (
          <Image
            src={logoUrl}
            alt={`Logo de ${storeName}`}
            fill
            sizes="(max-width: 640px) 112px, (max-width: 768px) 144px, 160px"
            className="object-cover w-full h-full"
            onError={() => setHasError(true)}
            priority
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-tr from-[#005141] via-[#003d31] to-[#166a57] flex items-center justify-center text-white shadow-inner">
            {iconEmoji ? (
              <span className="text-4xl sm:text-5xl" role="img" aria-label="Icono de la tienda">
                {iconEmoji}
              </span>
            ) : (
              <span className="font-display font-bold text-2xl sm:text-3xl tracking-wider text-amber-200">
                {initials}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
