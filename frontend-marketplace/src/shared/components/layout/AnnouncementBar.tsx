'use client';

import React, { useEffect, useState } from 'react';
import { Check, Copy, Tag, X } from 'lucide-react';

interface ActiveBannerData {
  active: boolean;
  code?: string | null;
  bannerText?: string | null;
}

export interface AnnouncementBarProps {
  onVisibilityChange?: (visible: boolean) => void;
}

const DISMISS_KEY_PREFIX = 'jaldishop_promo_banner_dismissed_';
const POLL_INTERVAL_MS = 60_000;

export function AnnouncementBar({ onVisibilityChange }: AnnouncementBarProps) {
  const [data, setData] = useState<ActiveBannerData | null>(null);
  const [dismissed, setDismissed] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);

  const isVisible = Boolean(data?.active && data.code && data.bannerText && !dismissed);

  useEffect(() => {
    onVisibilityChange?.(isVisible);
  }, [isVisible, onVisibilityChange]);

  useEffect(() => {
    let active = true;

    const loadBanner = () => {
      if (typeof document !== 'undefined' && document.hidden) {
        return;
      }
      fetch('/api/marketplace/active-banner', { cache: 'no-store' })
        .then((res) => (res.ok ? res.json() : { active: false }))
        .then((json: ActiveBannerData) => {
          if (!active) return;
          setData(json);
          if (!json?.active || !json.code) {
            setDismissed(true);
            return;
          }
          try {
            setDismissed(sessionStorage.getItem(`${DISMISS_KEY_PREFIX}${json.code}`) === '1');
          } catch {
            setDismissed(false);
          }
        })
        .catch(() => {
          if (active) setData({ active: false });
        });
    };

    const handleVisibilityOrFocus = () => {
      if (typeof document === 'undefined' || !document.hidden) {
        loadBanner();
      }
    };

    loadBanner();
    const intervalId = setInterval(loadBanner, POLL_INTERVAL_MS);
    window.addEventListener('focus', handleVisibilityOrFocus);
    document.addEventListener('visibilitychange', handleVisibilityOrFocus);

    return () => {
      active = false;
      clearInterval(intervalId);
      window.removeEventListener('focus', handleVisibilityOrFocus);
      document.removeEventListener('visibilitychange', handleVisibilityOrFocus);
    };
  }, []);

  if (!isVisible || !data?.code || !data?.bannerText) {
    return null;
  }

  const handleCopy = async () => {
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(data.code!);
      }
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore clipboard errors
    }
  };

  const handleDismiss = () => {
    try {
      sessionStorage.setItem(`${DISMISS_KEY_PREFIX}${data.code}`, '1');
    } catch {
      // Ignore storage errors
    }
    setDismissed(true);
  };

  return (
    <div
      role="region"
      aria-label="Promoción activa del marketplace"
      className="w-full h-10 bg-[#072720] text-white border-b border-[#1e2c31] transition-opacity duration-200 ease-out motion-reduce:transition-none"
    >
      <div className="max-w-[1440px] mx-auto h-full px-3 sm:px-6 flex items-center justify-center gap-2 sm:gap-3 text-xs">
        <div className="flex items-center gap-2 min-w-0">
          <Tag className="w-3.5 h-3.5 text-[#36f4a4] shrink-0" aria-hidden="true" />
          <p className="truncate font-medium text-stone-100">{data.bannerText}</p>
        </div>

        <button
          type="button"
          onClick={handleCopy}
          aria-label={copied ? `Código ${data.code} copiado` : `Copiar código ${data.code}`}
          className="inline-flex items-center gap-1.5 rounded-md bg-[#36f4a4] px-2.5 py-1 text-[11px] font-mono font-bold text-[#072720] hover:bg-[#5cf7b5] transition-colors duration-150 ease-out motion-reduce:transition-none shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#36f4a4]"
        >
          <span>{data.code}</span>
          {copied ? (
            <Check className="w-3 h-3 shrink-0" aria-hidden="true" />
          ) : (
            <Copy className="w-3 h-3 shrink-0" aria-hidden="true" />
          )}
        </button>

        <span className="sr-only" aria-live="polite">
          {copied ? `Código ${data.code} copiado al portapapeles.` : ''}
        </span>

        <button
          type="button"
          onClick={handleDismiss}
          aria-label="Cerrar anuncio promocional"
          className="ml-1 inline-flex items-center justify-center w-7 h-7 rounded-full text-[#99b3ad] hover:text-white hover:bg-white/10 transition-colors duration-150 shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#36f4a4]"
        >
          <X className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
