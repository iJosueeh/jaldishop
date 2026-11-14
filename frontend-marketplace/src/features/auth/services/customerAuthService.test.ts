import { beforeEach, describe, expect, it, vi } from 'vitest';
import { customerAuthService } from './customerAuthService';
import { apiClient } from '@/core/api/apiClient';

vi.mock('@/core/api/apiClient', () => ({
  apiClient: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('customerAuthService', () => {
  it('calls /api/customer/session with GET for getSession', async () => {
    vi.mocked(apiClient).mockResolvedValue({ authenticated: true, fullName: 'Cliente' });

    const result = await customerAuthService.getSession();
    expect(result).toEqual({ authenticated: true, fullName: 'Cliente' });
    expect(apiClient).toHaveBeenCalledWith('/api/customer/session', {
      baseUrl: '',
      cache: 'no-store',
    });
  });

  it('calls /api/customer/session with POST for login', async () => {
    vi.mocked(apiClient).mockResolvedValue({ authenticated: true });

    await customerAuthService.login({ email: 'test@example.com', password: 'password123' });
    expect(apiClient).toHaveBeenCalledWith('/api/customer/session', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify({ email: 'test@example.com', password: 'password123' }),
    });
  });

  it('calls /api/customer/register with POST for register', async () => {
    vi.mocked(apiClient).mockResolvedValue({ authenticated: true, autoLogin: true });

    const data = {
      firstName: 'Juan',
      lastName: 'Pérez',
      email: 'juan@example.com',
      password: 'password123',
    };
    await customerAuthService.register(data);
    expect(apiClient).toHaveBeenCalledWith('/api/customer/register', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify(data),
    });
  });

  it('calls /api/customer/session with DELETE for logout', async () => {
    vi.mocked(apiClient).mockResolvedValue({ authenticated: false });

    await customerAuthService.logout();
    expect(apiClient).toHaveBeenCalledWith('/api/customer/session', {
      baseUrl: '',
      method: 'DELETE',
    });
  });

  it('calls /api/customer/forgot-password with POST for forgotPassword', async () => {
    vi.mocked(apiClient).mockResolvedValue({
      message: 'Si el correo corresponde a una cuenta habilitada...',
    });

    const result = await customerAuthService.forgotPassword({ email: 'usuario@example.com' });
    expect(result.message).toContain('Si el correo corresponde');
    expect(apiClient).toHaveBeenCalledWith('/api/customer/forgot-password', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify({ email: 'usuario@example.com' }),
    });
  });

  it('calls /api/customer/reset-password with POST for resetPassword', async () => {
    vi.mocked(apiClient).mockResolvedValue(undefined as unknown as void);

    await customerAuthService.resetPassword({
      token: 'abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE',
      newPassword: 'NuevaContrasena2026',
    });
    expect(apiClient).toHaveBeenCalledWith('/api/customer/reset-password', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify({
        token: 'abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE',
        newPassword: 'NuevaContrasena2026',
      }),
    });
  });
});
