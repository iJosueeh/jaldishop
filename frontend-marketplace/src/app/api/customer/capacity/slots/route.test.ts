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

describe('Capacity Slots BFF Endpoint (GET /api/customer/capacity/slots)', () => {
  const validStoreId = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

  it('returns 401 when customer cookie is missing', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const request = new NextRequest(`http://localhost:3000/api/customer/capacity/slots?storeId=${validStoreId}`);
    const response = await GET(request);

    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.code).toBe('UNAUTHENTICATED');
  });

  it('returns 400 when storeId is missing', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'token-123', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const request = new NextRequest('http://localhost:3000/api/customer/capacity/slots');
    const response = await GET(request);

    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.code).toBe('INVALID_STORE_ID');
  });

  it('returns 400 when storeId is not a valid UUID', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'token-123', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const request = new NextRequest('http://localhost:3000/api/customer/capacity/slots?storeId=not-a-uuid');
    const response = await GET(request);

    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.code).toBe('INVALID_STORE_ID');
  });

  it('returns 400 when date format is invalid', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'token-123', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const request = new NextRequest(`http://localhost:3000/api/customer/capacity/slots?storeId=${validStoreId}&date=13-11-2026`);
    const response = await GET(request);

    expect(response.status).toBe(400);
    const body = await response.json();
    expect(body.code).toBe('INVALID_DATE_FORMAT');
  });

  it('forwards slots from Spring backend when authenticated and query valid', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const mockBackendResponse = {
      storeId: validStoreId,
      serviceDate: '2026-11-13',
      slots: [
        {
          storeId: validStoreId,
          serviceDate: '2026-11-13',
          startTime: '09:00:00',
          endTime: '11:00:00',
          effectiveCapacity: 10,
          availableCapacity: 8,
          reservedCapacity: 2,
          committedCapacity: 0,
          occupiedCapacity: 2,
        },
      ],
    };

    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(mockBackendResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    const request = new NextRequest(`http://localhost:3000/api/customer/capacity/slots?storeId=${validStoreId}&date=2026-11-13`);
    const response = await GET(request);

    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body.slots).toHaveLength(1);
    expect(body.slots[0].startTime).toBe('09:00:00');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(`/capacity/slots?storeId=${validStoreId}&date=2026-11-13`),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer valid-jwt-token',
        }),
      })
    );
  });

  it('handles session expiration (401 from backend)', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'expired-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ code: 'UNAUTHORIZED' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    const request = new NextRequest(`http://localhost:3000/api/customer/capacity/slots?storeId=${validStoreId}`);
    const response = await GET(request);

    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.code).toBe('SESSION_EXPIRED');
  });

  it('handles backend network failure with 503', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockRejectedValue(new Error('Connection timeout'));

    const request = new NextRequest(`http://localhost:3000/api/customer/capacity/slots?storeId=${validStoreId}`);
    const response = await GET(request);

    expect(response.status).toBe(503);
    const body = await response.json();
    expect(body.code).toBe('CAPACITY_UNAVAILABLE');
  });
});
