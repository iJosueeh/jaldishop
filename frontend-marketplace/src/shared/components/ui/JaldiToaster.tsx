'use client';

import React, { useEffect, useState } from 'react';
import { Toaster } from 'sonner';
import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';

const MOBILE_MEDIA_QUERY = '(max-width: 767px)';
const REDUCED_MOTION_MEDIA_QUERY = '(prefers-reduced-motion: reduce)';

export function JaldiToaster() {
  const [isMobile, setIsMobile] = useState(false);
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }

    const mobileMq = window.matchMedia(MOBILE_MEDIA_QUERY);
    const motionMq = window.matchMedia(REDUCED_MOTION_MEDIA_QUERY);

    const syncMobile = () => setIsMobile(mobileMq.matches);
    const syncMotion = () => setPrefersReducedMotion(motionMq.matches);

    syncMobile();
    syncMotion();

    mobileMq.addEventListener('change', syncMobile);
    motionMq.addEventListener('change', syncMotion);

    return () => {
      mobileMq.removeEventListener('change', syncMobile);
      motionMq.removeEventListener('change', syncMotion);
    };
  }, []);

  return (
    <Toaster
      position={isMobile ? 'top-center' : 'top-right'}
      visibleToasts={3}
      closeButton
      expand={false}
      gap={10}
      offset={{ top: 80, right: 16 }}
      mobileOffset={{ top: 76, left: 12, right: 12 }}
      data-reduced-motion={prefersReducedMotion ? 'true' : 'false'}
      icons={{
        success: (
          <span
            aria-hidden="true"
            className="w-7 h-7 rounded-lg bg-[#f0fdfa] text-[#005141] inline-flex items-center justify-center shrink-0"
          >
            <CheckCircle2 className="w-4 h-4" />
          </span>
        ),
        info: (
          <span
            aria-hidden="true"
            className="w-7 h-7 rounded-lg bg-[#f7f3ec] text-[#072720] inline-flex items-center justify-center shrink-0"
          >
            <Info className="w-4 h-4" />
          </span>
        ),
        warning: (
          <span
            aria-hidden="true"
            className="w-7 h-7 rounded-lg bg-[#fffbeb] text-[#92400e] inline-flex items-center justify-center shrink-0"
          >
            <AlertTriangle className="w-4 h-4" />
          </span>
        ),
        error: (
          <span
            aria-hidden="true"
            className="w-7 h-7 rounded-lg bg-[#fff1f2] text-[#be123c] inline-flex items-center justify-center shrink-0"
          >
            <XCircle className="w-4 h-4" />
          </span>
        ),
      }}
      toastOptions={{
        unstyled: false,
        classNames: {
          toast:
            'jaldi-toast font-sans bg-white text-[#1c1917] border border-[#e7e0d6] rounded-xl shadow-[0_4px_14px_rgba(2,9,10,0.08)] px-3.5 py-3 gap-3 items-start',
          title: 'text-sm font-semibold text-[#1c1917] leading-snug',
          description: 'text-[13px] text-[#57534e] leading-snug mt-0.5',
          actionButton:
            'bg-[#005141] hover:bg-[#072720] text-white text-xs font-semibold rounded-full px-3 py-1.5 transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
          cancelButton:
            'bg-[#faf7f2] hover:bg-[#f0eae0] text-[#1c1917] border border-[#e7e0d6] text-xs font-semibold rounded-full px-3 py-1.5 transition-colors duration-150',
          closeButton:
            'bg-white text-[#57534e] hover:text-[#1c1917] hover:bg-[#faf7f2] border border-[#e7e0d6] rounded-full transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]',
        },
      }}
    />
  );
}
