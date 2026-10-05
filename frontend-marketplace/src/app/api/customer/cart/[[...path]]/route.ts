import { NextRequest, NextResponse } from 'next/server';
import { customerProxy } from '@/core/api/customerProxy';

async function handle(request: NextRequest, context: { params: Promise<{ path?: string[] }> }) {
  const { path = [] } = await context.params;
  const valid = path.length === 0 ? ['GET', 'DELETE'].includes(request.method)
    : path.length === 1 && path[0] === 'items' ? request.method === 'POST'
    : path.length === 2 && path[0] === 'items' && /^[0-9a-f-]{36}$/i.test(path[1]) && ['PUT', 'DELETE'].includes(request.method);
  if (!valid) return NextResponse.json({ message: 'Operación de carrito no disponible.' }, { status: 404 });
  const storeId = request.nextUrl.searchParams.get('storeId');
  return customerProxy(request, `/cart${path.length ? `/${path.join('/')}` : ''}${storeId ? `?storeId=${encodeURIComponent(storeId)}` : ''}`);
}

export { handle as GET, handle as POST, handle as PUT, handle as DELETE };
