import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

interface RouteContext {
  params: Promise<{
    id: string;
  }>;
}

/**
 * BFF Endpoint for a single customer address:
 * - PUT: Update address
 * - DELETE: Delete address
 */
export async function PUT(request: NextRequest, context: RouteContext) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { id } = await context.params;
  if (!id || !id.trim()) {
    return NextResponse.json({ message: 'ID de dirección inválido.' }, { status: 400 });
  }

  try {
    const body = await request.json();
    const upstream = await fetch(`${env.apiUrl}/customer/addresses/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos actualizar tu dirección en este momento.' },
      { status: 503 }
    );
  }
}

export async function DELETE(request: NextRequest, context: RouteContext) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { id } = await context.params;
  if (!id || !id.trim()) {
    return NextResponse.json({ message: 'ID de dirección inválido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/addresses/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    if (upstream.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos eliminar la dirección en este momento.' },
      { status: 503 }
    );
  }
}
