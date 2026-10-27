import {
  InitiateCheckoutPayload,
  CheckoutResponse,
} from '../types/checkout.types';

export const checkoutService = {
  async initiateCheckout(
    payload: InitiateCheckoutPayload
  ): Promise<CheckoutResponse> {
    const res = await fetch('/api/customer/checkout', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      const message =
        error.message ||
        'No fue posible iniciar el proceso de checkout. Verifica los datos.';
      const err = new Error(message) as Error & { code?: string; status?: number };
      err.code = error.code;
      err.status = res.status;
      throw err;
    }

    return res.json();
  },
};
