import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ForgotPasswordForm } from './ForgotPasswordForm';
import { customerAuthService } from '../services/customerAuthService';

vi.mock('../services/customerAuthService', () => ({
  customerAuthService: {
    forgotPassword: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ForgotPasswordForm Component', () => {
  it('renders all form elements, labels, and back to login link', () => {
    render(<ForgotPasswordForm />);

    expect(
      screen.getByRole('heading', { name: /¿Olvidaste tu contraseña\?/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Correo electrónico/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Enviar enlace de recuperación/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /Inicia sesión aquí/i })
    ).toHaveAttribute('href', '/login');
  });

  it('validates email field on client side before calling API', async () => {
    render(<ForgotPasswordForm />);

    const submitBtn = screen.getByRole('button', {
      name: /Enviar enlace de recuperación/i,
    });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText('Ingresa tu correo electrónico.')
    ).toBeInTheDocument();
    expect(customerAuthService.forgotPassword).not.toHaveBeenCalled();

    const emailInput = screen.getByLabelText(/Correo electrónico/i);
    fireEvent.change(emailInput, { target: { value: 'formato-invalido' } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/formato válido/i)
    ).toBeInTheDocument();
    expect(customerAuthService.forgotPassword).not.toHaveBeenCalled();
  });

  it('submits valid email, transitions to generic anti-enumeration success view', async () => {
    vi.mocked(customerAuthService.forgotPassword).mockResolvedValue({
      message: 'Si el correo corresponde a una cuenta habilitada, recibirás instrucciones para restablecer la contraseña.',
    });

    render(<ForgotPasswordForm />);

    const emailInput = screen.getByLabelText(/Correo electrónico/i);
    fireEvent.change(emailInput, { target: { value: 'cliente@ejemplo.pe' } });

    const submitBtn = screen.getByRole('button', {
      name: /Enviar enlace de recuperación/i,
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(customerAuthService.forgotPassword).toHaveBeenCalledWith({
        email: 'cliente@ejemplo.pe',
      });
      expect(
        screen.getByTestId('forgot-password-success')
      ).toBeInTheDocument();
      expect(
        screen.getByRole('heading', { name: /Revisa tu correo/i })
      ).toBeInTheDocument();
    });

    // Check link to return to login
    const backLinks = screen.getAllByRole('link', { name: /Volver al inicio de sesión/i });
    expect(backLinks.length).toBeGreaterThan(0);
    expect(backLinks[0]).toHaveAttribute('href', '/login');
  });

  it('allows user to retry with another email from the success view', async () => {
    vi.mocked(customerAuthService.forgotPassword).mockResolvedValue({
      message: 'Instrucciones enviadas.',
    });

    render(<ForgotPasswordForm />);

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'cliente@ejemplo.pe' },
    });
    fireEvent.click(
      screen.getByRole('button', { name: /Enviar enlace de recuperación/i })
    );

    await waitFor(() => {
      expect(screen.getByTestId('forgot-password-success')).toBeInTheDocument();
    });

    const retryBtn = screen.getByRole('button', {
      name: /Intentar con otro correo electrónico/i,
    });
    fireEvent.click(retryBtn);

    expect(
      screen.getByRole('heading', { name: /¿Olvidaste tu contraseña\?/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Correo electrónico/i)).toHaveValue('');
  });

  it('displays error message when request fails due to network or rate limit', async () => {
    vi.mocked(customerAuthService.forgotPassword).mockRejectedValue(
      new Error('Demasiadas solicitudes. Por favor espera antes de reintentar.')
    );

    render(<ForgotPasswordForm />);

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'cliente@ejemplo.pe' },
    });
    fireEvent.click(
      screen.getByRole('button', { name: /Enviar enlace de recuperación/i })
    );

    expect(
      await screen.findByText(
        'Demasiadas solicitudes. Por favor espera antes de reintentar.'
      )
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId('forgot-password-success')
    ).not.toBeInTheDocument();
  });
});
