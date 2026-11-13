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
    const sanitizedOrderNumber = orderNumber.trim();
    let res: Response;

    try {
      res = await fetch(`/api/customer/orders/${encodeURIComponent(sanitizedOrderNumber)}/cancel`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(reason !== undefined ? { reason } : {}),
        cache: 'no-store',
      });
    } catch {
      // Network failure or timeout during PATCH: handle as uncertain state
      return this.handleUncertainCancellation(sanitizedOrderNumber);
    }

    if (res.ok) {
      const data = (await res.json().catch(() => null)) as CustomerOrderDetail | null;
      const isMatching =
        data &&
        typeof data.orderNumber === 'string' &&
        data.orderNumber.trim().toLowerCase() === sanitizedOrderNumber.toLowerCase();
      const isCancelled = data && data.status === 'CANCELLED';

      if (isMatching && isCancelled) {
        return data;
      }

      // Inconsistent response DTO: do not invent success; reconsult backend
      try {
        const verified = await this.getOrderDetail(sanitizedOrderNumber);
        if (
          verified &&
          verified.orderNumber?.trim().toLowerCase() === sanitizedOrderNumber.toLowerCase() &&
          verified.status === 'CANCELLED'
        ) {
          return verified;
        }
      } catch {
        // Fallback GET failed
      }

      throw new Error(
        'La respuesta de cancelación fue inconsistente. El pedido no figura como cancelado. Por favor, actualiza la página para revisar el estado.'
      );
    }

    // 503 or transient upstream timeout: check if backend actually cancelled
    if (res.status === 503) {
      return this.handleUncertainCancellation(sanitizedOrderNumber);
    }

    // Deterministic errors from backend: 400, 401, 403, 404, 409, 422
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
  },

  async handleUncertainCancellation(orderNumber: string): Promise<CustomerOrderDetail> {
    try {
      const verified = await this.getOrderDetail(orderNumber);
      if (
        verified &&
        verified.orderNumber?.trim().toLowerCase() === orderNumber.trim().toLowerCase() &&
        verified.status === 'CANCELLED'
      ) {
        return verified;
      }
    } catch {
      // Re-query failed
    }

    const uncertainErr = new Error(
      'No pudimos confirmar la cancelación debido a un problema de conexión. El pedido continúa en estado confirmado. Por favor, actualiza la vista antes de volver a intentarlo.'
    ) as Error & { status?: number; code?: string; isUncertain?: boolean };
    uncertainErr.status = 503;
    uncertainErr.code = 'UNCERTAIN_CANCELLATION';
    uncertainErr.isUncertain = true;
    throw uncertainErr;
  },
};
