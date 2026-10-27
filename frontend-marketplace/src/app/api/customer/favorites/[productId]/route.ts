import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

interface RouteContext {
  params: Promise<{
    productId: string;
  }>;
}

/**
 * BFF Endpoint for single product favorite status: GET /api/customer/favorites/[productId] -> Spring /api/v1/customer/favorites/products/[productId]/status
 */
export async function GET(_request: NextRequest, context: RouteContext) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { productId } = await context.params;
  if (!productId) {
    return NextResponse.json({ message: 'Identificador de producto requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/favorites/products/${encodeURIComponent(productId)}/status`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos consultar el estado del favorito.' },
      { status: 503 }
    );
  }
}

/**
 * BFF Endpoint to add product to favorites: POST /api/customer/favorites/[productId] -> Spring /api/v1/customer/favorites/products/[productId]
 */
export async function POST(_request: NextRequest, context: RouteContext) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { productId } = await context.params;
  if (!productId) {
    return NextResponse.json({ message: 'Identificador de producto requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/favorites/products/${encodeURIComponent(productId)}`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos agregar el producto a favoritos.' },
      { status: 503 }
    );
  }
}

/**
 * BFF Endpoint to remove product from favorites: DELETE /api/customer/favorites/[productId] -> Spring /api/v1/customer/favorites/products/[productId]
 */
export async function DELETE(_request: NextRequest, context: RouteContext) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const { productId } = await context.params;
  if (!productId) {
    return NextResponse.json({ message: 'Identificador de producto requerido.' }, { status: 400 });
  }

  try {
    const upstream = await fetch(`${env.apiUrl}/customer/favorites/products/${encodeURIComponent(productId)}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      signal: AbortSignal.timeout(10000),
    });

    const data = await upstream.json();
    const response = NextResponse.json(data, { status: upstream.status });

    if (upstream.status === 401) {
      response.cookies.delete(CUSTOMER_COOKIE);
    }

    return response;
  } catch {
    return NextResponse.json(
      { message: 'No pudimos quitar el producto de favoritos.' },
      { status: 503 }
    );
  }
}
