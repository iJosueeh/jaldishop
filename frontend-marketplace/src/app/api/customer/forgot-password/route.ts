import { NextRequest, NextResponse } from 'next/server';
import { rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';
import { isValidEmail } from '@/features/auth/utils/validation';

export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  try {
    const body = await request.json().catch(() => null);
    if (!body || typeof body.email !== 'string') {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'Ingresa un correo electrónico válido.',
        },
        {
          status: 400,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const cleanEmail = body.email.trim().toLowerCase();
    if (!cleanEmail || !isValidEmail(cleanEmail) || cleanEmail.length > 254) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'Ingresa un correo electrónico con formato válido.',
        },
        {
          status: 400,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const upstream = await fetch(`${env.apiUrl}/auth/forgot-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email: cleanEmail }),
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(data, {
      status: upstream.status,
      headers: { 'Cache-Control': 'no-store' },
    });

    const retryAfter = upstream.headers.get('Retry-After');
    if (retryAfter) {
      response.headers.set('Retry-After', retryAfter);
    }

    return response;
  } catch {
    return NextResponse.json(
      {
        code: 'SERVICE_UNAVAILABLE',
        message: 'No pudimos conectar con el servicio de recuperación. Vuelve a intentarlo.',
      },
      {
        status: 503,
        headers: { 'Cache-Control': 'no-store' },
      }
    );
  }
}
