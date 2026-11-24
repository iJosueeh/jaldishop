import { NextResponse } from 'next/server';
import { env } from '@/core/config/env';

/**
 * Public BFF endpoint for non-sensitive platform settings:
 * GET /api/platform-settings
 */
export async function GET() {
  try {
    const upstream = await fetch(`${env.apiUrl}/public/platform-settings`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(5000),
    });

    if (!upstream.ok) {
      return NextResponse.json(
        { customerServiceFeeRate: 0, customerServiceFeeCap: null },
        { status: 200 }
      );
    }

    const data = await upstream.json();
    const rate = Number(data?.customerServiceFeeRate ?? 0);
    const rawCap = data?.customerServiceFeeCap;
    const cap =
      rawCap !== null && rawCap !== undefined && Number.isFinite(Number(rawCap)) && Number(rawCap) >= 0
        ? Number(rawCap)
        : null;
    return NextResponse.json(
      {
        customerServiceFeeRate: Number.isFinite(rate) && rate >= 0 ? rate : 0,
        customerServiceFeeCap: cap,
      },
      {
        status: 200,
        headers: {
          'Cache-Control': 'no-store',
        },
      }
    );
  } catch {
    return NextResponse.json(
      { customerServiceFeeRate: 0, customerServiceFeeCap: null },
      { status: 200 }
    );
  }
}
