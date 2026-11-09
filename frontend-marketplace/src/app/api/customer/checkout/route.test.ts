import { beforeEach, describe, expect, it, vi } from 'vitest';
import { POST } from './route';
import { cookies } from 'next/headers';
import { NextRequest } from 'next/server';

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
  vi.stubGlobal('fetch', fetchMock);
});

describe('Checkout BFF Endpoint: POST /api/customer/checkout', () => {
  it('preserves 429, RATE_LIMIT_EXCEEDED and Retry-After without deleting the session', async () => {
    const deleteCookie = vi.fn();
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt' }),
      delete: deleteCookie,
    } as unknown as Awaited<ReturnType<typeof cookies>>);
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      code: 'RATE_LIMIT_EXCEEDED', message: 'Espera antes de reintentar.',
    }), { status: 429, headers: { 'Retry-After': '42' } }));

    const response = await POST(new NextRequest('http://localhost:3000/api/customer/checkout', {
      method: 'POST', body: JSON.stringify({}),
    }));

    expect(response.status).toBe(429);
    expect(response.headers.get('Retry-After')).toBe('42');
    expect((await response.json()).code).toBe('RATE_LIMIT_EXCEEDED');
    expect(response.headers.get('set-cookie')).toBeNull();
    expect(deleteCookie).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('returns 401 when customer is not authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/checkout', {
      method: 'POST',
      body: JSON.stringify({}),
    });

    const response = await POST(req);
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.message).toBe('No autenticado.');
  });

  it('forwards checkout initiation to Spring backend when authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const mockResponse = {
      reservationId: 'res-123',
      reservationExpiresAt: '2026-10-26T18:00:00Z',
      storeId: 'store-1',
      storeName: 'Don Pepe',
      userId: 'user-1',
      fulfillmentType: 'DELIVERY',
      pricing: { totalAmount: 55.0 },
    };

    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(mockResponse), { status: 201 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout', {
      method: 'POST',
      body: JSON.stringify({
        storeId: 'store-1',
        fulfillmentType: 'DELIVERY',
        deliveryAddress: 'Av. Larco 1234, MIRAFLORES, LIMA',
      }),
    });

    const response = await POST(req);
    expect(response.status).toBe(201);
    const body = await response.json();
    expect(body.reservationId).toBe('res-123');
  });

  it('handles backend error responses gracefully', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'DELIVERY_NOT_AVAILABLE',
          message: 'La tienda no realiza envíos a domicilio.',
        }),
        { status: 409 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout', {
      method: 'POST',
      body: JSON.stringify({}),
    });

    const response = await POST(req);
    expect(response.status).toBe(409);
    const body = await response.json();
    expect(body.code).toBe('DELIVERY_NOT_AVAILABLE');
  });
});
