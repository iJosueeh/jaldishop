import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';

export const dynamic = 'force-dynamic';

/**
 * BFF Endpoint (no-store) for live public product reviews summary:
 * GET /api/products/[productId]/reviews
 */
export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ productId: string }> }
) {
  const { productId } = await context.params;
  if (!productId) {
    return NextResponse.json({ message: 'Identificador de producto requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/products/${encodeURIComponent(productId)}/reviews`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      signal: AbortSignal.timeout(8000),
    });

    if (!upstream.ok) {
      return NextResponse.json({ message: 'No pudimos obtener las reseñas.' }, { status: upstream.status });
    }

    const data = await upstream.json();
    return NextResponse.json(data, {
      status: 200,
      headers: { 'Cache-Control': 'no-store, max-age=0' },
    });
  } catch {
    return NextResponse.json(
      { message: 'No pudimos sincronizar las reseñas en este momento.' },
      { status: 503 }
    );
  }
}
