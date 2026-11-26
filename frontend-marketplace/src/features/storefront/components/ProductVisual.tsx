'use client';

import { useState } from 'react';
import { Images } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { SafeProductImage, isRenderableImageUrl } from './SafeProductImage';

interface ProductVisualProps {
  name: string;
  imageUrl?: string;
  iconText?: string;
  imageBg?: string;
  category?: string;
  imageCount?: number;
  isDemo?: boolean;
}

export function ProductVisual({
  name,
  imageUrl,
  imageBg = 'bg-[#faf7f2]',
  category,
  imageCount,
  isDemo = false,
}: ProductVisualProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const cleanSrc = isRenderableImageUrl(imageUrl) ? imageUrl.trim() : null;
  const hasValidImage = Boolean(cleanSrc && failedUrl !== cleanSrc);

  return (
    <div className={cn('relative aspect-[4/3] overflow-hidden', imageBg)}>
      <SafeProductImage
        src={imageUrl}
        alt={isDemo ? `Imagen de referencia: ${name}` : name}
        variant="card"
        category={category}
        sizes="(max-width: 639px) 100vw, (max-width: 1023px) 50vw, 33vw"
        className="[@media(hover:hover)]:group-hover:scale-105"
        onImageError={(failed) => setFailedUrl(failed)}
      />

      {isDemo && hasValidImage && (
        <span className="absolute bottom-3 left-3 z-10 rounded-full bg-[#02090a]/80 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-sm">
          Imagen de referencia
        </span>
      )}

      {!isDemo && hasValidImage && imageCount != null && imageCount > 1 && (
        <span
          aria-label={`${imageCount} fotografías`}
          className="absolute bottom-2.5 left-2.5 z-10 inline-flex items-center gap-1 rounded-full bg-[#02090a]/80 px-2 py-0.5 text-[11px] font-semibold text-white backdrop-blur-sm tabular-nums"
        >
          <Images className="h-3 w-3 text-[#36f4a4]" aria-hidden="true" />
          <span>{imageCount}</span>
        </span>
      )}

      {hasValidImage && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/15 to-transparent transition-transform duration-700 [@media(hover:hover)]:group-hover:translate-x-full motion-reduce:hidden"
        />
      )}
    </div>
  );
}
