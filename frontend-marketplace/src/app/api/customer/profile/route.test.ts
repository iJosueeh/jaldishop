import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { GET, PUT } from './route';
import { cookies } from 'next/headers';

vi.mock('@/core/api/customerProxy', () => ({
  CUSTOMER_COOKIE: 'jaldishop_customer_token',
  rejectCrossOrigin: () => null,
}));

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('Customer Profile BFF Endpoint', () => {
  it('returns 401 when customer is not authenticated on GET', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const response = await GET();
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.message).toBe('No autenticado.');
  });

  it('forwards profile response from upstream when authenticated on GET', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'user-uuid-1',
          email: 'cliente@ejemplo.com',
          firstName: 'Juan',
          lastName: 'Pérez',
          phone: '+51987654321',
          status: 'ACTIVE',
          roles: ['CUSTOMER'],
          createdAt: '2026-10-25T10:00:00Z',
        }),
        { status: 200 }
      )
    );

    const response = await GET();
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.firstName).toBe('Juan');
    expect(body.email).toBe('cliente@ejemplo.com');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/users/me'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer valid-jwt-token',
        }),
      })
    );
  });

  it('forwards update payload on PUT and returns updated profile', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'user-uuid-1',
          email: 'cliente@ejemplo.com',
          firstName: 'Carlos',
          lastName: 'Gómez',
          phone: '+51999888777',
          status: 'ACTIVE',
          roles: ['CUSTOMER'],
          createdAt: '2026-10-25T10:00:00Z',
        }),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        firstName: 'Carlos',
        lastName: 'Gómez',
        phone: '+51999888777',
      }),
    });

    const response = await PUT(req);
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.firstName).toBe('Carlos');
    expect(body.lastName).toBe('Gómez');
  });
});
