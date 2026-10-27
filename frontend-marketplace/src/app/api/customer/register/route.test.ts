import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { POST } from './route';

vi.mock('@/core/api/customerProxy', () => ({
  CUSTOMER_COOKIE: 'jaldishop_customer_token',
  rejectCrossOrigin: () => null,
}));

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

const createRegisterRequest = (body: Record<string, unknown>) =>
  new NextRequest('http://localhost:3000/api/customer/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

describe('Customer register route handler (BFF)', () => {
  it('registers successfully and performs auto-login setting HttpOnly cookie', async () => {
    // 1. Mock upstream /auth/register -> 201 Created
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            id: 'user-uuid',
            email: 'cliente@example.com',
            firstName: 'Juan',
            lastName: 'Pérez',
            status: 'ACTIVE',
          }),
          { status: 201 }
        )
      )
    );

    // 2. Mock upstream /auth/login (auto-login) -> 200 OK with token & CUSTOMER role
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            token: 'valid-jwt-token',
            roles: ['CUSTOMER'],
            fullName: 'Juan Pérez',
          }),
          { status: 200 }
        )
      )
    );

    const request = createRegisterRequest({
      firstName: 'Juan',
      lastName: 'Pérez',
      email: 'cliente@example.com',
      password: 'password123',
      phone: '987654321',
    });

    const response = await POST(request);
    expect(response.status).toBe(201);

    const data = await response.json();
    expect(data.authenticated).toBe(true);
    expect(data.autoLogin).toBe(true);
    expect(data.fullName).toBe('Juan Pérez');

    expect(response.cookies.get('jaldishop_customer_token')?.value).toBe('valid-jwt-token');
    expect(response.headers.get('set-cookie')).toContain('HttpOnly');
    expect(response.headers.get('set-cookie')).toContain('SameSite=lax');
  });

  it('handles registration success when auto-login fails gracefully without repeating register', async () => {
    // 1. Upstream /auth/register succeeds
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            id: 'user-uuid',
            email: 'cliente@example.com',
            firstName: 'Ana',
            lastName: 'Gómez',
          }),
          { status: 201 }
        )
      )
    );

    // 2. Upstream /auth/login fails (e.g., 500 or timeout)
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({ message: 'Internal server error' }),
          { status: 500 }
        )
      )
    );

    const request = createRegisterRequest({
      firstName: 'Ana',
      lastName: 'Gómez',
      email: 'cliente@example.com',
      password: 'password123',
    });

    const response = await POST(request);
    expect(response.status).toBe(201);

    const data = await response.json();
    expect(data.authenticated).toBe(false);
    expect(data.autoLogin).toBe(false);
    expect(data.message).toContain('inicia sesión');
    expect(response.cookies.get('jaldishop_customer_token')).toBeUndefined();
  });

  it('returns 409 when email is already registered', async () => {
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            code: 'EMAIL_ALREADY_EXISTS',
            message: 'El correo ya esta registrado.',
          }),
          { status: 409 }
        )
      )
    );

    const request = createRegisterRequest({
      firstName: 'Carlos',
      lastName: 'Rios',
      email: 'existente@example.com',
      password: 'password123',
    });

    const response = await POST(request);
    expect(response.status).toBe(409);

    const data = await response.json();
    expect(data.code).toBe('EMAIL_ALREADY_EXISTS');
  });

  it('rejects invalid Peruvian phone format with 400 Bad Request before calling backend', async () => {
    const request = createRegisterRequest({
      firstName: 'Pedro',
      lastName: 'Picapiedra',
      email: 'pedro@example.com',
      password: 'password123',
      phone: '12345678', // Invalid (not 9 digits starting with 9)
    });

    const response = await POST(request);
    expect(response.status).toBe(400);

    const data = await response.json();
    expect(data.code).toBe('VALIDATION_ERROR');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('rejects passwords shorter than 6 characters before calling backend', async () => {
    const request = createRegisterRequest({
      firstName: 'Rosa',
      lastName: 'Luna',
      email: 'rosa@example.com',
      password: '123', // < 6 chars
    });

    const response = await POST(request);
    expect(response.status).toBe(400);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
