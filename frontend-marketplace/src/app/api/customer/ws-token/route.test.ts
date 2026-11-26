import { describe, it, expect, vi, beforeEach } from 'vitest';
import { NextRequest } from 'next/server';
import { GET } from './route';

const mockGetCookie = vi.fn();

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: mockGetCookie,
  }),
}));

describe('GET /api/customer/ws-token', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns 401 when customer cookie is absent', async () => {
    mockGetCookie.mockReturnValue(undefined);
    const req = new NextRequest('http://localhost:3000/api/customer/ws-token');
    const res = await GET(req);
    expect(res.status).toBe(401);
    expect(res.headers.get('Cache-Control')).toBe('no-store');
  });

  it('returns 403 on cross-origin requests', async () => {
    const req = new NextRequest('http://localhost:3000/api/customer/ws-token', {
      headers: { origin: 'https://evil.example.com' },
    });
    const res = await GET(req);
    expect(res.status).toBe(403);
  });

  it('returns token with no-store when authenticated same-origin', async () => {
    mockGetCookie.mockReturnValue({ value: 'jwt-customer-123' });
    const req = new NextRequest('http://localhost:3000/api/customer/ws-token', {
      headers: { origin: 'http://localhost:3000' },
    });
    const res = await GET(req);
    expect(res.status).toBe(200);
    expect(res.headers.get('Cache-Control')).toBe('no-store');
    await expect(res.json()).resolves.toEqual({ token: 'jwt-customer-123' });
  });
});
