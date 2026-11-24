import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';

/**
 * Public BFF endpoint for paginated store directory:
 * GET /api/stores?q=&page=1&pageSize=9
 */
export async function GET(request: NextRequest) {
  const q = request.nextUrl.searchParams.get('q')?.trim() ?? '';
  const pageParam = request.nextUrl.searchParams.get('page') ?? '1';
  const pageSizeParam = request.nextUrl.searchParams.get('pageSize') ?? '9';

  const page = Math.max(1, Number.parseInt(pageParam, 10) || 1);
  const pageSize = Math.min(50, Math.max(1, Number.parseInt(pageSizeParam, 10) || 9));

  const searchParams = new URLSearchParams({
    q,
    limit: '50',
  });

  try {
    const upstream = await fetch(`${env.apiUrl}/stores/search?${searchParams.toString()}`, {
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
        { message: 'No pudimos consultar el directorio de tiendas.' },
        { status: upstream.status }
      );
    }

    const allStores = (await upstream.json()) as unknown[];
    const safeList = Array.isArray(allStores) ? allStores : [];
    const total = safeList.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    const clampedPage = Math.min(page, totalPages);
    const start = (clampedPage - 1) * pageSize;
    const items = safeList.slice(start, start + pageSize);

    return NextResponse.json(
      {
        items,
        total,
        page: clampedPage,
        pageSize,
        totalPages,
      },
      {
        status: 200,
        headers: {
          'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=120',
        },
      }
    );
  } catch {
    return NextResponse.json(
      { message: 'No pudimos conectar con el directorio de tiendas.' },
      { status: 503 }
    );
  }
}
