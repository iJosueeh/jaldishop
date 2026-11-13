import { beforeEach, describe, expect, it, vi } from 'vitest';
import { customerOrderService } from './customerOrderService';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

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
    it('calls PATCH /api/customer/orders/[orderNumber]/cancel with reason', async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({
          orderNumber: 'ORD-123',
          status: 'CANCELLED',
        }),
      });

      const res = await customerOrderService.cancelOrder('ORD-123', 'Motivo del cliente');
      expect(res.status).toBe('CANCELLED');
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
