import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * BFF Endpoint for customer capacity slots query:
 * GET /api/customer/capacity/slots?storeId={storeId}&date={date}
 *
 * Enforces customer authentication, parameter validation, and proxies
 * to backend Spring Boot GET /api/v1/capacity/slots.
 */
export async function GET(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      {
        code: 'UNAUTHENTICATED',
        message: 'Inicia sesión como cliente para consultar los horarios disponibles.',
      },
      { status: 401 }
    );
  }

  const searchParams = request.nextUrl.searchParams;
  const storeId = searchParams.get('storeId');
  const date = searchParams.get('date');

  if (!storeId || !UUID_REGEX.test(storeId.trim())) {
    return NextResponse.json(
      {
        code: 'INVALID_STORE_ID',
        message: 'El identificador de la tienda es obligatorio y debe ser un UUID válido.',
      },
      { status: 400 }
    );
  }

  if (date && !DATE_REGEX.test(date.trim())) {
    return NextResponse.json(
      {
        code: 'INVALID_DATE_FORMAT',
        message: 'El formato de fecha no es válido (use AAAA-MM-DD).',
      },
      { status: 400 }
    );
  }

  const upstreamParams = new URLSearchParams();
  upstreamParams.set('storeId', storeId.trim());
  if (date) {
    upstreamParams.set('date', date.trim());
  }

  try {
    const upstreamUrl = `${env.apiUrl}/capacity/slots?${upstreamParams.toString()}`;
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
    const response = NextResponse.json(data, { status: upstream.status });

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
          message: 'Solo clientes autorizados pueden consultar las franjas de capacidad.',
        },
        { status: 403 }
      );
    }

    return response;
  } catch (error) {
    console.error('[CapacitySlotsBFF] Error conectando con el servicio de capacidad:', error);
    return NextResponse.json(
      {
        code: 'CAPACITY_UNAVAILABLE',
        message: 'No pudimos consultar los horarios en este momento. Intenta nuevamente.',
      },
      { status: 503 }
    );
  }
}
