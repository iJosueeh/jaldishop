import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';

/**
 * Entrega el token JWT de la cookie HttpOnly únicamente a peticiones same-origin
 * para autenticar la conexión WebSocket/STOMP (`/ws`) del cliente.
 */
export async function GET(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      { code: 'UNAUTHENTICATED', message: 'Sesión de cliente no iniciada.' },
      { status: 401, headers: { 'Cache-Control': 'no-store' } }
    );
  }

  return NextResponse.json(
    { token },
    { status: 200, headers: { 'Cache-Control': 'no-store' } }
  );
}
