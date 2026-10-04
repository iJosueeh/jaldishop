'use client';

import { useState } from 'react';
import Image from 'next/image';

interface ProductVisualProps {
  name: string;
  imageUrl?: string;
  iconText: string;
  imageBg: string;
  isDemo?: boolean;
}

export function ProductVisual({ name, imageUrl, iconText, imageBg, isDemo = false }: ProductVisualProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  return (
    <div className={`relative aspect-[4/3] overflow-hidden ${imageBg}`}>
      {imageUrl && failedUrl !== imageUrl ? (
        <Image src={imageUrl} alt={isDemo ? `Imagen de referencia: ${name}` : name} fill sizes="(max-width: 639px) 100vw, (max-width: 1023px) 50vw, 33vw" className="object-cover transition-transform duration-700 motion-safe:group-hover:scale-105" onError={() => setFailedUrl(imageUrl)} />
      ) : (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3">
          <span aria-hidden="true" className="text-6xl transition-transform duration-500 motion-safe:group-hover:scale-110">{iconText}</span>
          <span className="text-xs font-medium">Sin fotografía del producto</span>
        </div>
      )}
      {isDemo && imageUrl && failedUrl !== imageUrl && <span className="absolute bottom-3 left-3 rounded-lg bg-black/65 px-2.5 py-1 text-[11px] text-white backdrop-blur-sm">Imagen de referencia</span>}
      <div aria-hidden="true" className="absolute inset-0 -translate-x-full motion-safe:group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none" />
    </div>
  );
}
