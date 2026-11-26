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
  it('registers successfully without auto-login and requires email verification', async () => {
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            id: 'user-uuid',
            email: 'cliente@example.com',
            firstName: 'Juan',
            lastName: 'Pérez',
            status: 'ACTIVE',
            emailVerified: false,
          }),
          { status: 201 }
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
    expect(data.authenticated).toBe(false);
    expect(data.autoLogin).toBe(false);
    expect(data.requiresEmailVerification).toBe(true);
    expect(data.email).toBe('cliente@example.com');
    expect(data.fullName).toBe('Juan');
    expect(response.cookies.get('jaldishop_customer_token')).toBeUndefined();
    expect(fetchMock).toHaveBeenCalledTimes(1);
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
