import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GET } from './route';
import { cookies } from 'next/headers';
import { NextRequest } from 'next/server';

vi.mock('@/core/api/customerProxy', () => ({
  CUSTOMER_COOKIE: 'jaldishop_customer_token',
}));

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('Customer Analytics PDF Export BFF Endpoint', () => {
  it('returns 401 when customer is not authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/analytics/export/pdf');
    const response = await GET(req);
    expect(response.status).toBe(401);
  });

  it('streams PDF bytes and preserves Content-Disposition header', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const pdfContent = new Uint8Array([37, 80, 68, 70]); // %PDF
    fetchMock.mockResolvedValue(
      new Response(pdfContent, {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="jaldishop-customer-analytics-2026-10-25-al-2026-11-24.pdf"',
        },
      })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/analytics/export/pdf?from=2026-10-25&to=2026-11-24');
    const response = await GET(req);
    expect(response.status).toBe(200);
    expect(response.headers.get('Content-Type')).toBe('application/pdf');
    expect(response.headers.get('Content-Disposition')).toContain('jaldishop-customer-analytics-2026-10-25-al-2026-11-24.pdf');
  });
});
