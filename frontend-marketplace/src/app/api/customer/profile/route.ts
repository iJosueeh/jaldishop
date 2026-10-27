import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

/**
 * BFF Endpoint for customer profile inspection: GET /api/customer/profile -> Spring /api/v1/users/me
 */
export async function GET() {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/users/me`, {
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
      { message: 'No pudimos consultar tu perfil en este momento. Inténtalo más tarde.' },
      { status: 503 }
    );
  }
}

/**
 * BFF Endpoint for customer profile updates: PUT /api/customer/profile -> Spring /api/v1/users/me
 */
export async function PUT(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  try {
    const body = await request.json();
    const upstream = await fetch(`${env.apiUrl}/users/me`, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
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
      { message: 'No pudimos actualizar tu perfil. Inténtalo de nuevo.' },
      { status: 503 }
    );
  }
}
