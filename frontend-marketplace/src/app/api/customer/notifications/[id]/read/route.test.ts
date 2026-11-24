import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PATCH } from './route';
import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { rejectCrossOrigin } from '@/core/api/customerProxy';

vi.mock('@/core/api/customerProxy', () => ({
  CUSTOMER_COOKIE: 'jaldishop_customer_token',
  rejectCrossOrigin: vi.fn().mockReturnValue(null),
}));

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(rejectCrossOrigin).mockReturnValue(null);
  vi.stubGlobal('fetch', fetchMock);
});

describe('Mark Notification Read BFF Endpoint', () => {
  it('rejects cross-origin requests with 403', async () => {
    vi.mocked(rejectCrossOrigin).mockReturnValue(
      NextResponse.json({ code: 'FORBIDDEN' }, { status: 403 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/notifications/notif-1/read', {
      method: 'PATCH',
    });
    const response = await PATCH(req, { params: Promise.resolve({ id: 'notif-1' }) });
    expect(response.status).toBe(403);
  });

  it('forwards PATCH mark as read to Spring backend', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'notif-1',
          status: 'READ',
          readAt: '2026-11-24T10:30:00Z',
        }),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/notifications/notif-1/read', {
      method: 'PATCH',
    });
    const response = await PATCH(req, { params: Promise.resolve({ id: 'notif-1' }) });
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('READ');
  });
});
