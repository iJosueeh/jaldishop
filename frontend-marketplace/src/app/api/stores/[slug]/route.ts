import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';

/**
 * Public BFF endpoint for single store details:
 * GET /api/stores/[slug]
 */
export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ slug: string }> }
) {
  const { slug } = await context.params;
  if (!slug?.trim()) {
    return NextResponse.json({ message: 'Slug de tienda requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/stores/slug/${encodeURIComponent(slug)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      next: { revalidate: 60 },
      signal: AbortSignal.timeout(8000),
    });

    if (!upstream.ok) {
      return NextResponse.json(
        { message: 'No pudimos consultar la tienda.' },
        { status: upstream.status }
      );
    }

    const store = await upstream.json();
    return NextResponse.json(store, {
      status: 200,
      headers: {
        'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=120',
      },
    });
  } catch {
    return NextResponse.json(
      { message: 'Error al conectar con el servicio de tiendas.' },
      { status: 502 }
    );
  }
}
