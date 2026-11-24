'use client';

import { useEffect, useState } from 'react';
import { Heart } from 'lucide-react';
import { toast } from 'sonner';
import { customerFavoriteService } from '@/features/customer/services/customerFavoriteService';

export function FavoriteButton({ productId }: { productId: string }) {
  const [isFavorite, setIsFavorite] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    customerFavoriteService.checkFavoriteStatus(productId)
      .then((result) => setIsFavorite(result.isFavorite))
      .catch(() => undefined);
  }, [productId]);

  const toggleFavorite = async () => {
    setLoading(true);
    try {
      const result = isFavorite
        ? await customerFavoriteService.removeFavorite(productId)
        : await customerFavoriteService.addFavorite(productId);
      setIsFavorite(result.isFavorite);
      toast.success(result.isFavorite ? 'Producto guardado en favoritos.' : 'Producto retirado de favoritos.');
    } catch (error) {
      if (error instanceof Error && error.message === 'No autenticado.') {
        toast.error('Inicia sesión como cliente para guardar favoritos.');
        const currentUrl = `${window.location.pathname}${window.location.search}`;
        window.location.assign(`/login?redirect=${encodeURIComponent(currentUrl)}`);
        return;
      }
      toast.error(error instanceof Error ? error.message : 'No se pudo actualizar el favorito.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      aria-label={isFavorite ? 'Quitar de favoritos' : 'Agregar a favoritos'}
      aria-pressed={isFavorite}
      disabled={loading}
      onClick={toggleFavorite}
      className="rounded-full border border-stone-200 bg-white/95 p-2.5 text-stone-500 shadow-xs transition hover:border-[#005141] hover:text-[#005141] disabled:opacity-50"
    >
      <Heart className={isFavorite ? 'h-5 w-5 fill-[#ea580c] text-[#ea580c]' : 'h-5 w-5'} />
    </button>
  );
}
