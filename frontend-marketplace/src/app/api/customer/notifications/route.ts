import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

/**
 * BFF Endpoint for customer notifications list:
 * GET /api/customer/notifications -> Spring /api/v1/notifications
 */
export async function GET(request: NextRequest) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const pageParam = request.nextUrl.searchParams.get('page') ?? '0';
  const sizeParam = request.nextUrl.searchParams.get('size') ?? '20';

  const page = Math.max(0, Number.parseInt(pageParam, 10) || 0);
  const size = Math.min(100, Math.max(1, Number.parseInt(sizeParam, 10) || 20));

  try {
    const upstream = await fetch(`${env.apiUrl}/notifications?page=${page}&size=${size}`, {
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
      { message: 'No pudimos cargar tus notificaciones en este momento.' },
      { status: 503 }
    );
  }
}
