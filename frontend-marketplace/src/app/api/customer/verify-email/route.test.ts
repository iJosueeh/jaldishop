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

const validToken = 'A'.repeat(43);

const createRequest = (body: Record<string, unknown>) =>
  new NextRequest('http://localhost:3000/api/customer/verify-email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

describe('Customer verify-email route handler (BFF)', () => {
  it('returns 204 with Cache-Control: no-store when backend verifies token', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    const response = await POST(createRequest({ token: validToken }));
    expect(response.status).toBe(204);
    expect(response.headers.get('Cache-Control')).toBe('no-store');
  });

  it('rejects malformed token with 422 without calling backend', async () => {
    const response = await POST(createRequest({ token: 'bad-token' }));
    expect(response.status).toBe(422);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('forwards 422 INVALID_EMAIL_VERIFICATION_TOKEN from backend', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          code: 'INVALID_EMAIL_VERIFICATION_TOKEN',
          message: 'El enlace de verificación no es válido o ha vencido.',
        }),
        { status: 422 }
      )
    );

    const response = await POST(createRequest({ token: validToken }));
    expect(response.status).toBe(422);
    const data = await response.json();
    expect(data.code).toBe('INVALID_EMAIL_VERIFICATION_TOKEN');
  });
});
