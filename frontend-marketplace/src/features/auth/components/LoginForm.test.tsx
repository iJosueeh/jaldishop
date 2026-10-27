import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LoginForm } from './LoginForm';
import { customerAuthService } from '../services/customerAuthService';

const mockPush = vi.fn();
const mockRefresh = vi.fn();
let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    refresh: mockRefresh,
  }),
  useSearchParams: () => mockSearchParams,
}));

vi.mock('../services/customerAuthService', () => ({
  customerAuthService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getSession: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

function renderLoginForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <LoginForm />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
});

describe('LoginForm Component', () => {
  it('renders all form elements, labels, and accessible inputs', () => {
    renderLoginForm();

    expect(screen.getByRole('heading', { name: /Bienvenido de vuelta/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Correo electrónico/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Contraseña$/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Iniciar Sesión' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Regístrate aquí/i })).toBeInTheDocument();
  });

  it('toggles password visibility between text and password', () => {
    renderLoginForm();

    const passwordInput = screen.getByLabelText(/^Contraseña$/i) as HTMLInputElement;
    expect(passwordInput.type).toBe('password');

    const toggleButton = screen.getByRole('button', { name: 'Mostrar contraseña' });
    fireEvent.click(toggleButton);
    expect(passwordInput.type).toBe('text');

    const hideButton = screen.getByRole('button', { name: 'Ocultar contraseña' });
    fireEvent.click(hideButton);
    expect(passwordInput.type).toBe('password');
  });

  it('validates required fields on client side without calling login API', async () => {
    renderLoginForm();

    const submitButton = screen.getByRole('button', { name: 'Iniciar Sesión' });
    fireEvent.click(submitButton);

    expect(await screen.findByText('Ingresa tu correo electrónico.')).toBeInTheDocument();
    expect(await screen.findByText('Ingresa tu contraseña.')).toBeInTheDocument();
    expect(customerAuthService.login).not.toHaveBeenCalled();
  });

  it('submits valid credentials, handles successful login, and navigates to safe redirect', async () => {
    mockSearchParams = new URLSearchParams('redirect=/tienda/dulces-lima');
    vi.mocked(customerAuthService.login).mockResolvedValue({
      authenticated: true,
      fullName: 'María Santos',
    });

    renderLoginForm();

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'maria@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), {
      target: { value: 'secreta123' },
    });

    const submitButton = screen.getByRole('button', { name: 'Iniciar Sesión' });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(customerAuthService.login).toHaveBeenCalledWith({
        email: 'maria@example.com',
        password: 'secreta123',
      });
      expect(mockPush).toHaveBeenCalledWith('/tienda/dulces-lima');
      expect(mockRefresh).toHaveBeenCalled();
    });
  });

  it('displays error message when login fails with invalid credentials', async () => {
    vi.mocked(customerAuthService.login).mockRejectedValue(
      new Error('Correo o contraseña incorrectos.')
    );

    renderLoginForm();

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'maria@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), {
      target: { value: 'erronea' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(
      await screen.findByText('Correo o contraseña incorrectos.')
    ).toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('preserves redirect URL when linking to register page', () => {
    mockSearchParams = new URLSearchParams('redirect=/tienda/artesanias-cusco');
    renderLoginForm();

    const registerLink = screen.getByRole('link', { name: /Regístrate aquí/i });
    expect(registerLink.getAttribute('href')).toContain(
      encodeURIComponent('/tienda/artesanias-cusco')
    );
  });
});
