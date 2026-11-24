import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GET } from './route';
import { cookies } from 'next/headers';

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

describe('Unread Notifications Count BFF Endpoint', () => {
  it('returns 401 when unauthenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const response = await GET();
    expect(response.status).toBe(401);
  });

  it('returns unreadCount from Spring backend', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(new Response(JSON.stringify({ unreadCount: 3 }), { status: 200 }));

    const response = await GET();
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.unreadCount).toBe(3);
  });
});
