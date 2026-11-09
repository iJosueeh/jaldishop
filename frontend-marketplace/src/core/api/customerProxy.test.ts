// @vitest-environment node
import { beforeEach, expect, it, vi } from 'vitest';
import { cookies } from 'next/headers';
import { NextRequest } from 'next/server';
import { customerProxy } from './customerProxy';

vi.mock('server-only', () => ({}));
vi.mock('next/headers', () => ({ cookies: vi.fn() }));

const fetchMock = vi.fn();
beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
  vi.mocked(cookies).mockResolvedValue({
    get: vi.fn().mockReturnValue({ value: 'validated-by-backend-jwt' }),
  } as unknown as Awaited<ReturnType<typeof cookies>>);
});

it.each(['30', 'Wed, 11 Nov 2026 12:00:00 GMT'])('forwards Retry-After unchanged: %s', async (retryAfter) => {
  fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: 'RATE_LIMIT_EXCEEDED' }), {
    status: 429, headers: { 'Retry-After': retryAfter },
  }));
  const response = await customerProxy(new NextRequest('http://localhost:3000/api/customer/cart'), '/cart');
  expect(response.status).toBe(429);
  expect(response.headers.get('Retry-After')).toBe(retryAfter);
  expect((await response.json()).code).toBe('RATE_LIMIT_EXCEEDED');
  expect(response.headers.get('set-cookie')).toBeNull();
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it('does not invent Retry-After on successful responses', async () => {
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }));
  const response = await customerProxy(new NextRequest('http://localhost:3000/api/customer/cart'), '/cart');
  expect(response.status).toBe(200);
  expect(response.headers.has('Retry-After')).toBe(false);
});
