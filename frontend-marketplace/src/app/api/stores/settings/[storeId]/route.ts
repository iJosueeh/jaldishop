import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';

/**
 * Public BFF endpoint for store operational settings:
 * GET /api/stores/settings/[storeId]
 */
export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ storeId: string }> }
) {
  const { storeId } = await context.params;
  if (!storeId?.trim()) {
    return NextResponse.json({ message: 'ID de tienda requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(
      `${env.apiUrl}/stores/${encodeURIComponent(storeId)}/public-settings`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        next: { revalidate: 30 },
        signal: AbortSignal.timeout(8000),
      }
    );

    if (!upstream.ok) {
      return NextResponse.json(
        { message: 'No pudimos consultar la configuración de la tienda.' },
        { status: upstream.status }
      );
    }

    const settings = await upstream.json();
    return NextResponse.json(settings, {
      status: 200,
      headers: {
        'Cache-Control': 'public, s-maxage=30, stale-while-revalidate=60',
      },
    });
  } catch {
    return NextResponse.json(
      { message: 'Error al conectar con el servicio de configuración.' },
      { status: 502 }
    );
  }
}
