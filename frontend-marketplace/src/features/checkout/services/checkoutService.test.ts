import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  checkoutService,
  isValidMercadoPagoUrl,
  PaymentServiceError,
  savePendingPayment,
  getPendingPayment,
  clearPendingPayment,
  PENDING_PAYMENT_STORAGE_KEY,
} from './checkoutService';
import { CreatePaymentPreferencePayload } from '../types/checkout.types';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
  if (typeof window !== 'undefined') {
    sessionStorage.clear();
  }
});

describe('checkoutService & Payment Helpers', () => {
  describe('isValidMercadoPagoUrl', () => {
    it('accepts valid HTTPS URLs from official Mercado Pago domains', () => {
      expect(isValidMercadoPagoUrl('https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=123')).toBe(true);
      expect(isValidMercadoPagoUrl('https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=456')).toBe(true);
      expect(isValidMercadoPagoUrl('https://mercadopago.com.pe/checkout/orders/ORD-001')).toBe(true);
      expect(isValidMercadoPagoUrl('https://www.mercadopago.com/checkout/v1/redirect?pref_id=789')).toBe(true);
      expect(isValidMercadoPagoUrl('https://mercadopago.com/init/ord123')).toBe(true);
    });

    it('rejects non-HTTPS URLs', () => {
      expect(isValidMercadoPagoUrl('http://www.mercadopago.com.pe/checkout')).toBe(false);
    });

    it('rejects untrusted or look-alike phishing domains', () => {
      expect(isValidMercadoPagoUrl('https://evil-mercadopago.com/checkout')).toBe(false);
      expect(isValidMercadoPagoUrl('https://mercadopago.com.pe.attacker.com/checkout')).toBe(false);
      expect(isValidMercadoPagoUrl('https://attacker.com/mercadopago.com')).toBe(false);
      expect(isValidMercadoPagoUrl('javascript:alert(1)')).toBe(false);
      expect(isValidMercadoPagoUrl('')).toBe(false);
    });
  });

  describe('Pending payment sessionStorage correlation', () => {
    it('saves, retrieves, and clears pending payment in sessionStorage', () => {
      const correlation = {
        paymentId: 'pay-uuid-1',
        preferenceId: 'pref-abc',
        capacityReservationId: 'res-uuid-1',
        storeId: 'store-uuid-1',
        storeName: 'Bodega Central',
        createdAt: 1731500000000,
        status: 'PENDING',
      };

      savePendingPayment(correlation);
      expect(sessionStorage.getItem(PENDING_PAYMENT_STORAGE_KEY)).toBeTruthy();

      const retrieved = getPendingPayment();
      expect(retrieved).toEqual(correlation);

      clearPendingPayment();
      expect(getPendingPayment()).toBeNull();
    });
  });

  describe('createPaymentPreference', () => {
    const validPayload: CreatePaymentPreferencePayload = {
      capacityReservationId: 'b7f0c5a2-3f1d-4e92-8a9d-5a7c2b3e4f50',
      fulfillmentType: 'DELIVERY',
      customerName: 'Rosa Flores',
      customerPhone: '987654321',
      customerEmail: 'rosa@example.com',
      storeId: 'store-1',
      storeName: 'Bodega Central',
    };

    it('throws PaymentServiceError if capacityReservationId is missing', async () => {
      await expect(
        checkoutService.createPaymentPreference({
          ...validPayload,
          capacityReservationId: '',
        })
      ).rejects.toThrow(PaymentServiceError);
    });

    it('returns validated payment preference and saves correlation on success', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: 'pay-123',
            preferenceId: 'pref-456',
            initPoint: 'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-456',
            status: 'PENDING',
          }),
          { status: 201 }
        )
      );

      const result = await checkoutService.createPaymentPreference(validPayload);
      expect(result.paymentId).toBe('pay-123');
      expect(result.preferenceId).toBe('pref-456');
      expect(result.initPoint).toBe('https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-456');

      // Verify correlation was saved in sessionStorage
      const saved = getPendingPayment();
      expect(saved?.paymentId).toBe('pay-123');
      expect(saved?.preferenceId).toBe('pref-456');
      expect(saved?.capacityReservationId).toBe(validPayload.capacityReservationId);
    });

    it('throws Network error without retrying when fetch fails', async () => {
      fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'));

      await expect(
        checkoutService.createPaymentPreference(validPayload)
      ).rejects.toMatchObject({
        code: 'NETWORK_ERROR',
      });

      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('throws typed error with backend status and message on HTTP failure', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: 'RESERVATION_EXPIRED',
            message: 'La reserva ha caducado.',
          }),
          { status: 409 }
        )
      );

      await expect(
        checkoutService.createPaymentPreference(validPayload)
      ).rejects.toMatchObject({
        code: 'RESERVATION_EXPIRED',
        status: 409,
        message: 'La reserva ha caducado.',
      });
    });

    it('rejects incomplete response from provider (missing fields)', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: 'pay-123',
            // preferenceId, initPoint missing
          }),
          { status: 200 }
        )
      );

      await expect(
        checkoutService.createPaymentPreference(validPayload)
      ).rejects.toMatchObject({
        code: 'INVALID_PROVIDER_RESPONSE',
        status: 502,
      });
    });

    it('rejects initPoint pointing to an untrusted domain', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: 'pay-123',
            preferenceId: 'pref-456',
            initPoint: 'https://attacker.com/fake-checkout',
            status: 'PENDING',
          }),
          { status: 200 }
        )
      );

      await expect(
        checkoutService.createPaymentPreference(validPayload)
      ).rejects.toMatchObject({
        code: 'INSECURE_INIT_POINT',
        status: 502,
      });
    });

    it('deduplicates concurrent calls for the same reservation (in-flight guard)', async () => {
      let resolveFetch: (value: Response) => void;
      const deferredPromise = new Promise<Response>((resolve) => {
        resolveFetch = resolve;
      });

      fetchMock.mockReturnValueOnce(deferredPromise);

      // Trigger two concurrent requests simultaneously for the same reservation
      const call1 = checkoutService.createPaymentPreference(validPayload);
      const call2 = checkoutService.createPaymentPreference(validPayload);

      // Resolve the single network call
      resolveFetch!(
        new Response(
          JSON.stringify({
            paymentId: 'pay-123',
            preferenceId: 'pref-456',
            initPoint: 'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-456',
            status: 'PENDING',
          }),
          { status: 201 }
        )
      );

      const [res1, res2] = await Promise.all([call1, call2]);
      expect(res1).toEqual(res2);
      expect(fetchMock).toHaveBeenCalledTimes(1); // Only 1 fetch executed!
    });
  });

  describe('getPaymentStatus', () => {
    const validPaymentId = '6c318881-8051-4f32-be43-851f5e8dae41';

    it('throws error when paymentId is empty', async () => {
      await expect(checkoutService.getPaymentStatus('')).rejects.toThrow(PaymentServiceError);
    });

    it('returns typed response when backend returns payment status', async () => {
      const mockStatus = {
        paymentId: validPaymentId,
        paymentStatus: 'APPROVED',
        orderCreated: true,
        orderNumber: 'ORD-999',
        orderStatus: 'CONFIRMED',
        hasIncidence: false,
        incidenceCode: null,
        incidenceMessage: null,
        refundStatus: 'NOT_REQUIRED',
        amount: 45.5,
        currency: 'PEN',
        createdAt: '2026-11-13T10:00:00Z',
        approvedAt: '2026-11-13T10:01:00Z',
      };

      fetchMock.mockResolvedValueOnce(
        new Response(JSON.stringify(mockStatus), { status: 200 })
      );

      const res = await checkoutService.getPaymentStatus(validPaymentId);
      expect(res.paymentId).toBe(validPaymentId);
      expect(res.orderCreated).toBe(true);
      expect(res.orderNumber).toBe('ORD-999');
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/customer/payments/${validPaymentId}/status`,
        expect.objectContaining({ method: 'GET' })
      );
    });

    it('throws Network error on network connection error', async () => {
      fetchMock.mockRejectedValueOnce(new TypeError('Network offline'));

      await expect(checkoutService.getPaymentStatus(validPaymentId)).rejects.toMatchObject({
        code: 'NETWORK_ERROR',
        status: 0,
      });
    });

    it('throws PaymentServiceError with upstream status on error', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({ code: 'PAYMENT_NOT_FOUND', message: 'No encontrado' }),
          { status: 404 }
        )
      );

      await expect(checkoutService.getPaymentStatus(validPaymentId)).rejects.toMatchObject({
        code: 'PAYMENT_NOT_FOUND',
        status: 404,
      });
    });

    it('throws 502 when response body is not valid status format', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(JSON.stringify({ unexpected: 'shape' }), { status: 200 })
      );

      await expect(checkoutService.getPaymentStatus(validPaymentId)).rejects.toMatchObject({
        code: 'INVALID_STATUS_RESPONSE',
        status: 502,
      });
    });
  });

  describe('pollPaymentStatus', () => {
    const validPaymentId = '6c318881-8051-4f32-be43-851f5e8dae41';

    it('resolves immediately when order is created', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'APPROVED',
            orderCreated: true,
            orderNumber: 'ORD-101',
            orderStatus: 'CONFIRMED',
            hasIncidence: false,
          }),
          { status: 200 }
        )
      );

      const status = await checkoutService.pollPaymentStatus(validPaymentId, {
        maxAttempts: 3,
        intervalMs: 10,
      });

      expect(status.orderCreated).toBe(true);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('resolves immediately when incidence is detected', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'APPROVED',
            orderCreated: false,
            orderNumber: null,
            orderStatus: null,
            hasIncidence: true,
            incidenceCode: 'CAPACITY_RESERVATION_EXPIRED',
          }),
          { status: 200 }
        )
      );

      const status = await checkoutService.pollPaymentStatus(validPaymentId, {
        maxAttempts: 3,
        intervalMs: 10,
      });

      expect(status.hasIncidence).toBe(true);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('resolves immediately when paymentStatus is FAILED', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'FAILED',
            orderCreated: false,
            orderNumber: null,
            orderStatus: null,
            hasIncidence: false,
          }),
          { status: 200 }
        )
      );

      const status = await checkoutService.pollPaymentStatus(validPaymentId, {
        maxAttempts: 3,
        intervalMs: 10,
      });

      expect(status.paymentStatus).toBe('FAILED');
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('polls until order is created on subsequent attempts', async () => {
      // First attempt: PENDING
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'PENDING',
            orderCreated: false,
            orderNumber: null,
            orderStatus: null,
            hasIncidence: false,
          }),
          { status: 200 }
        )
      );

      // Second attempt: APPROVED with order
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'APPROVED',
            orderCreated: true,
            orderNumber: 'ORD-202',
            orderStatus: 'CONFIRMED',
            hasIncidence: false,
          }),
          { status: 200 }
        )
      );

      const status = await checkoutService.pollPaymentStatus(validPaymentId, {
        maxAttempts: 3,
        intervalMs: 10,
      });

      expect(status.orderCreated).toBe(true);
      expect(status.orderNumber).toBe('ORD-202');
      expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('returns latest status when maxAttempts are exhausted without terminal state (no fake incidence)', async () => {
      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            paymentId: validPaymentId,
            paymentStatus: 'PENDING',
            orderCreated: false,
            orderNumber: null,
            orderStatus: null,
            hasIncidence: false,
          }),
          { status: 200 }
        )
      );

      const status = await checkoutService.pollPaymentStatus(validPaymentId, {
        maxAttempts: 2,
        intervalMs: 10,
      });

      expect(status.paymentStatus).toBe('PENDING');
      expect(status.orderCreated).toBe(false);
      expect(status.hasIncidence).toBe(false);
      expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    it('stops polling immediately on 401/403/404 errors', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({ code: 'SESSION_EXPIRED', message: 'Expiró sesión' }),
          { status: 401 }
        )
      );

      await expect(
        checkoutService.pollPaymentStatus(validPaymentId, {
          maxAttempts: 5,
          intervalMs: 10,
        })
      ).rejects.toMatchObject({
        code: 'SESSION_EXPIRED',
        status: 401,
      });

      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('aborts polling when AbortSignal is fired', async () => {
      const controller = new AbortController();
      controller.abort();

      await expect(
        checkoutService.pollPaymentStatus(validPaymentId, {
          maxAttempts: 5,
          intervalMs: 10,
          signal: controller.signal,
        })
      ).rejects.toMatchObject({
        name: 'AbortError',
      });
    });

    it('propagates AbortSignal to in-flight getPaymentStatus fetch', async () => {
      const controller = new AbortController();
      fetchMock.mockImplementationOnce((_url, init) => {
        return new Promise((_, reject) => {
          init?.signal?.addEventListener('abort', () => {
            reject(new DOMException('This operation was aborted', 'AbortError'));
          });
        });
      });

      const promise = checkoutService.getPaymentStatus(validPaymentId, {
        signal: controller.signal,
      });
      controller.abort();

      await expect(promise).rejects.toMatchObject({
        name: 'AbortError',
      });
    });
  });
});
