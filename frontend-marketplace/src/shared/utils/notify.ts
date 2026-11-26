import { toast, type ExternalToast } from 'sonner';

export type NotifySeverity = 'success' | 'info' | 'warning' | 'error';

const DEFAULT_DURATIONS: Record<NotifySeverity, number> = {
  success: 3500,
  info: 3500,
  warning: 4500,
  error: 6500,
};

export const notifyKeys = {
  cartAdd: (productId: string) => `cart-add-${productId}`,
  cartError: () => 'cart-error',
  cartAuth: () => 'cart-auth-required',
  cartOwnStore: () => 'cart-own-store',
  favorite: (productId: string) => `favorite-${productId}`,
  shareStore: (slug: string) => `share-store-${slug}`,
  shareProduct: (productId: string) => `share-product-${productId}`,
  whatsapp: () => 'whatsapp-bridge',
  checkoutError: () => 'checkout-error',
  mercadoPagoTab: () => 'mercadopago-tab',
  paymentConfirmed: (orderNumber: string) => `payment-confirmed-${orderNumber}`,
  authLogin: () => 'auth-login',
  authRegister: () => 'auth-register',
  profileError: () => 'profile-error',
  address: (action: 'limit' | 'save' | 'default' | 'delete') => `address-${action}`,
  review: (id?: string) => (id ? `review-${id}` : 'review-action'),
  analyticsReport: () => 'analytics-report',
  orderStatus: (orderNumber: string) => `order-status-${orderNumber}`,
  network: () => 'network-error',
} as const;

export interface NotifyOptions extends ExternalToast {
  id?: string | number;
  duration?: number;
}

function buildOptions(severity: NotifySeverity, options?: NotifyOptions): ExternalToast {
  return {
    ...options,
    duration: options?.duration ?? DEFAULT_DURATIONS[severity],
  };
}

/**
 * Wrapper centralizado sobre Sonner con deduplicación por `id` y duración
 * calibrada por severidad. Delega a `toast.*` para mantener compatibilidad
 * con pruebas unitarias que mockean `sonner`.
 */
export const notify = {
  success(message: string, options?: NotifyOptions) {
    return toast.success(message, buildOptions('success', options));
  },
  info(message: string, options?: NotifyOptions) {
    return toast.info(message, buildOptions('info', options));
  },
  warning(message: string, options?: NotifyOptions) {
    return toast.warning(message, buildOptions('warning', options));
  },
  error(message: string, options?: NotifyOptions) {
    return toast.error(message, buildOptions('error', options));
  },
  dismiss(id?: string | number) {
    return toast.dismiss(id);
  },
};
