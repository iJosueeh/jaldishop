import 'server-only';
import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { env } from '../config/env';

export const CUSTOMER_COOKIE = 'jaldishop_customer_token';

export function rejectCrossOrigin(request: NextRequest) {
  const origin = request.headers.get('origin');
  return origin && origin !== request.nextUrl.origin
    ? NextResponse.json({ code: 'FORBIDDEN', message: 'Solicitud no permitida.' }, { status: 403 }) : null;
}

// Tokens stay in an HttpOnly cookie. Only this server adapter forwards them to Spring.
export async function customerProxy(request: NextRequest, endpoint: string) {
  if (request.method !== 'GET') {
    const rejected = rejectCrossOrigin(request);
    if (rejected) return rejected;
  }
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) return NextResponse.json({ code: 'UNAUTHENTICATED', message: 'Inicia sesión como cliente para usar tu carrito.' }, { status: 401 });
  try {
    const upstream = await fetch(`${env.apiUrl}${endpoint}`, {
      method: request.method,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: ['POST', 'PUT'].includes(request.method) ? await request.text() : undefined,
      cache: 'no-store', signal: AbortSignal.timeout(10000),
    });
    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });
    if (upstream.status === 401) response.cookies.delete(CUSTOMER_COOKIE);
    return response;
  } catch {
    return NextResponse.json({ code: 'CART_UNAVAILABLE', message: 'No pudimos conectar con el carrito. Vuelve a intentarlo.' }, { status: 503 });
  }
}
