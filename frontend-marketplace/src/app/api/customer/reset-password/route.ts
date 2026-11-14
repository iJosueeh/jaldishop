import { NextRequest, NextResponse } from 'next/server';
import { rejectCrossOrigin } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';
import { isValidResetToken, validateResetPassword } from '@/features/auth/utils/validation';

export async function POST(request: NextRequest) {
  const rejected = rejectCrossOrigin(request);
  if (rejected) return rejected;

  try {
    const body = await request.json().catch(() => null);
    if (!body || typeof body.token !== 'string' || typeof body.newPassword !== 'string') {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: 'Token y nueva contraseña son obligatorios.',
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
          code: 'VALIDATION_ERROR',
          message: 'El formato del token de recuperación no es válido.',
        },
        {
          status: 400,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const passwordError = validateResetPassword(body.newPassword);
    if (passwordError) {
      return NextResponse.json(
        {
          code: 'VALIDATION_ERROR',
          message: passwordError,
        },
        {
          status: 400,
          headers: { 'Cache-Control': 'no-store' },
        }
      );
    }

    const upstream = await fetch(`${env.apiUrl}/auth/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token, newPassword: body.newPassword }),
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
        message: 'No pudimos conectar con el servicio de recuperación. Vuelve a intentarlo.',
      },
      {
        status: 503,
        headers: { 'Cache-Control': 'no-store' },
      }
    );
  }
}
