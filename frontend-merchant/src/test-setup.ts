/**
 * Global test environment setup for Vitest / Angular CLI.
 * Polyfills navigator properties (userAgent, platform, appVersion) and ResizeObserver for Node.js 21/22+ on Linux/CI.
 */
if (typeof globalThis !== 'undefined') {
  const nav: Record<string, any> = (globalThis.navigator || {}) as Record<string, any>;

  const defineProp = (prop: string, val: string) => {
    try {
      if (!nav[prop]) {
        Object.defineProperty(nav, prop, {
          value: val,
          configurable: true,
          writable: true,
        });
      }
    } catch {
      nav[prop] = val;
    }
  };

  defineProp('userAgent', 'Mozilla/5.0 (Node.js/Vitest)');
  defineProp('platform', 'Linux x86_64');
  defineProp('appVersion', '5.0 (Linux)');

  if (!globalThis.navigator) {
    (globalThis as any).navigator = nav;
  }

  if (typeof (globalThis as any).ResizeObserver === 'undefined') {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  }
}
