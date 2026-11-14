import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ResetPasswordForm } from './ResetPasswordForm';
import { customerAuthService } from '../services/customerAuthService';
import { ApiException } from '@/core/api/apiClient';

let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useSearchParams: () => mockSearchParams,
}));

vi.mock('../services/customerAuthService', () => ({
  customerAuthService: {
    resetPassword: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
  window.history.pushState({}, '', '/');
  vi.spyOn(window.history, 'replaceState');
});

describe('ResetPasswordForm Component', () => {
  const validToken = 'abcdefghijklmnopqrstuvwxyz0123456789_-ABCDE'; // 43 chars base64url

  it('displays invalid/expired view when token parameter is missing from URL', () => {
    mockSearchParams = new URLSearchParams('');
    render(<ResetPasswordForm />);

    expect(screen.getByTestId('reset-password-invalid-token')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: /Enlace vencido o inválido/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Solicitar un nuevo enlace/i })
    ).toHaveAttribute('href', '/forgot-password');
  });

  it('displays invalid/expired view when token format is not 43 characters base64url', () => {
    mockSearchParams = new URLSearchParams('token=malformed-short-token');
    render(<ResetPasswordForm />);

    expect(screen.getByTestId('reset-password-invalid-token')).toBeInTheDocument();
  });

  it('captures valid token, cleans URL from address bar and displays form', () => {
    window.history.pushState({}, '', `/reset-password?token=${validToken}`);
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    render(<ResetPasswordForm />);

    expect(window.history.replaceState).toHaveBeenCalledWith(
      {},
      expect.any(String),
      '/reset-password'
    );

    expect(
      screen.getByRole('heading', { name: /Restablecer contraseña/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/^Nueva contraseña$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Confirmar nueva contraseña$/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Actualizar contraseña/i })
    ).toBeInTheDocument();
  });

  it('toggles password visibility for both password and confirmation fields', () => {
    window.history.pushState({}, '', `/reset-password?token=${validToken}`);
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    render(<ResetPasswordForm />);

    const passInput = screen.getByLabelText(/^Nueva contraseña$/i) as HTMLInputElement;
    const confirmInput = screen.getByLabelText(/^Confirmar nueva contraseña$/i) as HTMLInputElement;

    expect(passInput.type).toBe('password');
    expect(confirmInput.type).toBe('password');

    const togglePassBtn = screen.getByRole('button', { name: 'Mostrar nueva contraseña' });
    fireEvent.click(togglePassBtn);
    expect(passInput.type).toBe('text');

    const toggleConfirmBtn = screen.getByRole('button', { name: 'Mostrar confirmación' });
    fireEvent.click(toggleConfirmBtn);
    expect(confirmInput.type).toBe('text');
  });

  it('validates password constraints on client side before calling API', async () => {
    window.history.pushState({}, '', `/reset-password?token=${validToken}`);
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    render(<ResetPasswordForm />);

    const submitBtn = screen.getByRole('button', { name: /Actualizar contraseña/i });
    fireEvent.click(submitBtn);

    expect(await screen.findByText('Ingresa una nueva contraseña.')).toBeInTheDocument();
    expect(await screen.findByText('Confirma tu contraseña.')).toBeInTheDocument();
    expect(customerAuthService.resetPassword).not.toHaveBeenCalled();

    // Test password < 8 characters
    const passInput = screen.getByLabelText(/^Nueva contraseña$/i);
    const confirmInput = screen.getByLabelText(/^Confirmar nueva contraseña$/i);

    fireEvent.change(passInput, { target: { value: '1234567' } });
    fireEvent.change(confirmInput, { target: { value: '1234567' } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText('La contraseña debe tener al menos 8 caracteres.')
    ).toBeInTheDocument();

    // Test mismatched passwords
    fireEvent.change(passInput, { target: { value: 'Segura12345' } });
    fireEvent.change(confirmInput, { target: { value: 'Diferente12345' } });
    fireEvent.click(submitBtn);

    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(customerAuthService.resetPassword).not.toHaveBeenCalled();
  });

  it('validates UTF-8 multibyte boundary (max 72 bytes constraint)', async () => {
    window.history.pushState({}, '', `/reset-password?token=${validToken}`);
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    render(<ResetPasswordForm />);

    const passInput = screen.getByLabelText(/^Nueva contraseña$/i);
    const confirmInput = screen.getByLabelText(/^Confirmar nueva contraseña$/i);
    const submitBtn = screen.getByRole('button', { name: /Actualizar contraseña/i });

    // 37 multibyte characters * 2 bytes = 74 bytes > 72 bytes
    const multibyte = 'ñ'.repeat(37);
    fireEvent.change(passInput, { target: { value: multibyte } });
    fireEvent.change(confirmInput, { target: { value: multibyte } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText('La contraseña no puede superar los 72 bytes UTF-8.')
    ).toBeInTheDocument();
    expect(customerAuthService.resetPassword).not.toHaveBeenCalled();
  });

  it('submits valid new password and transitions to success state without auto-login', async () => {
    window.history.pushState({}, '', `/reset-password?token=${validToken}`);
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    vi.mocked(customerAuthService.resetPassword).mockResolvedValue(undefined);

    render(<ResetPasswordForm />);

    fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
      target: { value: 'NuevaContrasenaSegura2026' },
    });
    fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
      target: { value: 'NuevaContrasenaSegura2026' },
    });

    const submitBtn = screen.getByRole('button', { name: /Actualizar contraseña/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(customerAuthService.resetPassword).toHaveBeenCalledWith({
        token: validToken,
        newPassword: 'NuevaContrasenaSegura2026',
      });
      expect(screen.getByTestId('reset-password-success')).toBeInTheDocument();
      expect(
        screen.getByRole('heading', { name: /¡Contraseña actualizada!/i })
      ).toBeInTheDocument();
    });

    // Verifies link to login exists (no auto-login)
    expect(
      screen.getByRole('link', { name: /Iniciar sesión con nueva contraseña/i })
    ).toHaveAttribute('href', '/login');
  });

  describe('Structured Error Classification via ApiException', () => {
    it('transitions to invalid/expired token view when backend responds with structured 422', async () => {
      window.history.pushState({}, '', `/reset-password?token=${validToken}`);
      mockSearchParams = new URLSearchParams(`token=${validToken}`);
      vi.mocked(customerAuthService.resetPassword).mockRejectedValue(
        new ApiException(422, 'Unprocessable Entity', {
          code: 'INVALID_PASSWORD_RESET_TOKEN',
          message: 'El token de recuperación no es válido o ha vencido.',
        })
      );

      render(<ResetPasswordForm />);

      fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });
      fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Actualizar contraseña/i }));

      await waitFor(() => {
        expect(screen.getByTestId('reset-password-invalid-token')).toBeInTheDocument();
        expect(
          screen.getByRole('heading', { name: /Enlace vencido o inválido/i })
        ).toBeInTheDocument();
      });
    });

    it('preserves form and displays validation message when backend responds with structured 400', async () => {
      window.history.pushState({}, '', `/reset-password?token=${validToken}`);
      mockSearchParams = new URLSearchParams(`token=${validToken}`);
      vi.mocked(customerAuthService.resetPassword).mockRejectedValue(
        new ApiException(400, 'Bad Request', {
          code: 'VALIDATION_ERROR',
          message: 'La contraseña debe contener al menos 8 caracteres y como máximo 72 bytes UTF-8.',
        })
      );

      render(<ResetPasswordForm />);

      fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });
      fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Actualizar contraseña/i }));

      expect(
        await screen.findByText('La contraseña debe contener al menos 8 caracteres y como máximo 72 bytes UTF-8.')
      ).toBeInTheDocument();
      expect(screen.queryByTestId('reset-password-invalid-token')).not.toBeInTheDocument();
    });

    it('preserves form and displays rate limit message when backend responds with structured 429', async () => {
      window.history.pushState({}, '', `/reset-password?token=${validToken}`);
      mockSearchParams = new URLSearchParams(`token=${validToken}`);
      vi.mocked(customerAuthService.resetPassword).mockRejectedValue(
        new ApiException(429, 'Too Many Requests', {
          code: 'RATE_LIMIT_EXCEEDED',
          message: 'Demasiadas solicitudes. Por favor, espera antes de reintentar.',
        })
      );

      render(<ResetPasswordForm />);

      fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });
      fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Actualizar contraseña/i }));

      expect(
        await screen.findByText('Demasiadas solicitudes. Por favor, espera antes de reintentar.')
      ).toBeInTheDocument();
      expect(screen.queryByTestId('reset-password-invalid-token')).not.toBeInTheDocument();
    });

    it('preserves form and displays service unavailable message when backend responds with structured 503', async () => {
      window.history.pushState({}, '', `/reset-password?token=${validToken}`);
      mockSearchParams = new URLSearchParams(`token=${validToken}`);
      vi.mocked(customerAuthService.resetPassword).mockRejectedValue(
        new ApiException(503, 'Service Unavailable', {
          code: 'SERVICE_UNAVAILABLE',
          message: 'No pudimos conectar con el servicio.',
        })
      );

      render(<ResetPasswordForm />);

      fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });
      fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Actualizar contraseña/i }));

      expect(
        await screen.findByText(/El servicio no se encuentra disponible temporalmente/i)
      ).toBeInTheDocument();
      expect(screen.queryByTestId('reset-password-invalid-token')).not.toBeInTheDocument();
    });

    it('does NOT treat a generic Error containing the string "422" as an invalid token', async () => {
      window.history.pushState({}, '', `/reset-password?token=${validToken}`);
      mockSearchParams = new URLSearchParams(`token=${validToken}`);
      vi.mocked(customerAuthService.resetPassword).mockRejectedValue(
        new Error('Unexpected error occurred at step 422 in processing pipeline.')
      );

      render(<ResetPasswordForm />);

      fireEvent.change(screen.getByLabelText(/^Nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });
      fireEvent.change(screen.getByLabelText(/^Confirmar nueva contraseña$/i), {
        target: { value: 'NuevaContrasena2026' },
      });

      fireEvent.click(screen.getByRole('button', { name: /Actualizar contraseña/i }));

      expect(
        await screen.findByText('Unexpected error occurred at step 422 in processing pipeline.')
      ).toBeInTheDocument();
      // Verifies form is preserved and is NOT treated as an invalid token!
      expect(screen.queryByTestId('reset-password-invalid-token')).not.toBeInTheDocument();
    });
  });
});
