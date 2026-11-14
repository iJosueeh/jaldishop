import { beforeEach, describe, expect, it, vi } from 'vitest';
import { POST } from './route';
import { NextRequest } from 'next/server';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('Customer Reset Password BFF Endpoint (POST)', () => {
  const validToken = 'abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE'; // 43 chars base64url
  const validPassword = 'NuevaContrasena2026';

  describe('CSRF Protection', () => {
    it('returns 403 when origin is external/mismatched without calling upstream', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          origin: 'https://evil.example.org',
        },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(403);
      const data = await response.json();
      expect(data.code).toBe('FORBIDDEN');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('allows same-origin requests to proceed', async () => {
      fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          origin: 'http://localhost:3000',
        },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(204);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });
  });

  describe('Validation', () => {
    it('returns 400 when body fields are missing or not strings', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when token format is not 43 characters base64url', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: 'short-token', newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(data.message).toContain('formato del token');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when password is shorter than 8 characters', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: '1234567' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(data.message).toContain('al menos 8 caracteres');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when password exceeds 72 characters', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: 'a'.repeat(73) }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(data.message).toContain('72 caracteres');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when password exceeds 72 UTF-8 bytes (multibyte characters)', async () => {
      // 25 multibyte characters (3 bytes each = 75 bytes > 72 bytes constraint)
      const multibytePass = 'ñ'.repeat(37); // 37 * 2 bytes = 74 bytes
      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: multibytePass }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(data.message).toContain('72 bytes UTF-8');
      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  describe('Upstream Responses and Security Headers', () => {
    it('returns 204 No Content with no-store cache control on success', async () => {
      fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(204);
      expect(response.headers.get('Cache-Control')).toBe('no-store');
      expect(await response.text()).toBe('');
    });

    it('forwards 422 when token is invalid, expired, or already consumed', async () => {
      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'INVALID_PASSWORD_RESET_TOKEN',
            message: 'El token de recuperación no es válido o ha vencido.',
          }),
          { status: 422, headers: { 'Content-Type': 'application/json' } }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(422);
      expect(response.headers.get('Cache-Control')).toBe('no-store');
      const data = await response.json();
      expect(data.code).toBe('INVALID_PASSWORD_RESET_TOKEN');
      expect(data.message).toContain('El token de recuperación no es válido o ha vencido');
    });

    it('forwards 429 RateLimit with Retry-After header', async () => {
      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'Demasiadas solicitudes.',
          }),
          {
            status: 429,
            headers: {
              'Content-Type': 'application/json',
              'Retry-After': '90',
            },
          }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(429);
      expect(response.headers.get('Retry-After')).toBe('90');
      expect(response.headers.get('Cache-Control')).toBe('no-store');
    });

    it('handles upstream connection failure or timeout with 503', async () => {
      fetchMock.mockRejectedValue(new Error('Connection aborted'));

      const req = new NextRequest('http://localhost:3000/api/customer/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: validToken, newPassword: validPassword }),
      });

      const response = await POST(req);
      expect(response.status).toBe(503);
      const data = await response.json();
      expect(data.code).toBe('SERVICE_UNAVAILABLE');
    });
  });
});
