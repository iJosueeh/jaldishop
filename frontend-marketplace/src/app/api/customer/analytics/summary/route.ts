import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/**
 * BFF Endpoint for customer analytics summary:
 * GET /api/customer/analytics/summary -> Spring /api/v1/customer/analytics/summary
 */
export async function GET(request: NextRequest) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const from = request.nextUrl.searchParams.get('from');
  const to = request.nextUrl.searchParams.get('to');

  if ((from && !ISO_DATE_PATTERN.test(from)) || (to && !ISO_DATE_PATTERN.test(to))) {
    return NextResponse.json(
      { code: 'INVALID_DATE_FORMAT', message: 'El formato de fecha debe ser YYYY-MM-DD.' },
      { status: 400 }
    );
  }

  const query = new URLSearchParams();
  if (from) query.set('from', from);
  if (to) query.set('to', to);
  const queryString = query.toString();
  const endpoint = `${env.apiUrl}/customer/analytics/summary${queryString ? `?${queryString}` : ''}`;

  try {
    const upstream = await fetch(endpoint, {
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
      { message: 'No pudimos cargar tus estadísticas de compras en este momento.' },
      { status: 503 }
    );
  }
}
