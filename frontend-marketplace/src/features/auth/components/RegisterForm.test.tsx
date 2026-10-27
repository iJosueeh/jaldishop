import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RegisterForm } from './RegisterForm';
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

function renderRegisterForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <RegisterForm />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
});

describe('RegisterForm Component', () => {
  it('renders all customer registration fields and link to login', () => {
    renderRegisterForm();

    expect(screen.getByRole('heading', { name: /Crea tu cuenta de cliente/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Nombres/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Apellidos/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Correo electrónico/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Celular/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Contraseña$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Confirmar contraseña$/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear mi cuenta' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Inicia sesión aquí/i })).toBeInTheDocument();
  });

  it('validates client-side constraints for empty fields and short password without calling API', async () => {
    renderRegisterForm();

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    expect(await screen.findByText('Ingresa tus nombres.')).toBeInTheDocument();
    expect(await screen.findByText('Ingresa tus apellidos.')).toBeInTheDocument();
    expect(await screen.findByText('Ingresa tu correo electrónico.')).toBeInTheDocument();
    expect(await screen.findByText('Crea una contraseña.')).toBeInTheDocument();
    expect(customerAuthService.register).not.toHaveBeenCalled();
  });

  it('validates that passwords must match before submitting', async () => {
    renderRegisterForm();

    fireEvent.change(screen.getByLabelText(/Nombres/i), { target: { value: 'Carlos' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/i), { target: { value: 'Rosas' } });
    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), { target: { value: 'carlos@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^Confirmar contraseña$/i), { target: { value: 'diferente123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(customerAuthService.register).not.toHaveBeenCalled();
  });

  it('validates Peruvian phone number when provided', async () => {
    renderRegisterForm();

    fireEvent.change(screen.getByLabelText(/Nombres/i), { target: { value: 'Carlos' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/i), { target: { value: 'Rosas' } });
    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), { target: { value: 'carlos@example.com' } });
    fireEvent.change(screen.getByLabelText(/Celular/i), { target: { value: '12345678' } }); // invalid
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^Confirmar contraseña$/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    expect(
      await screen.findByText('Ingresa un celular peruano válido (9 dígitos comenzando con 9).')
    ).toBeInTheDocument();
    expect(customerAuthService.register).not.toHaveBeenCalled();
  });

  it('submits valid registration, handles auto-login and redirects to target URL', async () => {
    mockSearchParams = new URLSearchParams('redirect=/tienda/panaderia');
    vi.mocked(customerAuthService.register).mockResolvedValue({
      authenticated: true,
      autoLogin: true,
      fullName: 'Carlos Rosas',
    });

    renderRegisterForm();

    fireEvent.change(screen.getByLabelText(/Nombres/i), { target: { value: 'Carlos' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/i), { target: { value: 'Rosas' } });
    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), { target: { value: 'carlos@example.com' } });
    fireEvent.change(screen.getByLabelText(/Celular/i), { target: { value: '987654321' } });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^Confirmar contraseña$/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    await waitFor(() => {
      expect(customerAuthService.register).toHaveBeenCalledWith({
        firstName: 'Carlos',
        lastName: 'Rosas',
        email: 'carlos@example.com',
        phone: '987654321',
        password: 'password123',
      });
      expect(mockPush).toHaveBeenCalledWith('/tienda/panaderia');
      expect(mockRefresh).toHaveBeenCalled();
    });
  });

  it('redirects to login with registered=true flag if auto-login is false', async () => {
    mockSearchParams = new URLSearchParams('redirect=/tienda/panaderia');
    vi.mocked(customerAuthService.register).mockResolvedValue({
      authenticated: false,
      autoLogin: false,
    });

    renderRegisterForm();

    fireEvent.change(screen.getByLabelText(/Nombres/i), { target: { value: 'Ana' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/i), { target: { value: 'Torres' } });
    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), { target: { value: 'ana@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^Confirmar contraseña$/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(
        expect.stringContaining('/login?registrado=true&redirect=')
      );
    });
  });

  it('displays field error when email is already registered (409 Conflict)', async () => {
    vi.mocked(customerAuthService.register).mockRejectedValue({
      errorData: {
        code: 'EMAIL_ALREADY_EXISTS',
        message: 'El correo ya está registrado.',
      },
    });

    renderRegisterForm();

    fireEvent.change(screen.getByLabelText(/Nombres/i), { target: { value: 'Ana' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/i), { target: { value: 'Torres' } });
    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), { target: { value: 'existente@example.com' } });
    fireEvent.change(screen.getByLabelText(/^Contraseña$/i), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText(/^Confirmar contraseña$/i), { target: { value: 'password123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));

    expect(
      await screen.findByText('Este correo ya se encuentra registrado.')
    ).toBeInTheDocument();
  });
});
