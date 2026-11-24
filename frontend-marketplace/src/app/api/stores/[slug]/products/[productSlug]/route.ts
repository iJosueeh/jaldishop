import { NextRequest, NextResponse } from 'next/server';
import { env } from '@/core/config/env';
import { toProductItem } from '@/features/storefront/services/storeService';

export const dynamic = 'force-dynamic';

/**
 * BFF Endpoint (no-store) for live product detail & stock synchronization:
 * GET /api/stores/[slug]/products/[productSlug]
 */
export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ slug: string; productSlug: string }> }
) {
  const { slug, productSlug } = await context.params;
  if (!slug || !productSlug) {
    return NextResponse.json({ message: 'Parámetros inválidos.' }, { status: 400 });
  }

  try {
    const [productRes, categoriesRes] = await Promise.all([
      fetch(
        `${env.apiUrl}/stores/slug/${encodeURIComponent(slug)}/products/${encodeURIComponent(productSlug)}`,
        { method: 'GET', headers: { Accept: 'application/json' }, cache: 'no-store', signal: AbortSignal.timeout(10000) }
      ),
      fetch(`${env.apiUrl}/stores/slug/${encodeURIComponent(slug)}/categories`, {
        method: 'GET',
        headers: { Accept: 'application/json' },
        cache: 'no-store',
        signal: AbortSignal.timeout(10000),
      }).catch(() => null),
    ]);

    if (productRes.status === 404) {
      return NextResponse.json({ message: 'Producto no encontrado.' }, { status: 404 });
    }

    if (!productRes.ok) {
      return NextResponse.json({ message: 'No pudimos consultar el producto.' }, { status: productRes.status });
    }

    const productDto = await productRes.json();
    const categoriesDto = categoriesRes && categoriesRes.ok ? await categoriesRes.json().catch(() => []) : [];
    const mapped = toProductItem(productDto, Array.isArray(categoriesDto) ? categoriesDto : []);

    return NextResponse.json(mapped, {
      status: 200,
      headers: { 'Cache-Control': 'no-store, max-age=0' },
    });
  } catch {
    return NextResponse.json(
      { message: 'No pudimos sincronizar la disponibilidad del producto.' },
      { status: 503 }
    );
  }
}
