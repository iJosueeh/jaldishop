import { NextRequest, NextResponse } from 'next/server';
import { rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';
import { isValidResetToken } from '@/features/auth/utils/validation';

export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  try {
    const body = await request.json().catch(() => null);
    if (!body || typeof body.token !== 'string') {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'El token de verificación es obligatorio.',
        },
        {
          status: 400,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const token = body.token.trim();
    if (!isValidResetToken(token)) {
      return NextResponse.json(
        {
          code: 'INVALID_EMAIL_VERIFICATION_TOKEN',
          message: 'El enlace de verificación no es válido o ha vencido.',
        },
        {
          status: 422,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const upstream = await fetch(`${env.apiUrl}/auth/verify-email`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    if (upstream.status === 204) {
      return new NextResponse(null, {
        status: 204,
        headers: { 'Cache-Control': 'no-store' },
      });
    }

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
        message: 'No pudimos conectar con el servicio de verificación. Vuelve a intentarlo.',
      },
      {
        status: 503,
        headers: { 'Cache-Control': 'no-store' },
      }
    );
  }
}
