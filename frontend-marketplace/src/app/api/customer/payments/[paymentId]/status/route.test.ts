import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GET } from './route';
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

describe('Customer Payment Status BFF: GET /api/customer/payments/{paymentId}/status', () => {
  const validPaymentId = '6c318881-8051-4f32-be43-851f5e8dae41';

  it('returns 401 when customer session cookie is absent', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.code).toBe('UNAUTHENTICATED');
  });

  it('returns 400 when paymentId is not a valid UUID', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/payments/not-a-uuid/status');
    const res = await GET(req, { params: Promise.resolve({ paymentId: 'not-a-uuid' }) });

    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('INVALID_PAYMENT_ID');
  });

  it('returns 200 with payment status when upstream succeeds', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const mockResponse = {
      paymentId: validPaymentId,
      paymentStatus: 'APPROVED',
      orderCreated: true,
      orderNumber: 'ORD-54321',
      orderStatus: 'CONFIRMED',
      hasIncidence: false,
      refundStatus: 'NOT_REQUIRED',
    };

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(mockResponse), { status: 200 })
    );

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.orderNumber).toBe('ORD-54321');
    expect(body.paymentStatus).toBe('APPROVED');
  });

  it('deletes cookie and returns 401 when upstream returns 401', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'expired-jwt' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Token expirado' }), { status: 401 })
    );

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.code).toBe('SESSION_EXPIRED');
    expect(res.headers.get('set-cookie')).toContain('jaldishop_customer_token=;');
  });

  it('returns 403 when payment belongs to another user', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Forbidden' }), { status: 403 })
    );

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(403);
    const body = await res.json();
    expect(body.code).toBe('FORBIDDEN');
  });

  it('returns 404 when payment does not exist', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Not found' }), { status: 404 })
    );

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(404);
    const body = await res.json();
    expect(body.code).toBe('PAYMENT_NOT_FOUND');
  });

  it('returns 503 on network timeout', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'jwt-token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockRejectedValueOnce(new Error('Network error'));

    const req = new NextRequest(`http://localhost:3000/api/customer/payments/${validPaymentId}/status`);
    const res = await GET(req, { params: Promise.resolve({ paymentId: validPaymentId }) });

    expect(res.status).toBe(503);
    const body = await res.json();
    expect(body.code).toBe('PAYMENT_SERVICE_UNAVAILABLE');
  });
});
