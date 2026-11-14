import { beforeEach, describe, expect, it, vi } from 'vitest';
import { POST } from './route';
import { NextRequest } from 'next/server';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('Customer Forgot Password BFF Endpoint (POST)', () => {
  describe('CSRF Protection', () => {
    it('returns 403 when origin is external/mismatched without calling upstream', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          origin: 'https://attacker.site',
        },
        body: JSON.stringify({ email: 'usuario@example.com' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(403);
      const data = await response.json();
      expect(data.code).toBe('FORBIDDEN');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('allows same-origin requests to proceed', async () => {
      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            message: 'Si el correo corresponde a una cuenta habilitada, recibirás instrucciones para restablecer la contraseña.',
          }),
          { status: 202, headers: { 'Content-Type': 'application/json' } }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          origin: 'http://localhost:3000',
        },
        body: JSON.stringify({ email: 'usuario@example.com' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(202);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });
  });

  describe('Validation', () => {
    it('returns 400 when body is empty or email is not a string', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 12345 }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when email format is invalid', async () => {
      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'correo-invalido' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('returns 400 when email exceeds 254 characters', async () => {
      const longEmail = `${'a'.repeat(250)}@test.com`;
      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: longEmail }),
      });

      const response = await POST(req);
      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.code).toBe('VALIDATION_ERROR');
      expect(fetchMock).not.toHaveBeenCalled();
    });
  });

  describe('Upstream Forwarding and Security Headers', () => {
    it('forwards request to backend and returns 202 with no-store cache control', async () => {
      fetchMock.mockResolvedValue(
        new Response(
          JSON.stringify({
            message: 'Si el correo corresponde a una cuenta habilitada, recibirás instrucciones para restablecer la contraseña.',
          }),
          { status: 202, headers: { 'Content-Type': 'application/json' } }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: '  USUARIO@EXAMPLE.COM  ' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(202);
      expect(response.headers.get('Cache-Control')).toBe('no-store');

      const data = await response.json();
      expect(data.message).toContain('Si el correo corresponde a una cuenta habilitada');

      // Verifies normalization in payload sent to backend
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/auth/forgot-password'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ email: 'usuario@example.com' }),
        })
      );
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
              'Retry-After': '60',
            },
          }
        )
      );

      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'usuario@example.com' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(429);
      expect(response.headers.get('Retry-After')).toBe('60');
      expect(response.headers.get('Cache-Control')).toBe('no-store');
    });

    it('handles upstream network error or timeout with 503', async () => {
      fetchMock.mockRejectedValue(new Error('Network timeout'));

      const req = new NextRequest('http://localhost:3000/api/customer/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'usuario@example.com' }),
      });

      const response = await POST(req);
      expect(response.status).toBe(503);
      const data = await response.json();
      expect(data.code).toBe('SERVICE_UNAVAILABLE');
      expect(response.headers.get('Cache-Control')).toBe('no-store');
    });
  });
});
