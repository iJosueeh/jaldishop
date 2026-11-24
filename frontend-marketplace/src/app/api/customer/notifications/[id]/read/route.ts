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
 * BFF Endpoint to mark a notification as read:
 * PATCH /api/customer/notifications/[id]/read -> Spring /api/v1/notifications/{id}/read
 */
export async function PATCH(request: NextRequest, context: RouteContext) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { id } = await context.params;
  if (!id) {
    return NextResponse.json({ message: 'Identificador de notificación requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/notifications/${encodeURIComponent(id)}/read`, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
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
      { message: 'No pudimos marcar la notificación como leída.' },
      { status: 503 }
    );
  }
}
