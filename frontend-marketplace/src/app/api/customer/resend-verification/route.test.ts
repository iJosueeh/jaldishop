import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { POST } from './route';

vi.mock('@/core/api/customerProxy', () => ({
  rejectCrossOrigin: () => null,
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

const createRequest = (body: Record<string, unknown>) =>
  new NextRequest('http://localhost:3000/api/customer/resend-verification', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

describe('Customer resend-verification route handler (BFF)', () => {
  it('forwards normalized email and returns 202 with Cache-Control: no-store', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          message: 'Si tu correo corresponde a una cuenta pendiente de verificación, recibirás un nuevo enlace.',
        }),
        { status: 202 }
      )
    );

    const response = await POST(createRequest({ email: '  ANA@EXAMPLE.COM ' }));
    expect(response.status).toBe(202);
    expect(response.headers.get('Cache-Control')).toBe('no-store');
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/auth/resend-verification'),
      expect.objectContaining({
        body: JSON.stringify({ email: 'ana@example.com' }),
      })
    );
  });

  it('rejects invalid email with 400', async () => {
    const response = await POST(createRequest({ email: 'invalid-email' }));
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('propagates 429 and Retry-After header from backend', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Too many requests' }), {
        status: 429,
        headers: { 'Retry-After': '45' },
      })
    );

    const response = await POST(createRequest({ email: 'ana@example.com' }));
    expect(response.status).toBe(429);
    expect(response.headers.get('Retry-After')).toBe('45');
  });
});
