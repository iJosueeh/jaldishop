import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

/**
 * BFF Endpoint for customer favorites list: GET /api/customer/favorites -> Spring /api/v1/customer/favorites/products
 */
export async function GET() {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/favorites/products`, {
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
      { message: 'No pudimos consultar tus favoritos en este momento.' },
      { status: 503 }
    );
  }
}
