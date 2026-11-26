import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';

export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null);
  if (!body || typeof body !== 'object') {
    return NextResponse.json(
      { valid: false, errorCode: 'INVALID_REQUEST', message: 'Solicitud inválida.' },
      { status: 400 }
    );
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/public/checkout/validate-promo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({
        storeId: body.storeId,
        code: typeof body.code === 'string' ? body.code : '',
        subtotal: Number(body.subtotal ?? 0),
      }),
      cache: 'no-store',
      signal: AbortSignal.timeout(5000),
    });

    const data = await upstream.json().catch(() => ({
      valid: false,
      errorCode: 'PROMO_VALIDATION_ERROR',
      message: 'No se pudo validar el código promocional.',
    }));

    return NextResponse.json(data, { status: upstream.status });
  } catch {
    return NextResponse.json(
      {
        valid: false,
        errorCode: 'PROMO_SERVICE_UNAVAILABLE',
        message: 'No se pudo conectar para validar el código.',
      },
      { status: 503 }
    );
  }
}
