'use client';

import { useEffect, useState } from 'react';
import Image from 'next/image';
import { Clock, Expand, PackageOpen, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface ProductGalleryProps {
  name: string;
  imageUrl?: string;
  category?: string;
  prepTimeMinutes?: number;
  isAvailable?: boolean;
  isOutOfStock?: boolean;
}

export function ProductGallery({
  name,
  imageUrl,
  category,
  prepTimeMinutes,
  isAvailable = true,
  isOutOfStock = false,
}: ProductGalleryProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  const hasValidImage = Boolean(imageUrl && failedUrl !== imageUrl);

  useEffect(() => {
    if (!lightboxOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setLightboxOpen(false);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [lightboxOpen]);

  return (
    <>
      <div className="group relative overflow-hidden rounded-[12px] border border-stone-200/90 bg-white shadow-2xs">
        <div className="relative aspect-[4/3] lg:aspect-[5/4] w-full overflow-hidden bg-gradient-to-br from-[#061a1c] via-[#072720] to-[#02090a]">
          {hasValidImage && imageUrl ? (
            <Image
              src={imageUrl}
              alt={name}
              fill
              priority
              sizes="(max-width: 1023px) 100vw, 55vw"
              className="object-cover transition-transform duration-500 ease-out motion-safe:group-hover:scale-[1.03] motion-reduce:transition-none"
              onError={() => setFailedUrl(imageUrl)}
            />
          ) : (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 p-6 text-center bg-gradient-to-br from-[#061a1c] via-[#072720] to-[#02090a] text-white">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl border border-[#133b32] bg-[#072720] text-[#36f4a4] shadow-inner">
                <PackageOpen className="h-8 w-8" aria-hidden="true" />
              </div>
              <div className="space-y-1 max-w-xs">
                <p className="font-display text-lg font-bold text-white line-clamp-2">{name}</p>
                <p className="text-xs text-[#99b3ad]">Fotografía no publicada por el comercio</p>
              </div>
            </div>
          )}

          {/* Badges superiores */}
          <div className="absolute top-3 left-3 z-10 flex flex-wrap items-center gap-2 pointer-events-none">
            {category && (
              <span className="rounded-full bg-[#02090a]/80 backdrop-blur-md border border-[#1e2c31] px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-[#36f4a4]">
                {category}
              </span>
            )}
            <span
              className={cn(
                'rounded-full px-3 py-1 text-[11px] font-bold backdrop-blur-md border',
                isOutOfStock || !isAvailable
                  ? 'bg-rose-950/85 border-rose-500/40 text-rose-200'
                  : 'bg-[#072720]/85 border-[#133b32] text-white'
              )}
            >
              {isOutOfStock ? 'Agotado' : isAvailable ? 'Disponible' : 'No disponible'}
            </span>
          </div>

          {/* Botón Lightbox */}
          {hasValidImage && (
            <button
              type="button"
              onClick={() => setLightboxOpen(true)}
              aria-label={`Ampliar fotografía de ${name}`}
              className="absolute top-3 right-3 z-10 inline-flex min-h-[40px] min-w-[40px] items-center justify-center rounded-full bg-[#02090a]/75 border border-white/15 p-2 text-white backdrop-blur-md transition-all hover:bg-[#02090a] hover:border-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer"
            >
              <Expand className="h-4 w-4" />
            </button>
          )}

          {/* Tiempo de preparación */}
          {prepTimeMinutes ? (
            <div className="absolute bottom-3 left-3 z-10 inline-flex items-center gap-1.5 rounded-full bg-white/95 backdrop-blur-md px-3 py-1.5 text-xs font-semibold text-stone-800 border border-stone-200 shadow-xs pointer-events-none">
              <Clock className="h-3.5 w-3.5 text-[#ea580c]" />
              <span>Preparación ~{prepTimeMinutes} min</span>
            </div>
          ) : null}
        </div>
      </div>

      {/* Lightbox Modal */}
      {lightboxOpen && hasValidImage && imageUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={`Vista ampliada de ${name}`}
          onClick={() => setLightboxOpen(false)}
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#02090a]/90 p-4 sm:p-8 backdrop-blur-sm"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-4xl overflow-hidden rounded-[16px] border border-[#1e2c31] bg-[#061a1c] shadow-2xl"
          >
            <button
              type="button"
              onClick={() => setLightboxOpen(false)}
              aria-label="Cerrar vista ampliada"
              className="absolute top-3 right-3 z-10 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/85 border border-white/20 text-white hover:border-[#36f4a4] hover:text-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
            <div className="relative aspect-[4/3] w-full bg-[#02090a]">
              <Image
                src={imageUrl}
                alt={name}
                fill
                sizes="(max-width: 1024px) 100vw, 900px"
                className="object-contain"
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
