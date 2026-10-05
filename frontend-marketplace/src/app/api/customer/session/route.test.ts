import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { POST } from './route';

vi.mock('@/core/api/customerProxy', () => ({ CUSTOMER_COOKIE: 'jaldishop_customer_token', rejectCrossOrigin: () => null }));
vi.mock('next/headers', () => ({ cookies: vi.fn() }));
const fetchMock = vi.fn();
beforeEach(() => { vi.clearAllMocks(); vi.stubGlobal('fetch', fetchMock); });
const request = () => new NextRequest('http://localhost:3000/api/customer/session', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'customer@example.test', password: 'test-password' }) });

describe('Customer session adapter', () => {
  it('keeps the JWT in an HttpOnly cookie and excludes it from the browser response', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ token: 'test-jwt', roles: ['CUSTOMER'], fullName: 'Cliente' }), { status: 200 }));
    const response = await POST(request());
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ authenticated: true, fullName: 'Cliente' });
    expect(response.cookies.get('jaldishop_customer_token')?.value).toBe('test-jwt');
    expect(response.headers.get('set-cookie')).toContain('HttpOnly');
    expect(response.headers.get('set-cookie')).toContain('SameSite=lax');
  });
  it('rejects merchant-only accounts without setting a customer session', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ token: 'merchant-token', roles: ['MERCHANT'] }), { status: 200 }));
    const response = await POST(request());
    expect(response.status).toBe(403);
    expect(response.cookies.get('jaldishop_customer_token')).toBeUndefined();
  });
  it('preserves failed authentication as an error instead of inventing a session', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: 'Invalid credentials' }), { status: 401 }));
    const response = await POST(request());
    expect(response.status).toBe(401);
    expect(response.cookies.get('jaldishop_customer_token')).toBeUndefined();
  });
});
