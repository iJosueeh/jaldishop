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

describe('Customer Analytics Summary BFF Endpoint', () => {
  it('returns 401 when customer is not authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/analytics/summary');
    const response = await GET(req);
    expect(response.status).toBe(401);
  });

  it('returns 400 when date query params have invalid format', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/analytics/summary?from=24-11-2026');
    const response = await GET(req);
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('forwards summary with from/to query params to Spring backend', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          userId: 'u-1',
          period: { from: '2026-10-25', to: '2026-11-24', timeZone: 'America/Lima' },
          spending: {
            userId: 'u-1',
            totalOrdersCount: 4,
            totalSpentAmount: 185.5,
            totalRefundedAmount: 0,
            firstOrderAt: '2026-10-26T12:00:00Z',
            lastOrderAt: '2026-11-20T18:00:00Z',
          },
          topPurchasedProducts: [],
        }),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/analytics/summary?from=2026-10-25&to=2026-11-24');
    const response = await GET(req);
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.spending.totalOrdersCount).toBe(4);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/customer/analytics/summary?from=2026-10-25&to=2026-11-24'),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer valid-jwt' }),
      })
    );
  });
});
