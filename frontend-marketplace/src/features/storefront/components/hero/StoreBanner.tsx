'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowLeft, Share2 } from 'lucide-react';
import { toast } from 'sonner';

interface StoreBannerProps {
  bannerUrl?: string;
  storeName: string;
}

export function StoreBanner({ bannerUrl, storeName }: StoreBannerProps) {
  const [hasError, setHasError] = useState(false);

  const handleShare = () => {
    if (typeof window !== 'undefined') {
      navigator.clipboard.writeText(window.location.href);
      toast.success('¡Enlace de la tienda copiado!', {
        description: `Compartiendo la tienda de ${storeName}.`,
      });
    }
  };

  const showImage = bannerUrl && !hasError;

  return (
    <div className="relative w-full h-64 sm:h-80 md:h-96 bg-gradient-to-br from-[#00382d] via-[#005141] to-[#002820] overflow-hidden">
      {/* Portada Fotográfica */}
      {showImage ? (
        <div className="absolute inset-0">
          <Image
            src={bannerUrl}
            alt={`Portada de ${storeName}`}
            fill
            priority
            sizes="100vw"
            className="object-cover object-center transition-transform duration-700 hover:scale-102"
            onError={() => setHasError(true)}
          />
          {/* Scrim sutil superior e inferior: Permite ver la foto clara y nítida protegiendo la legibilidad de navegación */}
          <div className="absolute inset-0 bg-gradient-to-b from-black/60 via-transparent to-black/50" />
        </div>
      ) : (
        /* Fondo Artesanal por Defecto con Iluminación Cálida */
        <div className="absolute inset-0 bg-gradient-to-br from-[#00382d] via-[#005141] to-[#002820]">
          <div className="absolute top-0 right-1/4 w-[400px] h-[300px] bg-[#feae2c]/20 blur-[100px] pointer-events-none" />
          <div className="absolute bottom-0 left-10 w-[350px] h-[300px] bg-[#ea580c]/15 blur-[100px] pointer-events-none" />
          {/* Patrón geométrico artesanal tenue */}
          <div
            className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:24px_24px]"
            aria-hidden="true"
          />
        </div>
      )}

      {/* Barra de Navegación Superior Flotante */}
      <div className="absolute top-0 inset-x-0 z-20 pt-6 sm:pt-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          <Link
            href="/"
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/40 hover:bg-black/60 backdrop-blur-md text-xs font-bold text-white border border-white/20 transition-all hover:-translate-x-0.5 shadow-sm"
          >
            <ArrowLeft className="w-4 h-4 text-[#feae2c]" />
            <span>Volver al Marketplace</span>
          </Link>

          <div className="flex items-center gap-2">
            <button
              onClick={handleShare}
              className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/40 hover:bg-black/60 backdrop-blur-md text-xs font-bold text-white border border-white/20 transition-all cursor-pointer shadow-sm hover:scale-105 active:scale-95"
              title="Compartir tienda"
              aria-label="Compartir tienda"
            >
              <Share2 className="w-3.5 h-3.5 text-[#feae2c]" />
              <span className="hidden sm:inline">Compartir</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
