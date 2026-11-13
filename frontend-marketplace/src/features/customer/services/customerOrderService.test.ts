import { beforeEach, describe, expect, it, vi } from 'vitest';
import { customerOrderService } from './customerOrderService';
import { CustomerOrderDetail } from '../types/customer.types';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

const mockOrderDetail: CustomerOrderDetail = {
  id: 'uuid-1',
  orderNumber: 'ORD-123',
  store: { id: 's1', name: 'Tienda', slug: 'tienda' },
  status: 'CONFIRMED',
  deliveryMode: 'DELIVERY',
  serviceDate: '2026-11-14',
  serviceStartTime: '10:00:00',
  serviceEndTime: '11:00:00',
  customerName: 'Juan',
  customerPhone: '999',
  customerEmail: 'juan@example.com',
  currency: 'PEN',
  productsSubtotalAmount: 30,
  discountAmount: 0,
  deliveryFeeAmount: 5,
  includedTaxAmount: 4,
  totalAmount: 35,
  confirmedAt: '2026-11-13T10:00:00Z',
  updatedAt: '2026-11-13T10:00:00Z',
  items: [],
  statusHistory: [],
};

describe('customerOrderService', () => {
  describe('getOrders', () => {
    it('fetches customer orders list successfully', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => [{ orderNumber: 'ORD-1', status: 'CONFIRMED' }],
      });

      const orders = await customerOrderService.getOrders();
      expect(orders).toHaveLength(1);
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/customer/orders',
        expect.objectContaining({ method: 'GET' })
      );
    });

    it('throws error when getOrders fails', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        json: async () => ({ message: 'Error de servidor' }),
      });

      await expect(customerOrderService.getOrders()).rejects.toThrow('Error de servidor');
    });
  });

  describe('getOrderDetail', () => {
    it('fetches customer order detail successfully', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({ orderNumber: 'ORD-1', status: 'CONFIRMED' }),
      });

      const order = await customerOrderService.getOrderDetail('ORD-1');
      expect(order.orderNumber).toBe('ORD-1');
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/customer/orders/ORD-1',
        expect.objectContaining({ method: 'GET' })
      );
    });

    it('throws error when getOrderDetail fails', async () => {
      fetchMock.mockResolvedValue({
        ok: false,
        json: async () => ({ message: 'Pedido no encontrado' }),
      });

      await expect(customerOrderService.getOrderDetail('ORD-999')).rejects.toThrow(
        'Pedido no encontrado'
      );
    });
  });

  describe('cancelOrder', () => {
    it('calls PATCH /api/customer/orders/[orderNumber]/cancel and returns confirmed cancelled order', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({
          ...mockOrderDetail,
          orderNumber: 'ORD-123',
          status: 'CANCELLED',
        }),
      });

      const res = await customerOrderService.cancelOrder('ORD-123', 'Motivo del cliente');
      expect(res.status).toBe('CANCELLED');
      expect(res.orderNumber).toBe('ORD-123');
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/customer/orders/ORD-123/cancel',
        expect.objectContaining({
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ reason: 'Motivo del cliente' }),
        })
      );
    });

    it('calls PATCH with empty body when reason is omitted', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({
          ...mockOrderDetail,
          orderNumber: 'ORD-123',
          status: 'CANCELLED',
        }),
      });

      const res = await customerOrderService.cancelOrder('ORD-123');
      expect(res.status).toBe('CANCELLED');
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/customer/orders/ORD-123/cancel',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({}),
        })
      );
    });

    describe('Response Validation & Inconsistency Recovery', () => {
      it('reconsults GET detail when PATCH returns mismatched orderNumber and succeeds if backend shows CANCELLED', async () => {
        // PATCH returns order ORD-999 (mismatched)
        fetchMock
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-999',
              status: 'CANCELLED',
            }),
          })
          // Fallback GET /api/customer/orders/ORD-123
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CANCELLED',
            }),
          });

        const res = await customerOrderService.cancelOrder('ORD-123');
        expect(res.orderNumber).toBe('ORD-123');
        expect(res.status).toBe('CANCELLED');
        expect(fetchMock).toHaveBeenCalledTimes(2);
      });

      it('throws inconsistent error when PATCH returns mismatched orderNumber and fallback GET shows not CANCELLED', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-999',
              status: 'CANCELLED',
            }),
          })
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CONFIRMED',
            }),
          });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'La respuesta de cancelación fue inconsistente. El pedido no figura como cancelado. Por favor, actualiza la página para revisar el estado.'
        );
      });

      it('reconsults GET detail when PATCH returns status !== CANCELLED and recovers if backend shows CANCELLED', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'IN_PREPARATION',
            }),
          })
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CANCELLED',
            }),
          });

        const res = await customerOrderService.cancelOrder('ORD-123');
        expect(res.status).toBe('CANCELLED');
      });

      it('throws inconsistent error when PATCH status !== CANCELLED and fallback GET also confirms not CANCELLED', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CONFIRMED',
            }),
          })
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CONFIRMED',
            }),
          });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'La respuesta de cancelación fue inconsistente. El pedido no figura como cancelado. Por favor, actualiza la página para revisar el estado.'
        );
      });
    });

    describe('Timeout & Uncertain State Handling', () => {
      it('recovers cancelled order when PATCH returns 503 timeout but backend finished cancellation (GET CANCELLED)', async () => {
        // PATCH 503
        fetchMock
          .mockResolvedValueOnce({
            ok: false,
            status: 503,
            json: async () => ({ message: 'Gateway timeout' }),
          })
          // Fallback GET shows CANCELLED
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CANCELLED',
            }),
          });

        const res = await customerOrderService.cancelOrder('ORD-123');
        expect(res.status).toBe('CANCELLED');
      });

      it('throws informative uncertain error when PATCH returns 503 and order remains CONFIRMED', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: false,
            status: 503,
            json: async () => ({ message: 'Gateway timeout' }),
          })
          // Fallback GET shows order is still CONFIRMED
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CONFIRMED',
            }),
          });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'No pudimos confirmar la cancelación debido a un problema de conexión. El pedido continúa en estado confirmado. Por favor, actualiza la vista antes de volver a intentarlo.'
        );
      });

      it('recovers cancelled order when fetch throws network error but backend finished cancellation (GET CANCELLED)', async () => {
        // Fetch throws network error
        fetchMock
          .mockRejectedValueOnce(new TypeError('Failed to fetch (timeout)'))
          // Fallback GET succeeds with CANCELLED
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CANCELLED',
            }),
          });

        const res = await customerOrderService.cancelOrder('ORD-123');
        expect(res.status).toBe('CANCELLED');
      });

      it('throws uncertain error when fetch throws network error and fallback GET also fails', async () => {
        fetchMock
          .mockRejectedValueOnce(new TypeError('Failed to fetch'))
          .mockRejectedValueOnce(new TypeError('Failed to fetch'));

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'No pudimos confirmar la cancelación debido a un problema de conexión. El pedido continúa en estado confirmado. Por favor, actualiza la vista antes de volver a intentarlo.'
        );
      });
    });

    describe('Deterministic Backend Exceptions', () => {
      it('handles 401 unauthorized session expiration', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 401,
          json: async () => ({}),
        });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.'
        );
      });

      it('handles 403 access denied (pedido ajeno)', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 403,
          json: async () => ({ message: 'No tiene permisos para cancelar un pedido ajeno.' }),
        });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'No tiene permisos para cancelar un pedido ajeno.'
        );
      });

      it('handles 404 resource not found', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 404,
          json: async () => ({}),
        });

        await expect(customerOrderService.cancelOrder('ORD-999')).rejects.toThrow(
          'El pedido solicitado no fue encontrado.'
        );
      });

      it('handles 422 business rule exception (RN-ORD-09)', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 422,
          json: async () => ({
            code: 'RN-ORD-09',
            message: 'El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.',
          }),
        });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.'
        );
      });

      it('handles 409 conflict exception', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 409,
          json: async () => ({
            message: 'Conflicto en la orden.',
          }),
        });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'Conflicto en la orden.'
        );
      });

      it('handles 400 validation error', async () => {
        fetchMock.mockResolvedValue({
          ok: false,
          status: 400,
          json: async () => ({
            message: 'El motivo de cancelación no puede superar los 255 caracteres.',
          }),
        });

        await expect(customerOrderService.cancelOrder('ORD-123', 'x'.repeat(256))).rejects.toThrow(
          'El motivo de cancelación no puede superar los 255 caracteres.'
        );
      });
    });
  });
});
