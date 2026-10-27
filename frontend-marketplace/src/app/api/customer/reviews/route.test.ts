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

describe('Customer Reviews BFF Endpoint', () => {
  it('returns 401 when customer is not authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue(undefined),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    const response = await GET();
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body.message).toBe('No autenticado.');
  });

  it('forwards reviews list from Spring backend when authenticated', async () => {
    vi.mocked(cookies).mockResolvedValue({
      get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
    } as unknown as Awaited<ReturnType<typeof cookies>>);

    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 'rev-1',
            productId: 'prod-123',
            productName: 'Mueble de Madera',
            rating: 5,
            comment: 'Excelente acabado.',
          },
        ]),
        { status: 200 }
      )
    );

    const response = await GET();
    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toHaveLength(1);
    expect(body[0].productName).toBe('Mueble de Madera');
  });
});
