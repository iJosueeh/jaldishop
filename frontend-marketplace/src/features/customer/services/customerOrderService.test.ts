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
      it('reconsults GET detail when PATCH returns mismatched orderNumber and succeeds if backend shows CANCELLED (Scenario 1)', async () => {
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
              status: 'CANCELLED',
            }),
          });

        const res = await customerOrderService.cancelOrder('ORD-123');
        expect(res.orderNumber).toBe('ORD-123');
        expect(res.status).toBe('CANCELLED');
        expect(fetchMock).toHaveBeenCalledTimes(2);
      });

      it('indicates order remains confirmed in last query when PATCH returns mismatched orderNumber and GET shows CONFIRMED (Scenario 2)', async () => {
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
          'El pedido continúa en estado confirmado en la última consulta. No se dispone de confirmación sobre la cancelación. Por favor, actualiza la página más adelante para verificar el estado definitivo.'
        );
      });

      it('shows real status when PATCH returns status !== CANCELLED and GET confirms IN_PREPARATION (Scenario 3)', async () => {
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
              status: 'IN_PREPARATION',
            }),
          });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'El pedido figura actualmente en estado IN_PREPARATION. No se pudo confirmar la cancelación. Por favor, actualiza la página para revisar el detalle.'
        );
      });

      it('indicates status could not be determined when PATCH status !== CANCELLED and GET fails (Scenario 4 - does NOT claim CONFIRMED)', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'CONFIRMED',
            }),
          })
          .mockRejectedValueOnce(new TypeError('Failed to fetch'));

        const errorPromise = customerOrderService.cancelOrder('ORD-123');
        await expect(errorPromise).rejects.toThrow(
          'No se pudo determinar el estado actual del pedido debido a un problema de conexión. Por favor, actualiza la página para verificar el estado.'
        );
        await expect(errorPromise).rejects.not.toThrow(/continúa en estado confirmado/i);
      });
    });

    describe('Timeout & Uncertain State Handling', () => {
      it('recovers cancelled order when PATCH returns 503 timeout but backend finished cancellation (Scenario 1)', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: false,
            status: 503,
            json: async () => ({ message: 'Gateway timeout' }),
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

      it('indicates order remains confirmed in last query when PATCH returns 503 and order remains CONFIRMED (Scenario 2)', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: false,
            status: 503,
            json: async () => ({ message: 'Gateway timeout' }),
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
          'El pedido continúa en estado confirmado en la última consulta. No se dispone de confirmación sobre la cancelación. Por favor, actualiza la página más adelante para verificar el estado definitivo.'
        );
      });

      it('shows real status when PATCH returns 503 and order is already IN_PREPARATION (Scenario 3)', async () => {
        fetchMock
          .mockResolvedValueOnce({
            ok: false,
            status: 503,
            json: async () => ({ message: 'Gateway timeout' }),
          })
          .mockResolvedValueOnce({
            ok: true,
            json: async () => ({
              ...mockOrderDetail,
              orderNumber: 'ORD-123',
              status: 'IN_PREPARATION',
            }),
          });

        await expect(customerOrderService.cancelOrder('ORD-123')).rejects.toThrow(
          'El pedido figura actualmente en estado IN_PREPARATION. No se pudo confirmar la cancelación. Por favor, actualiza la página para revisar el detalle.'
        );
      });

      it('recovers cancelled order when fetch throws network error but backend finished cancellation (Scenario 1)', async () => {
        fetchMock
          .mockRejectedValueOnce(new TypeError('Failed to fetch (timeout)'))
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

      it('indicates status could not be determined when fetch throws network error and fallback GET also fails (Scenario 4 - does NOT claim CONFIRMED)', async () => {
        fetchMock
          .mockRejectedValueOnce(new TypeError('Failed to fetch'))
          .mockRejectedValueOnce(new TypeError('Failed to fetch'));

        const errorPromise = customerOrderService.cancelOrder('ORD-123');
        await expect(errorPromise).rejects.toThrow(
          'No se pudo determinar el estado actual del pedido debido a un problema de conexión. Por favor, actualiza la página para verificar el estado.'
        );
        await expect(errorPromise).rejects.not.toThrow(/continúa en estado confirmado/i);
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
