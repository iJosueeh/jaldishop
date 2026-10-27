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
};
