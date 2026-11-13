import { beforeEach, describe, expect, it, vi } from 'vitest';
import { POST } from './route';
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
  vi.stubGlobal('fetch', fetchMock);
});

describe('Mercado Pago Preference BFF: POST /api/customer/checkout/mercado-pago/preference', () => {
  const validPayload = {
    capacityReservationId: 'b7f0c5a2-3f1d-4e92-8a9d-5a7c2b3e4f50',
    fulfillmentType: 'DELIVERY',
    customerName: 'Rosa Flores',
    customerPhone: '987654321',
    customerEmail: 'rosa@example.com',
    deliveryAddress: 'Av. Larco 123',
    deliveryReference: 'Frente al parque',
    deliveryLatitude: -12.12,
    deliveryLongitude: -77.03,
  };

  it('rejects cross-origin requests', async () => {
    vi.mocked(rejectCrossOrigin).mockReturnValueOnce(
      NextResponse.json({ code: 'FORBIDDEN', message: 'Solicitud no permitida.' }, { status: 403 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(403);
    const body = await res.json();
    expect(body.code).toBe('FORBIDDEN');
  });

  it('returns 401 when customer session cookie is absent', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.code).toBe('UNAUTHENTICATED');
  });

  it('returns 400 when capacityReservationId is invalid UUID', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify({
        ...validPayload,
        capacityReservationId: 'not-a-uuid',
      }),
    });

    const res = await POST(req);
    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('INVALID_RESERVATION_ID');
  });

  it('returns 400 when fulfillmentType is invalid', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify({
        ...validPayload,
        fulfillmentType: 'DRONE_DELIVERY',
      }),
    });

    const res = await POST(req);
    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('INVALID_FULFILLMENT_TYPE');
  });

  it('falls back to /users/me when customer details are omitted', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    // First call: GET /users/me
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          firstName: 'Carlos',
          lastName: 'Mendoza',
          phone: '999888777',
          email: 'carlos@example.com',
        }),
        { status: 200 }
      )
    );

    // Second call: POST /checkout/mercado-pago/preference
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          paymentId: '6c318881-8051-4f32-be43-851f5e8dae41',
          preferenceId: 'pref_12345',
          initPoint: 'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref_12345',
          status: 'PENDING',
        }),
        { status: 201 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify({
        capacityReservationId: 'b7f0c5a2-3f1d-4e92-8a9d-5a7c2b3e4f50',
        fulfillmentType: 'PICKUP',
      }),
    });

    const res = await POST(req);
    expect(res.status).toBe(201);
    const body = await res.json();
    expect(body.preferenceId).toBe('pref_12345');

    // Verify upstream call used profile data
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const upstreamBody = JSON.parse(fetchMock.mock.calls[1][1].body);
    expect(upstreamBody.customerName).toBe('Carlos Mendoza');
    expect(upstreamBody.customerPhone).toBe('999888777');
    expect(upstreamBody.customerEmail).toBe('carlos@example.com');
  });

  it('returns 201 with payment preference when upstream succeeds', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const mockResponse = {
      paymentId: '6c318881-8051-4f32-be43-851f5e8dae41',
      preferenceId: 'pref_abc123',
      initPoint: 'https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref_abc123',
      status: 'PENDING',
    };

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(mockResponse), { status: 201 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(201);
    const body = await res.json();
    expect(body.paymentId).toBe('6c318881-8051-4f32-be43-851f5e8dae41');
    expect(body.initPoint).toBe('https://mercadopago.com.pe/checkout/v1/redirect?pref_id=pref_abc123');
  });

  it('deletes customer cookie and returns 401 when upstream session expires', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'expired-jwt' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Token expirado' }), { status: 401 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.code).toBe('SESSION_EXPIRED');
    expect(res.headers.get('set-cookie')).toContain('jaldishop_customer_token=;');
  });

  it('propagates 409 Conflict when reservation is expired', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          code: 'RESERVATION_EXPIRED',
          message: 'La reserva de capacidad se encuentra expirada.',
        }),
        { status: 409 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(409);
    const body = await res.json();
    expect(body.code).toBe('RESERVATION_EXPIRED');
  });

  it('propagates 429 and Retry-After header without deleting session', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          code: 'RATE_LIMIT_EXCEEDED',
          message: 'Límite de solicitudes excedido.',
        }),
        { status: 429, headers: { 'Retry-After': '60' } }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(429);
    expect(res.headers.get('Retry-After')).toBe('60');
    expect(res.headers.get('set-cookie')).toBeNull();
  });

  it('returns 503 when upstream gateway is unavailable (502/503)', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Bad Gateway' }), { status: 502 })
    );

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(503);
    const body = await res.json();
    expect(body.code).toBe('PAYMENT_GATEWAY_UNAVAILABLE');
  });

  it('returns 503 on network timeout or fetch abort', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockRejectedValueOnce(new Error('Network timeout'));

    const req = new NextRequest('http://localhost:3000/api/customer/checkout/mercado-pago/preference', {
      method: 'POST',
      body: JSON.stringify(validPayload),
    });

    const res = await POST(req);
    expect(res.status).toBe(503);
    const body = await res.json();
    expect(body.code).toBe('PAYMENT_SERVICE_UNAVAILABLE');
  });
});
