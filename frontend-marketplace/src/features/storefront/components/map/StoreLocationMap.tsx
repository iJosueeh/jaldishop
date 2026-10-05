'use client';

import React, { useEffect, useRef, useState } from 'react';
import { MapPin, Navigation, ExternalLink, Copy, Check } from 'lucide-react';
import { PublicStore } from '../../types/storefront.types';

interface StoreLocationMapProps {
  store: PublicStore;
}

export function StoreLocationMap({ store }: StoreLocationMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const [copied, setCopied] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [mapError, setMapError] = useState(false);

  const lat = store.latitude;
  const lng = store.longitude;
  const hasCoords = typeof lat === 'number' && Number.isFinite(lat) && Math.abs(lat) <= 90 && typeof lng === 'number' && Number.isFinite(lng) && Math.abs(lng) <= 180;

  const address = store.address;
  const reference = store.addressReference;

  const mapsUrl = hasCoords || address ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(hasCoords ? `${lat},${lng}` : address!)}` : null;
  const wazeUrl = `https://waze.com/ul?ll=${lat},${lng}&navigate=yes`;

  const copyAddress = async () => {
    try {
      await navigator.clipboard.writeText([address, reference].filter(Boolean).join('. '));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback silencioso si no hay permisos de portapapeles
    }
  };

  useEffect(() => {
    let isMounted = true;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mapInstance: any = null;

    const initMap = async () => {
      if (!hasCoords || typeof window === 'undefined' || !mapContainerRef.current) return;

      try {
        const L = await import('leaflet');
        if (!isMounted || !mapContainerRef.current) return;

        // Evitar reinicialización si el contenedor ya tiene mapa
        if ((mapContainerRef.current as { _leaflet_id?: unknown })._leaflet_id) {
          return;
        }

        mapInstance = L.map(mapContainerRef.current, {
          center: [lat, lng],
          zoom: 15,
          zoomControl: false,
          scrollWheelZoom: false, // Evita atrapar el scroll de la página accidentalmente
        });

        // Capa OpenStreetMap 100% gratuita y sin requerimiento de API Key
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a>',
          maxZoom: 19,
          crossOrigin: true,
        }).addTo(mapInstance);

        L.control.zoom({ position: 'bottomright' }).addTo(mapInstance);

        // Pin Artesanal idéntico al portal de comerciantes
        const pinIcon = L.divIcon({
          className: 'jaldishop-store-pin',
          html: `
            <div style="width: 36px; height: 48px; cursor: pointer; display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.35));">
              <svg xmlns="http://www.w3.org/2000/svg" width="36" height="48" viewBox="0 0 36 48" fill="none">
                <path d="M18 2C9.163 2 2 9.163 2 18C2 28.5 18 46 18 46C18 46 34 28.5 34 18C34 9.163 26.837 2 18 2Z" fill="#ea580c" stroke="#ffffff" stroke-width="2.5" stroke-linejoin="round"/>
                <circle cx="18" cy="18" r="6" fill="#ffffff"/>
              </svg>
            </div>
          `,
          iconSize: [36, 48],
          iconAnchor: [18, 46],
        });

        const marker = L.marker([lat, lng], { icon: pinIcon }).addTo(mapInstance);
        const popup = document.createElement('div');
        popup.textContent = [store.name, address].filter(Boolean).join(' — ');
        marker.bindPopup(popup);

        if (isMounted) setMapLoaded(true);
      } catch (err) {
        if (isMounted) setMapError(true);
        console.error('Error inicializando mapa OpenStreetMap Leaflet:', err);
      }
    };

    initMap();

    return () => {
      isMounted = false;
      if (mapInstance) {
        mapInstance.remove();
        mapInstance = null;
      }
    };
  }, [lat, lng, hasCoords, store.name, address]);

  return (
    <section id="ubicacion" aria-labelledby="map-heading" className="scroll-mt-28 rounded-3xl border border-stone-200/90 bg-white p-5 sm:p-7 shadow-xs space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-stone-100">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-orange-100/70 text-[#ea580c] flex items-center justify-center">
            <MapPin className="w-4 h-4" />
          </div>
          <div>
            <h2 id="map-heading" className="font-display font-bold text-base sm:text-lg text-stone-900 leading-tight">
              Dónde encontrarnos
            </h2>
            <p className="text-xs text-stone-500">
              {store.pickupEnabled ? 'Consulta la ubicación para recoger tu pedido.' : 'Ubicación publicada por la tienda.'}
            </p>
          </div>
        </div>

        {/* Acciones de Navegación externa */}
        <div className="flex items-center gap-2">
          {address && <button
            onClick={copyAddress}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-stone-200 hover:border-stone-300 bg-stone-50 hover:bg-stone-100 text-stone-700 text-xs font-semibold transition-colors"
            title="Copiar dirección completa"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-stone-500" />}
            <span>{copied ? 'Copiada' : 'Copiar'}</span>
          </button>}

          {mapsUrl && <a
            href={mapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#005141] hover:bg-[#00382d] text-white text-xs font-semibold transition-colors shadow-xs"
            title="Abrir en Google Maps"
          >
            <Navigation className="w-3.5 h-3.5 text-[#feae2c]" />
            <span>Cómo llegar</span>
            <ExternalLink className="w-3 h-3 opacity-70" />
          </a>}

          {hasCoords && <a
            href={wazeUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-stone-200 hover:border-stone-300 bg-stone-50 hover:bg-stone-100 text-stone-700 text-xs font-semibold transition-colors"
            title="Abrir en Waze"
          >
            <span>Waze</span>
          </a>}
        </div>
      </div>

      {/* Contenedor del Mapa OpenStreetMap */}
      {hasCoords ? <div className="relative w-full h-56 sm:h-64 rounded-2xl overflow-hidden border border-stone-200/80 bg-stone-100 shadow-inner">
        <div ref={mapContainerRef} className="w-full h-full z-10" />

        {!mapLoaded && (
          <div className="absolute inset-0 flex items-center justify-center bg-stone-100 text-stone-400 text-xs gap-2">
            {!mapError && <span className="w-2 h-2 rounded-full bg-[#ea580c] animate-ping" />}
            <span>{mapError ? 'No pudimos cargar el mapa. Usa «Cómo llegar» para ver la ubicación.' : 'Cargando mapa...'}</span>
          </div>
        )}
      </div> : <p className="text-sm text-stone-500">La tienda aún no ha publicado su ubicación en el mapa.</p>}

      {/* Tarjeta de Resumen de Dirección */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3.5 rounded-2xl bg-[#faf7f2] border border-stone-200/70 text-xs">
        <div className="space-y-0.5">
          <p className="font-bold text-stone-800 flex items-center gap-1.5">
            <MapPin className="w-3.5 h-3.5 text-[#ea580c] shrink-0" />
            <span>{address || 'Dirección de la tienda no publicada'}</span>
          </p>
          {reference && <p className="text-stone-500 pl-5">Ref: {reference}</p>}
        </div>

        {store.openingHours && <span className="text-xs text-stone-600">{store.openingHours}</span>}
      </div>
    </section>
  );
}
