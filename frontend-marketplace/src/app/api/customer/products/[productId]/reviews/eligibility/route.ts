import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

/**
 * BFF Endpoint for review eligibility: GET /api/customer/products/[productId]/reviews/eligibility
 */
export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ productId: string }> }
) {
  const { productId } = await context.params;
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;

  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  try {
    const upstream = await fetch(
      `${env.apiUrl}/customer/products/${encodeURIComponent(productId)}/reviews/eligibility`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
        signal: AbortSignal.timeout(10000),
      }
    );

    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos verificar la elegibilidad para calificar.' },
      { status: 503 }
    );
  }
}
