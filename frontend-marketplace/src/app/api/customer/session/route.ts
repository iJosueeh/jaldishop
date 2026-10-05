import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE, rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

export async function GET() {
  return NextResponse.json({ authenticated: Boolean((await cookies()).get(CUSTOMER_COOKIE)?.value) });
}

export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;
  try {
    const { email, password } = await request.json();
    if (typeof email !== 'string' || typeof password !== 'string') return NextResponse.json({ message: 'Ingresa tu correo y contraseña.' }, { status: 400 });
    const upstream = await fetch(`${env.apiUrl}/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }), cache: 'no-store', signal: AbortSignal.timeout(10000),
    });
    const data = await upstream.json();
    if (!upstream.ok) return NextResponse.json({ message: upstream.status === 401 ? 'Correo o contraseña incorrectos.' : data.message || 'No pudimos iniciar sesión.' }, { status: upstream.status });
    if (!Array.isArray(data.roles) || !data.roles.includes('CUSTOMER') || typeof data.token !== 'string') return NextResponse.json({ message: 'Ingresa con una cuenta de cliente para usar el carrito.' }, { status: 403 });
    const response = NextResponse.json({ authenticated: true, fullName: data.fullName });
    response.cookies.set(CUSTOMER_COOKIE, data.token, { httpOnly: true, secure: process.env.NODE_ENV === 'production', sameSite: 'lax', path: '/' });
    return response;
  } catch {
    return NextResponse.json({ message: 'No pudimos conectar con el acceso de clientes. Vuelve a intentarlo.' }, { status: 503 });
  }
}

export async function DELETE(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;
  const response = NextResponse.json({ authenticated: false });
  response.cookies.delete(CUSTOMER_COOKIE);
  return response;
}
