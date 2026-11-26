'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Image from 'next/image';
import { ChevronLeft, ChevronRight, Clock, Expand, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { ProductImagePlaceholder } from '../ProductImagePlaceholder';
import { SafeProductImage, isRenderableImageUrl } from '../SafeProductImage';

interface ProductGalleryProps {
  name: string;
  imageUrl?: string;
  images?: string[];
  category?: string;
  prepTimeMinutes?: number;
  isAvailable?: boolean;
  isOutOfStock?: boolean;
}

const SWIPE_THRESHOLD_PX = 40;

export function ProductGallery({
  name,
  imageUrl,
  images,
  category,
  prepTimeMinutes,
  isAvailable = true,
  isOutOfStock = false,
}: ProductGalleryProps) {
  const [failedUrls, setFailedUrls] = useState<Set<string>>(() => new Set());
  const [activeIndex, setActiveIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  const triggerBtnRef = useRef<HTMLButtonElement>(null);
  const closeBtnRef = useRef<HTMLButtonElement>(null);
  const wasLightboxOpenRef = useRef(false);
  const touchStartRef = useRef<{ x: number; y: number } | null>(null);

  const candidateImages = useMemo(() => {
    const unique = new Set<string>();
    if (Array.isArray(images)) {
      for (const url of images) {
        if (isRenderableImageUrl(url)) {
          unique.add(url.trim());
        }
      }
    }
    if (isRenderableImageUrl(imageUrl)) {
      unique.add(imageUrl.trim());
    }
    return Array.from(unique);
  }, [images, imageUrl]);

  const validImages = useMemo(
    () => candidateImages.filter((url) => !failedUrls.has(url)),
    [candidateImages, failedUrls]
  );

  const safeIndex =
    validImages.length > 0 ? Math.min(activeIndex, validImages.length - 1) : 0;
  const activeUrl = validImages[safeIndex] ?? null;
  const hasMultiple = validImages.length > 1;

  const markUrlFailed = useCallback((url: string) => {
    setFailedUrls((prev) => {
      if (prev.has(url)) return prev;
      const next = new Set(prev);
      next.add(url);
      return next;
    });
  }, []);

  const goPrev = useCallback(() => {
    if (validImages.length <= 1) return;
    setActiveIndex((prev) => {
      const current = Math.min(prev, validImages.length - 1);
      return (current - 1 + validImages.length) % validImages.length;
    });
  }, [validImages.length]);

  const goNext = useCallback(() => {
    if (validImages.length <= 1) return;
    setActiveIndex((prev) => {
      const current = Math.min(prev, validImages.length - 1);
      return (current + 1) % validImages.length;
    });
  }, [validImages.length]);

  useEffect(() => {
    if (lightboxOpen) {
      wasLightboxOpenRef.current = true;
      closeBtnRef.current?.focus();
    } else if (wasLightboxOpenRef.current) {
      wasLightboxOpenRef.current = false;
      triggerBtnRef.current?.focus();
    }
  }, [lightboxOpen]);

  useEffect(() => {
    if (!lightboxOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setLightboxOpen(false);
      } else if (event.key === 'ArrowLeft') {
        event.preventDefault();
        goPrev();
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        goNext();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [lightboxOpen, goPrev, goNext]);

  const handleGalleryKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (!hasMultiple) return;
    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      goPrev();
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      goNext();
    }
  };

  const handleTouchStart = (event: React.TouchEvent<HTMLDivElement>) => {
    const touch = event.touches[0];
    if (!touch) return;
    touchStartRef.current = { x: touch.clientX, y: touch.clientY };
  };

  const handleTouchEnd = (event: React.TouchEvent<HTMLDivElement>) => {
    if (!hasMultiple || !touchStartRef.current) return;
    const touch = event.changedTouches[0];
    if (!touch) return;
    const dx = touch.clientX - touchStartRef.current.x;
    const dy = touch.clientY - touchStartRef.current.y;
    touchStartRef.current = null;
    if (Math.abs(dx) >= SWIPE_THRESHOLD_PX && Math.abs(dx) > Math.abs(dy)) {
      if (dx < 0) {
        goNext();
      } else {
        goPrev();
      }
    }
  };

  return (
    <>
      <div className="space-y-3">
        <div
          role="region"
          aria-roledescription="carrusel"
          aria-label={`Galería de imágenes de ${name}`}
          tabIndex={hasMultiple ? 0 : undefined}
          onKeyDown={handleGalleryKeyDown}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
          className="group relative overflow-hidden rounded-[12px] border border-[#e7e0d6] bg-[#faf7f2] shadow-2xs touch-pan-y focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141]"
        >
          {hasMultiple && (
            <p className="sr-only" aria-live="polite">
              Imagen {safeIndex + 1} de {validImages.length}
            </p>
          )}

          <div className="relative aspect-[4/3] lg:aspect-[5/4] w-full overflow-hidden bg-[#faf7f2]">
            {activeUrl ? (
              <Image
                key={activeUrl}
                src={activeUrl}
                alt={hasMultiple ? `${name} — imagen ${safeIndex + 1} de ${validImages.length}` : name}
                fill
                priority
                sizes="(max-width: 1023px) 100vw, 55vw"
                className="object-cover transition-all duration-200 ease-out [@media(hover:hover)]:group-hover:scale-[1.03] motion-reduce:transition-none"
                onError={() => markUrlFailed(activeUrl)}
              />
            ) : (
              <ProductImagePlaceholder variant="detail" category={category} />
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
            {activeUrl && (
              <button
                ref={triggerBtnRef}
                type="button"
                onClick={() => setLightboxOpen(true)}
                aria-label={`Ampliar fotografía de ${name}`}
                className="absolute top-3 right-3 z-10 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/80 border border-white/15 p-2 text-white backdrop-blur-md transition-colors duration-150 ease-out hover:bg-[#02090a] hover:border-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer motion-reduce:transition-none"
              >
                <Expand className="h-4 w-4" aria-hidden="true" />
              </button>
            )}

            {/* Controles Prev / Next */}
            {hasMultiple && (
              <>
                <button
                  type="button"
                  onClick={goPrev}
                  aria-label="Imagen anterior"
                  className="absolute left-3 top-1/2 z-10 -translate-y-1/2 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/80 border border-white/15 text-white backdrop-blur-md transition-colors duration-150 ease-out hover:bg-[#02090a] hover:border-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer motion-reduce:transition-none"
                >
                  <ChevronLeft className="h-5 w-5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={goNext}
                  aria-label="Imagen siguiente"
                  className="absolute right-3 top-1/2 z-10 -translate-y-1/2 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/80 border border-white/15 text-white backdrop-blur-md transition-colors duration-150 ease-out hover:bg-[#02090a] hover:border-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer motion-reduce:transition-none"
                >
                  <ChevronRight className="h-5 w-5" aria-hidden="true" />
                </button>
              </>
            )}

            {/* Indicador 1 / N */}
            {hasMultiple && (
              <div className="absolute bottom-3 right-3 z-10 rounded-full bg-[#02090a]/80 border border-white/15 px-3 py-1 text-xs font-semibold text-white backdrop-blur-md tabular-nums pointer-events-none">
                {safeIndex + 1} / {validImages.length}
              </div>
            )}

            {/* Tiempo de preparación */}
            {prepTimeMinutes ? (
              <div className="absolute bottom-3 left-3 z-10 inline-flex items-center gap-1.5 rounded-full bg-white/95 backdrop-blur-md px-3 py-1.5 text-xs font-semibold text-stone-800 border border-stone-200 shadow-xs pointer-events-none">
                <Clock className="h-3.5 w-3.5 text-[#ea580c]" aria-hidden="true" />
                <span>Preparación ~{prepTimeMinutes} min</span>
              </div>
            ) : null}
          </div>
        </div>

        {/* Tira de miniaturas */}
        {hasMultiple && (
          <div
            role="group"
            aria-label={`Miniaturas de ${name}`}
            className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar"
          >
            {validImages.map((thumbUrl, idx) => {
              const isActive = idx === safeIndex;
              return (
                <button
                  key={thumbUrl}
                  type="button"
                  onClick={() => setActiveIndex(idx)}
                  aria-label={`Ver imagen ${idx + 1} de ${validImages.length}`}
                  aria-current={isActive ? 'true' : undefined}
                  className={cn(
                    'relative h-16 w-20 shrink-0 overflow-hidden rounded-[8px] border transition-all duration-150 ease-out cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#005141] motion-reduce:transition-none',
                    isActive
                      ? 'border-[#005141] ring-2 ring-[#005141] opacity-100'
                      : 'border-[#e7e0d6] opacity-75 hover:opacity-100 hover:border-[#005141]/50'
                  )}
                >
                  <SafeProductImage
                    src={thumbUrl}
                    alt={`${name} miniatura ${idx + 1}`}
                    variant="thumb"
                    category={category}
                    sizes="80px"
                    onImageError={() => markUrlFailed(thumbUrl)}
                  />
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Lightbox Modal */}
      {lightboxOpen && activeUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={`Vista ampliada de ${name}`}
          onClick={() => setLightboxOpen(false)}
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#02090a]/90 p-4 sm:p-8 backdrop-blur-sm overscroll-contain"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-4xl overflow-hidden rounded-[16px] border border-[#1e2c31] bg-[#061a1c] shadow-2xl"
          >
            <button
              ref={closeBtnRef}
              type="button"
              onClick={() => setLightboxOpen(false)}
              aria-label="Cerrar vista ampliada"
              className="absolute top-3 right-3 z-10 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/85 border border-white/20 text-white hover:border-[#36f4a4] hover:text-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer"
            >
              <X className="h-5 w-5" aria-hidden="true" />
            </button>

            {hasMultiple && (
              <>
                <button
                  type="button"
                  onClick={goPrev}
                  aria-label="Imagen anterior en vista ampliada"
                  className="absolute left-3 top-1/2 z-10 -translate-y-1/2 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/85 border border-white/20 text-white hover:border-[#36f4a4] hover:text-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer"
                >
                  <ChevronLeft className="h-5 w-5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={goNext}
                  aria-label="Imagen siguiente en vista ampliada"
                  className="absolute right-3 top-1/2 z-10 -translate-y-1/2 inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-full bg-[#02090a]/85 border border-white/20 text-white hover:border-[#36f4a4] hover:text-[#36f4a4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4] cursor-pointer"
                >
                  <ChevronRight className="h-5 w-5" aria-hidden="true" />
                </button>
                <div className="absolute bottom-3 left-1/2 z-10 -translate-x-1/2 rounded-full bg-[#02090a]/85 border border-white/15 px-3 py-1 text-xs font-semibold text-white tabular-nums pointer-events-none">
                  {safeIndex + 1} / {validImages.length}
                </div>
              </>
            )}

            <div className="relative aspect-[4/3] w-full bg-[#02090a]">
              <Image
                key={`lightbox-${activeUrl}`}
                src={activeUrl}
                alt={hasMultiple ? `${name} — imagen ${safeIndex + 1} de ${validImages.length}` : name}
                fill
                sizes="(max-width: 1024px) 100vw, 900px"
                className="object-contain"
                onError={() => markUrlFailed(activeUrl)}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
