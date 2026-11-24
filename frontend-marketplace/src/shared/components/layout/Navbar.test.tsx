import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Navbar } from './Navbar';

vi.mock('next/navigation', () => ({
  usePathname: () => '/tiendas',
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/features/customer/hooks/useCustomerSession', () => ({
  useCustomerSession: () => ({
    isAuthenticated: false,
    user: null,
    logout: vi.fn(),
  }),
}));

vi.mock('@/features/customer/hooks/useCustomerNotifications', () => ({
  useUnreadNotificationsCount: () => ({ unreadCount: 0 }),
}));

vi.mock('@/features/storefront/cart/StorefrontCart', () => ({
  StorefrontCart: () => <div data-testid="storefront-cart" />,
}));

describe('Navbar Component', () => {
  it('renders real marketplace navigation routes and marks active route', () => {
    render(<Navbar />);

    const tiendasLink = screen.getByRole('link', { name: 'Tiendas' });
    expect(tiendasLink).toHaveAttribute('href', '/tiendas');
    expect(tiendasLink).toHaveAttribute('aria-current', 'page');

    expect(screen.getByRole('link', { name: 'Productos' })).toHaveAttribute('href', '/productos');
    expect(screen.getByRole('link', { name: '¿Cómo funciona?' })).toHaveAttribute(
      'href',
      '/como-funciona'
    );
    expect(screen.getByRole('link', { name: 'Para Comercios' })).toHaveAttribute(
      'href',
      '/para-comercios'
    );
  });
});
