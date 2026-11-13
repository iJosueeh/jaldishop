import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PATCH } from './route';
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

describe('Customer Order Cancellation BFF Endpoint (PATCH)', () => {
  const defaultContext = {
    params: Promise.resolve({ orderNumber: 'ORD-20261113-0001' }),
  };

  it('returns 401 when customer is not authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.message).toBe('No autenticado.');
  });

  it('returns 400 when orderNumber is empty or blank', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/orders/%20/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, {
      params: Promise.resolve({ orderNumber: '   ' }),
    });
    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.message).toBe('Número de pedido inválido.');
  });

  it('returns 400 when reason exceeds 255 characters', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const tooLongReason = 'a'.repeat(256);
    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
      body: JSON.stringify({ reason: tooLongReason }),
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.message).toBe('El motivo de cancelación no puede superar los 255 caracteres.');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('returns 400 when body is malformed JSON', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
      body: '{invalid-json',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.message).toBe('El cuerpo de la solicitud no tiene un formato JSON válido.');
  });

  it('returns 400 when reason is not a string', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
      body: JSON.stringify({ reason: 12345 }),
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.message).toBe('El formato del motivo de cancelación no es válido.');
  });

  it('forwards cancellation to Spring Boot with Bearer token and reason', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          orderNumber: 'ORD-20261113-0001',
          status: 'CANCELLED',
        }),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
      body: JSON.stringify({ reason: 'Compré por error.' }),
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.status).toBe('CANCELLED');

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/customer/orders/ORD-20261113-0001/cancel'),
      expect.objectContaining({
        method: 'PATCH',
        headers: expect.objectContaining({
          Authorization: 'Bearer valid-jwt',
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify({ reason: 'Compré por error.' }),
      })
    );
  });

  it('forwards empty body if reason is omitted', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          orderNumber: 'ORD-20261113-0001',
          status: 'CANCELLED',
        }),
        { status: 200 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(200);

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/customer/orders/ORD-20261113-0001/cancel'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({}),
      })
    );
  });

  it('propagates upstream 422 business rule exception from Spring', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'RN-ORD-09',
          message: 'El pedido no se puede cancelar porque se encuentra en estado IN_PREPARATION.',
        }),
        { status: 422 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
      body: JSON.stringify({ reason: 'Ya no lo quiero' }),
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(422);
    const body = await response.json();
    expect(body.code).toBe('RN-ORD-09');
    expect(body.message).toContain('IN_PREPARATION');
  });

  it('propagates upstream 403 access denied when order belongs to another customer', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'ACCESS_DENIED',
          message: 'No tiene permisos para cancelar un pedido ajeno.',
        }),
        { status: 403 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(403);
    const body = await response.json();
    expect(body.code).toBe('ACCESS_DENIED');
  });

  it('clears cookie when upstream returns 401', async () => {
    const deleteCookieMock = vi.fn();
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'expired-jwt', name: 'jaldishop_customer_token' }),
      delete: deleteCookieMock,
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ message: 'Token expirado' }),
        { status: 401 }
      )
    );

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(401);
    expect(response.cookies.get('jaldishop_customer_token')).toBeDefined();
  });

  it('returns 503 on upstream network timeout or error', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockRejectedValue(new Error('Network timeout'));

    const req = new NextRequest('http://localhost:3000/api/customer/orders/ORD-20261113-0001/cancel', {
      method: 'PATCH',
    });

    const response = await PATCH(req, defaultContext);
    expect(response.status).toBe(503);
    const body = await response.json();
    expect(body.message).toContain('No pudimos procesar la cancelación en este momento');
  });
});
