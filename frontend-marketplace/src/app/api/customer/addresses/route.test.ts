import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GET, POST } from './route';
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

describe('Customer Addresses BFF Endpoints', () => {
  describe('GET /api/customer/addresses', () => {
    it('returns 401 when customer is not authenticated', async () => {
      vi.mocked(cookies).mockResolvedValue({
        get: vi.fn().mockReturnValue(undefined),
      } as unknown as Awaited<ReturnType<typeof cookies>>);

      const response = await GET();
      expect(response.status).toBe(401);
      const body = await response.json();
      expect(body.message).toBe('No autenticado.');
    });

    it('forwards address list from Spring backend when authenticated', async () => {
      vi.mocked(cookies).mockResolvedValue({
        get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
      } as unknown as Awaited<ReturnType<typeof cookies>>);

      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            addresses: [
              {
                id: 'addr-1',
                alias: 'Casa',
                recipientName: 'Juan Pérez',
                recipientPhone: '987654321',
                department: 'LIMA',
                province: 'LIMA',
                district: 'MIRAFLORES',
                ubigeoCode: '150122',
                addressLine: 'Av. Larco 123',
                isDefault: true,
              },
            ],
            total: 1,
          }),
          { status: 200 }
        )
      );

      const response = await GET();
      expect(response.status).toBe(200);
      const body = await response.json();
      expect(body.total).toBe(1);
      expect(body.addresses[0].alias).toBe('Casa');
    });
  });

  describe('POST /api/customer/addresses', () => {
    it('forwards creation request to Spring backend', async () => {
      vi.mocked(cookies).mockResolvedValue({
        get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
      } as unknown as Awaited<ReturnType<typeof cookies>>);

      const payload = {
        alias: 'Trabajo',
        recipientName: 'Juan Pérez',
        recipientPhone: '987654321',
        department: 'LIMA',
        province: 'LIMA',
        district: 'SAN ISIDRO',
        ubigeoCode: '150131',
        addressLine: 'Av. Rivera Navarrete 456',
        isDefault: false,
      };

      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            id: 'addr-2',
            ...payload,
          }),
          { status: 201 }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/addresses', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      const response = await POST(req);
      expect(response.status).toBe(201);
      const body = await response.json();
      expect(body.id).toBe('addr-2');
      expect(body.alias).toBe('Trabajo');
    });

    it('returns 409 when address limit exceeded', async () => {
      vi.mocked(cookies).mockResolvedValue({
        get: vi.fn().mockReturnValue({ value: 'valid-jwt-token', name: 'jaldishop_customer_token' }),
      } as unknown as Awaited<ReturnType<typeof cookies>>);

      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'ADDRESS_LIMIT_EXCEEDED',
            message: 'Has alcanzado el límite máximo de 5 direcciones registradas.',
          }),
          { status: 409 }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/addresses', {
        method: 'POST',
        body: JSON.stringify({}),
      });

      const response = await POST(req);
      expect(response.status).toBe(409);
      const body = await response.json();
      expect(body.code).toBe('ADDRESS_LIMIT_EXCEEDED');
    });
  });
});
