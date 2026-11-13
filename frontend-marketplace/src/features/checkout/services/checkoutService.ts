import {
  InitiateCheckoutPayload,
  CheckoutResponse,
  CreatePaymentPreferencePayload,
  CreatePaymentPreferenceResponse,
  PendingPaymentCorrelation,
  CustomerPaymentStatusResponse,
} from '../types/checkout.types';

export const PENDING_PAYMENT_STORAGE_KEY = 'jaldishop_pending_payment';

export interface PollPaymentOptions {
  maxAttempts?: number;
  intervalMs?: number;
  signal?: AbortSignal;
}

export class PaymentServiceError extends Error {
  code?: string;
  status?: number;

  constructor(message: string, code?: string, status?: number) {
    super(message);
    this.name = 'PaymentServiceError';
    this.code = code;
    this.status = status;
  }
}

/**
 * Validates that an initPoint URL belongs strictly to official Mercado Pago domains.
 * Prevents arbitrary open redirection vulnerabilities.
 */
export function isValidMercadoPagoUrl(urlString: string): boolean {
  if (!urlString || typeof urlString !== 'string') return false;
  try {
    const url = new URL(urlString.trim());
    if (url.protocol !== 'https:') return false;
    const hostname = url.hostname.toLowerCase();
    return (
      hostname === 'mercadopago.com' ||
      hostname.endsWith('.mercadopago.com') ||
      hostname === 'mercadopago.com.pe' ||
      hostname.endsWith('.mercadopago.com.pe')
    );
  } catch {
    return false;
  }
}

/**
 * Manages pending payment correlation in sessionStorage.
 */
export function savePendingPayment(correlation: PendingPaymentCorrelation): void {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.setItem(PENDING_PAYMENT_STORAGE_KEY, JSON.stringify(correlation));
  } catch (e) {
    console.warn('[checkoutService] No se pudo guardar la correlación de pago:', e);
  }
}

export function getPendingPayment(): PendingPaymentCorrelation | null {
  if (typeof window === 'undefined') return null;
  try {
    const stored = sessionStorage.getItem(PENDING_PAYMENT_STORAGE_KEY);
    if (!stored) return null;
    return JSON.parse(stored) as PendingPaymentCorrelation;
  } catch {
    return null;
  }
}

export function clearPendingPayment(): void {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.removeItem(PENDING_PAYMENT_STORAGE_KEY);
  } catch {
    // Ignore storage deletion errors
  }
}

// In-flight request map to prevent duplicate requests from double clicks / concurrent interactions
const inFlightPreferenceRequests = new Map<string, Promise<CreatePaymentPreferenceResponse>>();

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

  /**
   * Generates a real Mercado Pago checkout preference through the authenticated BFF.
   * Enforces in-flight deduplication, response validation, safe URL policy,
   * and correlation persistence.
   */
  async createPaymentPreference(
    payload: CreatePaymentPreferencePayload
  ): Promise<CreatePaymentPreferenceResponse> {
    const reservationKey = payload.capacityReservationId?.trim();
    if (!reservationKey) {
      throw new PaymentServiceError(
        'El ID de la reserva de capacidad es obligatorio para iniciar el pago.',
        'INVALID_RESERVATION_ID',
        400
      );
    }

    // Deduplicate concurrent requests for the same reservation
    const existingPromise = inFlightPreferenceRequests.get(reservationKey);
    if (existingPromise) {
      return existingPromise;
    }

    const requestPromise = (async () => {
      let res: Response;
      try {
        res = await fetch('/api/customer/checkout/mercado-pago/preference', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        });
      } catch {
        throw new PaymentServiceError(
          'Error de red al conectar con el servicio de pagos. Intenta nuevamente.',
          'NETWORK_ERROR',
          0
        );
      }

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        const message =
          errorData.message ||
          'No se pudo generar la sesión de pago. Intenta nuevamente.';
        throw new PaymentServiceError(message, errorData.code, res.status);
      }

      const data = await res.json().catch(() => null);
      if (
        !data ||
        typeof data.paymentId !== 'string' ||
        typeof data.preferenceId !== 'string' ||
        typeof data.initPoint !== 'string' ||
        typeof data.status !== 'string'
      ) {
        throw new PaymentServiceError(
          'Respuesta incompleta del proveedor de pagos.',
          'INVALID_PROVIDER_RESPONSE',
          502
        );
      }

      // Security check: validate redirection domain
      if (!isValidMercadoPagoUrl(data.initPoint)) {
        throw new PaymentServiceError(
          'La URL de pago generada no pertenece a un dominio seguro de Mercado Pago.',
          'INSECURE_INIT_POINT',
          502
        );
      }

      // Persist transaction correlation for return verification
      savePendingPayment({
        paymentId: data.paymentId,
        preferenceId: data.preferenceId,
        capacityReservationId: reservationKey,
        storeId: payload.storeId,
        storeName: payload.storeName,
        createdAt: Date.now(),
        status: data.status,
      });

      return data as CreatePaymentPreferenceResponse;
    })();

    inFlightPreferenceRequests.set(reservationKey, requestPromise);
    try {
      return await requestPromise;
    } finally {
      inFlightPreferenceRequests.delete(reservationKey);
    }
  },

  /**
   * Fetches the current payment verification status for a specific paymentId.
   */
  async getPaymentStatus(paymentId: string): Promise<CustomerPaymentStatusResponse> {
    const cleanId = paymentId?.trim();
    if (!cleanId) {
      throw new PaymentServiceError(
        'El identificador de pago es obligatorio.',
        'INVALID_PAYMENT_ID',
        400
      );
    }

    let res: Response;
    try {
      res = await fetch(`/api/customer/payments/${encodeURIComponent(cleanId)}/status`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
      });
    } catch {
      throw new PaymentServiceError(
        'Error de red al consultar el estado del pago. Intenta nuevamente.',
        'NETWORK_ERROR',
        0
      );
    }

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      const message =
        errorData.message ||
        'No se pudo obtener el estado del pago. Intenta nuevamente.';
      throw new PaymentServiceError(message, errorData.code, res.status);
    }

    const data = await res.json().catch(() => null);
    if (!data || typeof data.paymentId !== 'string' || typeof data.paymentStatus !== 'string') {
      throw new PaymentServiceError(
        'Respuesta inválida del servidor al consultar el estado del pago.',
        'INVALID_STATUS_RESPONSE',
        502
      );
    }

    return data as CustomerPaymentStatusResponse;
  },

  /**
   * Polls the payment status until terminal state or maximum attempts reached.
   * Terminal states: orderCreated === true, hasIncidence === true, paymentStatus === 'FAILED'.
   * When max attempts are reached without terminal state, returns the latest status
   * (does not fabricate an incidence or error).
   */
  async pollPaymentStatus(
    paymentId: string,
    options?: PollPaymentOptions
  ): Promise<CustomerPaymentStatusResponse> {
    const maxAttempts = options?.maxAttempts ?? 10;
    const intervalMs = options?.intervalMs ?? 2500;
    const signal = options?.signal;

    let latestStatus: CustomerPaymentStatusResponse | null = null;
    let lastError: unknown = null;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      if (signal?.aborted) {
        throw new DOMException('La verificación del pago fue cancelada.', 'AbortError');
      }

      try {
        const status = await this.getPaymentStatus(paymentId);
        latestStatus = status;

        // Terminal states: order created, incidence recorded, or payment failed
        if (status.orderCreated || status.hasIncidence || status.paymentStatus === 'FAILED') {
          return status;
        }
      } catch (err) {
        lastError = err;
        // If authentication error or client error (400, 401, 403, 404), do not keep polling
        if (
          err instanceof PaymentServiceError &&
          err.status &&
          [400, 401, 403, 404].includes(err.status)
        ) {
          throw err;
        }
      }

      if (attempt < maxAttempts) {
        await new Promise<void>((resolve, reject) => {
          if (signal?.aborted) {
            return reject(new DOMException('La verificación del pago fue cancelada.', 'AbortError'));
          }
          const timer = setTimeout(() => {
            if (signal) {
              signal.removeEventListener('abort', onAbort);
            }
            resolve();
          }, intervalMs);

          const onAbort = () => {
            clearTimeout(timer);
            if (signal) {
              signal.removeEventListener('abort', onAbort);
            }
            reject(new DOMException('La verificación del pago fue cancelada.', 'AbortError'));
          };

          if (signal) {
            signal.addEventListener('abort', onAbort, { once: true });
          }
        });
      }
    }

    if (latestStatus) {
      return latestStatus;
    }

    if (lastError) {
      throw lastError;
    }

    throw new PaymentServiceError(
      'No se pudo determinar el estado del pago dentro del tiempo límite.',
      'POLL_TIMEOUT',
      408
    );
  },
};
