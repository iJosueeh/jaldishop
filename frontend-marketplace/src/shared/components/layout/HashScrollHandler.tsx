'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';

/**
 * Manages smooth, reliable scrolling to anchor targets in Next.js 15 App Router.
 * Handles both intra-page navigation and transitions arriving from subpages.
 */
export function HashScrollHandler() {
  const pathname = usePathname();

  useEffect(() => {
    // Only execute on client side
    if (typeof window === 'undefined') return;

    const handleHashScroll = () => {
      const hash = window.location.hash.replace('#', '');
      if (!hash) return;

      const element = document.getElementById(hash);
      if (element) {
        // Small delay to allow Next.js hydration and image layout to stabilize
        setTimeout(() => {
          element.scrollIntoView({ behavior: 'smooth' });
        }, 100);
      }
    };

    // Run on initial route mount or change
    handleHashScroll();

    // Listen to manual hash changes (e.g. back/forward button)
    window.addEventListener('hashchange', handleHashScroll);
    return () => window.removeEventListener('hashchange', handleHashScroll);
  }, [pathname]);

  return null;
}
