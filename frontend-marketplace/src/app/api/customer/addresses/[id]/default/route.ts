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
 * BFF Endpoint to set a customer address as default:
 * - PATCH: Set default address
 */
export async function PATCH(request: NextRequest, context: RouteContext) {
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
    const upstream = await fetch(`${env.apiUrl}/customer/addresses/${encodeURIComponent(id)}/default`, {
      method: 'PATCH',
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
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos establecer la dirección predeterminada en este momento.' },
      { status: 503 }
    );
  }
}
