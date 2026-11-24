'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Heart } from 'lucide-react';
import { toast } from 'sonner';
import { customerFavoriteService } from '@/features/customer/services/customerFavoriteService';
import { cn } from '@/shared/utils/cn';

interface FavoriteButtonProps {
  productId: string;
  variant?: 'icon' | 'pill';
  className?: string;
}

const TOGGLE_COOLDOWN_MS = 700;

export function FavoriteButton({ productId, variant = 'icon', className }: FavoriteButtonProps) {
  const router = useRouter();
  const [isFavorite, setIsFavorite] = useState(false);
  const [loading, setLoading] = useState(false);
  const [coolingDown, setCoolingDown] = useState(false);

  const inFlightRef = useRef(false);
  const isUnauthenticatedRef = useRef(false);
  const cooldownTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    let active = true;
    customerFavoriteService
      .checkFavoriteStatus(productId)
      .then((result) => {
        if (active) {
          isUnauthenticatedRef.current = false;
          setIsFavorite(result.isFavorite);
        }
      })
      .catch((error) => {
        if (active && error instanceof Error && error.message === 'No autenticado.') {
          isUnauthenticatedRef.current = true;
        }
      });

    return () => {
      active = false;
      if (cooldownTimerRef.current) {
        clearTimeout(cooldownTimerRef.current);
      }
    };
  }, [productId]);

  const startCooldown = () => {
    setCoolingDown(true);
    if (cooldownTimerRef.current) clearTimeout(cooldownTimerRef.current);
    cooldownTimerRef.current = setTimeout(() => {
      setCoolingDown(false);
    }, TOGGLE_COOLDOWN_MS);
  };

  const promptLogin = () => {
    const currentUrl = `${window.location.pathname}${window.location.search}`;
    toast.info('Inicia sesión para guardar este producto en tus favoritos.', {
      id: `favorite-${productId}`,
      action: {
        label: 'Iniciar sesión',
        onClick: () => router.push(`/login?redirect=${encodeURIComponent(currentUrl)}`),
      },
    });
  };

  const toggleFavorite = async () => {
    if (inFlightRef.current || loading || coolingDown) return;

    if (isUnauthenticatedRef.current) {
      startCooldown();
      promptLogin();
      return;
    }

    inFlightRef.current = true;
    const previous = isFavorite;
    const next = !previous;
    setIsFavorite(next);
    setLoading(true);

    try {
      const result = previous
        ? await customerFavoriteService.removeFavorite(productId)
        : await customerFavoriteService.addFavorite(productId);

      setIsFavorite(result.isFavorite);
      if (result.isFavorite) {
        toast.success('Añadido a tus favoritos.', {
          id: `favorite-${productId}`,
          action: {
            label: 'Ver favoritos',
            onClick: () => router.push('/mi-jaldishop/favoritos'),
          },
        });
      } else {
        toast.success('Retirado de tus favoritos.', {
          id: `favorite-${productId}`,
        });
      }
    } catch (error) {
      setIsFavorite(previous);
      if (error instanceof Error && error.message === 'No autenticado.') {
        isUnauthenticatedRef.current = true;
        promptLogin();
        return;
      }
      toast.error(error instanceof Error ? error.message : 'No se pudo actualizar el favorito.', {
        id: `favorite-${productId}`,
      });
    } finally {
      inFlightRef.current = false;
      setLoading(false);
      startCooldown();
    }
  };

  return (
    <button
      type="button"
      aria-label={isFavorite ? 'Quitar de favoritos' : 'Agregar a favoritos'}
      aria-pressed={isFavorite}
      disabled={loading || coolingDown}
      onClick={toggleFavorite}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-full border transition-all cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#36f4a4]',
        variant === 'pill'
          ? 'min-h-[44px] px-4 py-2 text-xs font-semibold border-stone-200 bg-white text-stone-700 hover:border-[#005141] hover:text-[#005141]'
          : 'min-h-[44px] min-w-[44px] border-stone-200 bg-white/95 p-2.5 text-stone-500 shadow-xs hover:border-[#005141] hover:text-[#005141]',
        isFavorite && 'border-rose-200 bg-rose-50/80 text-rose-600 hover:border-rose-300 hover:text-rose-700',
        className
      )}
    >
      <Heart
        className={cn(
          'h-4.5 w-4.5 transition-transform active:scale-125',
          isFavorite ? 'fill-rose-500 text-rose-500' : ''
        )}
      />
      {variant === 'pill' && <span>{isFavorite ? 'En favoritos' : 'Guardar'}</span>}
    </button>
  );
}
