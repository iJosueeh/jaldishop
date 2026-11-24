import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { CustomerNav } from './CustomerNav';

let mockPathname = '/mi-jaldishop';

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}));

vi.mock('../hooks/useCustomerNotifications', () => ({
  useUnreadNotificationsCount: () => ({ unreadCount: 4, isLoading: false, reload: vi.fn() }),
}));

vi.mock('../hooks/useCustomerProfile', () => ({
  useCustomerProfile: () => ({
    data: {
      id: 'u-1',
      email: 'ana@jaldishop.pe',
      firstName: 'Ana',
      lastName: 'Torres',
      phone: null,
      status: 'ACTIVE',
      roles: ['CUSTOMER'],
      createdAt: '2026-01-01T00:00:00Z',
    },
  }),
}));

beforeEach(() => {
  mockPathname = '/mi-jaldishop';
});

describe('CustomerNav Sidebar', () => {
  it('renders all 9 required marketplace sidebar items and unread badge', () => {
    render(<CustomerNav />);

    const expectedLinks = [
      { name: 'Inicio', href: '/mi-jaldishop' },
      { name: 'Mis Pedidos', href: '/mi-jaldishop/pedidos' },
      { name: 'Mis Pagos', href: '/mi-jaldishop/pagos' },
      { name: 'Mis Estadísticas', href: '/mi-jaldishop/estadisticas' },
      { name: 'Mis Favoritos', href: '/mi-jaldishop/favoritos' },
      { name: 'Mis Reseñas', href: '/mi-jaldishop/resenas' },
      { name: /Notificaciones/i, href: '/mi-jaldishop/notificaciones' },
      { name: 'Mis Direcciones', href: '/mi-jaldishop/direcciones' },
      { name: 'Mi Cuenta / Seguridad', href: '/mi-jaldishop/perfil' },
    ];

    for (const item of expectedLinks) {
      const link = screen.getByRole('link', { name: item.name });
      expect(link).toHaveAttribute('href', item.href);
    }

    expect(screen.getAllByText('4').length).toBeGreaterThanOrEqual(1);
  });

  it('opens and closes accessible mobile drawer', () => {
    render(<CustomerNav />);

    const trigger = screen.getByRole('button', { name: /Abrir menú de Mi JaldiShop/i });
    fireEvent.click(trigger);

    const dialog = screen.getByRole('dialog', { name: /Menú de navegación de Mi JaldiShop/i });
    expect(dialog).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
