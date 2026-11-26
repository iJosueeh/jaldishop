'use client';

import { useState } from 'react';
import Image from 'next/image';
import { cn } from '@/shared/utils/cn';
import {
  ProductImagePlaceholder,
  ProductPlaceholderVariant,
} from './ProductImagePlaceholder';

export function isRenderableImageUrl(src?: string | null): src is string {
  if (typeof src !== 'string') return false;
  const trimmed = src.trim();
  if (!trimmed) return false;
  return (
    trimmed.startsWith('https://') ||
    trimmed.startsWith('http://') ||
    trimmed.startsWith('/')
  );
}

interface SafeProductImageProps {
  src?: string | null;
  alt: string;
  variant?: ProductPlaceholderVariant;
  category?: string;
  sizes?: string;
  priority?: boolean;
  className?: string;
  placeholderClassName?: string;
  useNativeImg?: boolean;
  onImageError?: (failedSrc: string) => void;
}

/**
 * Renders a product image safely with URL validation and onError fallback.
 * Never renders a broken <img> or raw alt text when src is missing or fails.
 */
export function SafeProductImage({
  src,
  alt,
  variant = 'card',
  category,
  sizes = '(max-width: 639px) 100vw, (max-width: 1023px) 50vw, 33vw',
  priority = false,
  className,
  placeholderClassName,
  useNativeImg = false,
  onImageError,
}: SafeProductImageProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const cleanSrc = isRenderableImageUrl(src) ? src.trim() : null;
  const shouldRenderImage = Boolean(cleanSrc && failedUrl !== cleanSrc);

  if (!shouldRenderImage || !cleanSrc) {
    return (
      <ProductImagePlaceholder
        variant={variant}
        category={category}
        className={placeholderClassName}
      />
    );
  }

  const handleError = () => {
    setFailedUrl(cleanSrc);
    onImageError?.(cleanSrc);
  };

  if (useNativeImg) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={cleanSrc}
        alt={alt}
        onError={handleError}
        className={cn(
          'h-full w-full object-cover transition-opacity duration-200 ease-out motion-reduce:transition-none',
          className
        )}
      />
    );
  }

  return (
    <Image
      src={cleanSrc}
      alt={alt}
      fill
      priority={priority}
      sizes={sizes}
      onError={handleError}
      className={cn(
        'object-cover transition-transform duration-200 ease-out motion-reduce:transition-none',
        className
      )}
    />
  );
}
