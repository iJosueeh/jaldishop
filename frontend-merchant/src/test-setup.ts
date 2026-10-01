/**
 * Global test environment setup for Vitest / Angular CLI.
 * Polyfills navigator.userAgent and ResizeObserver for Node.js 21/22+ environments on Linux/CI.
 */
if (typeof globalThis !== 'undefined') {
  if (!globalThis.navigator) {
    (globalThis as any).navigator = { userAgent: 'Mozilla/5.0 (Node.js/Vitest)' };
  } else if (!globalThis.navigator.userAgent) {
    try {
      Object.defineProperty(globalThis.navigator, 'userAgent', {
        value: 'Mozilla/5.0 (Node.js/Vitest)',
        configurable: true,
        writable: true,
      });
    } catch {
      (globalThis.navigator as any).userAgent = 'Mozilla/5.0 (Node.js/Vitest)';
    }
  }

  if (typeof (globalThis as any).ResizeObserver === 'undefined') {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  }
}
