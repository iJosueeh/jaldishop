import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * BFF Endpoint to resolve a paymentId from an attemptId (pa1_<attemptId>):
 * GET /api/customer/payments/attempts/{attemptId} -> Spring /api/v1/customer/payments/attempts/{attemptId}
 */
export async function GET(
  request: NextRequest,
  context: { params: Promise<{ attemptId: string }> }
) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      {
        code: 'UNAUTHENTICATED',
        message: 'Inicia sesión como cliente para consultar el estado del pago.',
      },
      { status: 401 }
    );
  }

  const { attemptId } = await context.params;
  if (!attemptId || !UUID_REGEX.test(attemptId.trim())) {
    return NextResponse.json(
      {
        code: 'INVALID_ATTEMPT_ID',
        message: 'El identificador de intento es obligatorio y debe ser un UUID válido.',
      },
      { status: 400 }
    );
  }

  try {
    const upstreamUrl = `${env.apiUrl}/customer/payments/attempts/${attemptId.trim()}`;
    const upstream = await fetch(upstreamUrl, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json().catch(() => ({}));

    if (upstream.status === 401) {
      const expiredResponse = NextResponse.json(
        {
          code: 'SESSION_EXPIRED',
          message: 'Tu sesión ha expirado. Por favor inicia sesión nuevamente.',
        },
        { status: 401 }
      );
      expiredResponse.cookies.delete(CUSTOMER_COOKIE);
      return expiredResponse;
    }

    if (upstream.status === 403) {
      return NextResponse.json(
        {
          code: 'FORBIDDEN',
          message: 'No tienes autorización para consultar este pago.',
        },
        { status: 403 }
      );
    }

    if (upstream.status === 404) {
      return NextResponse.json(
        {
          code: 'PAYMENT_NOT_FOUND',
          message: 'El pago solicitado no fue encontrado.',
        },
        { status: 404 }
      );
    }

    if (upstream.status >= 500) {
      return NextResponse.json(
        {
          code: 'PAYMENT_GATEWAY_UNAVAILABLE',
          message: 'El servicio de verificación no está disponible en este momento.',
        },
        { status: 503 }
      );
    }

    return NextResponse.json(data, { status: upstream.status });
  } catch {
    return NextResponse.json(
      {
        code: 'PAYMENT_SERVICE_UNAVAILABLE',
        message: 'No se pudo conectar con el servicio de pagos. Intenta nuevamente.',
      },
      { status: 503 }
    );
  }
}
