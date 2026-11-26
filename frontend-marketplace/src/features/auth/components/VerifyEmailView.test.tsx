import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { VerifyEmailView } from './VerifyEmailView';
import { customerAuthService } from '../services/customerAuthService';
import { ApiException } from '@/core/api/apiClient';

let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useSearchParams: () => mockSearchParams,
}));

vi.mock('../services/customerAuthService', () => ({
  customerAuthService: {
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
});

const validToken = 'A'.repeat(43);

describe('VerifyEmailView Component', () => {
  it('shows success state and CTA after verifying valid token', async () => {
    mockSearchParams = new URLSearchParams(`token=${validToken}&redirect=/checkout`);
    vi.mocked(customerAuthService.verifyEmail).mockResolvedValueOnce(undefined);

    render(<VerifyEmailView />);

    await waitFor(() => {
      expect(screen.getByTestId('verify-email-success')).toBeInTheDocument();
    });

    expect(customerAuthService.verifyEmail).toHaveBeenCalledTimes(1);
    expect(customerAuthService.verifyEmail).toHaveBeenCalledWith({ token: validToken });
    const cta = screen.getByRole('link', { name: /Iniciar sesión para continuar al checkout/i });
    expect(cta).toHaveAttribute('href', '/login?redirect=%2Fcheckout');
  });

  it('shows invalid_token state and allows resending verification email', async () => {
    mockSearchParams = new URLSearchParams('token=invalid');

    render(<VerifyEmailView />);

    expect(screen.getByTestId('verify-email-invalid')).toBeInTheDocument();
    expect(customerAuthService.verifyEmail).not.toHaveBeenCalled();

    vi.mocked(customerAuthService.resendVerification).mockResolvedValueOnce({
      message: 'Enlace reenviado.',
    });

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'ana@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Reenviar correo de verificación/i }));

    await waitFor(() => {
      expect(screen.getByText('Enlace reenviado.')).toBeInTheDocument();
    });
    expect(customerAuthService.resendVerification).toHaveBeenCalledWith({
      email: 'ana@example.com',
    });
  });

  it('shows network_error state with retry when backend is unavailable', async () => {
    mockSearchParams = new URLSearchParams(`token=${validToken}`);
    vi.mocked(customerAuthService.verifyEmail).mockRejectedValueOnce(
      new ApiException(503, 'Service Unavailable', { code: 'SERVICE_UNAVAILABLE', message: 'Servicio no disponible.' })
    );

    render(<VerifyEmailView />);

    await waitFor(() => {
      expect(screen.getByTestId('verify-email-network-error')).toBeInTheDocument();
    });

    vi.mocked(customerAuthService.verifyEmail).mockResolvedValueOnce(undefined);
    fireEvent.click(screen.getByRole('button', { name: /Reintentar verificación/i }));

    await waitFor(() => {
      expect(screen.getByTestId('verify-email-success')).toBeInTheDocument();
    });
  });
});
