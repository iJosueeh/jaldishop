import { NextResponse } from 'next/server';
import { env } from '@/core/config/env';

export async function GET() {
  try {
    const upstream = await fetch(`${env.apiUrl}/public/marketplace/active-banner`, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(4000),
    });

    if (!upstream.ok) {
      return NextResponse.json({ active: false }, { status: 200 });
    }

    const data = await upstream.json();
    return NextResponse.json(data, {
      status: 200,
      headers: {
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return NextResponse.json({ active: false }, { status: 200 });
  }
}
