import { CustomerOrderDetail, CustomerOrderSummary } from '../types/customer.types';

export const customerOrderService = {
  async getOrders(): Promise<CustomerOrderSummary[]> {
    const res = await fetch('/api/customer/orders', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible cargar tus pedidos.');
    }

    return res.json();
  },

  async getOrderDetail(orderNumber: string): Promise<CustomerOrderDetail> {
    const res = await fetch(`/api/customer/orders/${encodeURIComponent(orderNumber)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible cargar el detalle del pedido.');
    }

    return res.json();
  },

  async cancelOrder(orderNumber: string, reason?: string): Promise<CustomerOrderDetail> {
    const res = await fetch(`/api/customer/orders/${encodeURIComponent(orderNumber.trim())}/cancel`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(reason !== undefined ? { reason } : {}),
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      const message =
        error.message ||
        (res.status === 401
          ? 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.'
          : res.status === 403
          ? 'No tienes permisos para cancelar este pedido.'
          : res.status === 404
          ? 'El pedido solicitado no fue encontrado.'
          : res.status === 422
          ? (error.message || 'El pedido no puede ser cancelado en su estado actual.')
          : res.status === 409
          ? (error.message || 'Conflicto al procesar la cancelación del pedido.')
          : res.status === 400
          ? (error.message || 'Los datos de la solicitud no son válidos.')
          : 'No fue posible cancelar el pedido en este momento. Inténtalo nuevamente.');
      const err = new Error(message) as Error & { status?: number; code?: string };
      err.status = res.status;
      err.code = typeof error.code === 'string' ? error.code : undefined;
      throw err;
    }

    return res.json();
  },
};
