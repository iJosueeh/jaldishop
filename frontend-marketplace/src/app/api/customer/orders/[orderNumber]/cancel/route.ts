import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

interface RouteContext {
  params: Promise<{
    orderNumber: string;
  }>;
}

/**
 * BFF Endpoint for customer order cancellation:
 * PATCH /api/customer/orders/[orderNumber]/cancel -> Spring /api/v1/customer/orders/[orderNumber]/cancel
 */
export async function PATCH(request: NextRequest, context: RouteContext) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { orderNumber } = await context.params;
  if (!orderNumber || !orderNumber.trim()) {
    return NextResponse.json({ message: 'Número de pedido inválido.' }, { status: 400 });
  }

  let reason: string | undefined;

  try {
    const text = await request.text();
    if (text && text.trim().length > 0) {
      const parsed = JSON.parse(text);
      if (parsed && typeof parsed === 'object') {
        if (typeof parsed.reason === 'string') {
          reason = parsed.reason;
        } else if (parsed.reason !== undefined && parsed.reason !== null) {
          return NextResponse.json(
            { message: 'El formato del motivo de cancelación no es válido.' },
            { status: 400 }
          );
        }
      }
    }
  } catch {
    return NextResponse.json(
      { message: 'El cuerpo de la solicitud no tiene un formato JSON válido.' },
      { status: 400 }
    );
  }

  if (reason && reason.length > 255) {
    return NextResponse.json(
      { message: 'El motivo de cancelación no puede superar los 255 caracteres.' },
      { status: 400 }
    );
  }

  try {
    const upstream = await fetch(
      `${env.apiUrl}/customer/orders/${encodeURIComponent(orderNumber.trim())}/cancel`,
      {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(reason !== undefined ? { reason: reason.trim() } : {}),
        cache: 'no-store',
        signal: AbortSignal.timeout(10000),
      }
    );

    const data = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos procesar la cancelación en este momento. Inténtalo nuevamente.' },
      { status: 503 }
    );
  }
}
