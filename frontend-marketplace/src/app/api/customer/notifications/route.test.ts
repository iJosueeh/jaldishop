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

describe('Customer Notifications BFF Endpoint', () => {
  it('returns 401 when unauthenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/notifications');
    const response = await GET(req);
    expect(response.status).toBe(401);
  });

  it('forwards page and size params to Spring backend', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 'notif-1',
            userId: 'u-1',
            type: 'ORDER_STATUS_CHANGED',
            title: 'Pedido en preparación',
            message: 'Tu pedido #ORD-1 está en preparación.',
            status: 'UNREAD',
            createdAt: '2026-11-24T10:00:00Z',
            readAt: null,
          },
        ]),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/notifications?page=1&size=10');
    const response = await GET(req);
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/notifications?page=1&size=10'),
      expect.any(Object)
    );
  });
});
