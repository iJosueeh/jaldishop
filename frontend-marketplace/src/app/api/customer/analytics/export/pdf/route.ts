import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { CUSTOMER_COOKIE } from '@/core/api/customerProxy';
import { env } from '@/core/config/env';

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/**
 * BFF Endpoint for customer analytics PDF export:
 * GET /api/customer/analytics/export/pdf -> Spring /api/v1/customer/analytics/export/pdf
 */
export async function GET(request: NextRequest) {
  const token = (await cookies()).get(CUSTOMER_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ message: 'No autenticado.' }, { status: 401 });
  }

  const from = request.nextUrl.searchParams.get('from');
  const to = request.nextUrl.searchParams.get('to');

  if ((from && !ISO_DATE_PATTERN.test(from)) || (to && !ISO_DATE_PATTERN.test(to))) {
    return NextResponse.json(
      { code: 'INVALID_DATE_FORMAT', message: 'El formato de fecha debe ser YYYY-MM-DD.' },
      { status: 400 }
    );
  }

  const query = new URLSearchParams();
  if (from) query.set('from', from);
  if (to) query.set('to', to);
  const queryString = query.toString();
  const endpoint = `${env.apiUrl}/customer/analytics/export/pdf${queryString ? `?${queryString}` : ''}`;

  try {
    const upstream = await fetch(endpoint, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/pdf',
      },
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    });

    if (!upstream.ok) {
      const errorData = await upstream.json().catch(() => ({
        message: 'No pudimos generar el reporte PDF.',
      }));
      const response = NextResponse.json(errorData, { status: upstream.status });
      if (upstream.status === 401) {
        response.cookies.delete(CUSTOMER_COOKIE);
      }
      return response;
    }

    const pdfBuffer = await upstream.arrayBuffer();
    const contentDisposition =
      upstream.headers.get('Content-Disposition') ||
      'attachment; filename="jaldishop-customer-analytics.pdf"';

    return new NextResponse(pdfBuffer, {
      status: 200,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': contentDisposition,
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return NextResponse.json(
      { message: 'No pudimos exportar tu reporte PDF en este momento.' },
      { status: 503 }
    );
  }
}
