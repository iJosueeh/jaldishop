import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

interface RouteContext {
  params: Promise<{
    orderNumber: string;
  }>;
}

/**
 * BFF Endpoint for customer single order detail: GET /api/customer/orders/[orderNumber] -> Spring /api/v1/customer/orders/[orderNumber]
 */
export async function GET(_request: NextRequest, context: RouteContext) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { orderNumber } = await context.params;
  if (!orderNumber || !orderNumber.trim()) {
    return NextResponse.json({ message: 'Número de pedido inválido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/orders/${encodeURIComponent(orderNumber)}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos consultar los detalles del pedido en este momento.' },
      { status: 503 }
    );
  }
}
