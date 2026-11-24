import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { toast } from 'sonner';
import { FavoriteButton } from './FavoriteButton';
import { customerFavoriteService } from '@/features/customer/services/customerFavoriteService';

const mockPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock('@/features/customer/services/customerFavoriteService', () => ({
  customerFavoriteService: {
    checkFavoriteStatus: vi.fn(),
    addFavorite: vi.fn(),
    removeFavorite: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

describe('FavoriteButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('optimistically toggles favorite state and notifies user when authenticated', async () => {
    vi.mocked(customerFavoriteService.checkFavoriteStatus).mockResolvedValueOnce({
      productId: 'p-1',
      isFavorite: false,
    });
    vi.mocked(customerFavoriteService.addFavorite).mockResolvedValueOnce({
      productId: 'p-1',
      isFavorite: true,
    });

    render(<FavoriteButton productId="p-1" variant="pill" />);

    const btn = await screen.findByRole('button', { name: /Agregar a favoritos/i });
    fireEvent.click(btn);

    expect(btn).toHaveAttribute('aria-pressed', 'true');
    await waitFor(() => {
      expect(customerFavoriteService.addFavorite).toHaveBeenCalledWith('p-1');
      expect(toast.success).toHaveBeenCalledWith(
        'Añadido a tus favoritos.',
        expect.objectContaining({
          id: 'favorite-p-1',
          action: expect.objectContaining({ label: 'Ver favoritos' }),
        })
      );
    });
  });

  it('prompts login via actionable toast without auto-redirecting and blocks spam clicks', async () => {
    vi.mocked(customerFavoriteService.checkFavoriteStatus).mockRejectedValueOnce(
      new Error('No autenticado.')
    );

    render(<FavoriteButton productId="p-2" />);

    await waitFor(() => {
      expect(customerFavoriteService.checkFavoriteStatus).toHaveBeenCalledTimes(1);
    });

    const btn = screen.getByRole('button', { name: /Agregar a favoritos/i });
    fireEvent.click(btn);
    fireEvent.click(btn);
    fireEvent.click(btn);

    expect(customerFavoriteService.addFavorite).not.toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
    expect(toast.info).toHaveBeenCalledTimes(1);
    expect(toast.info).toHaveBeenCalledWith(
      'Inicia sesión para guardar este producto en tus favoritos.',
      expect.objectContaining({
        id: 'favorite-p-2',
        action: expect.objectContaining({ label: 'Iniciar sesión' }),
      })
    );
  });

  it('prevents duplicate API requests during in-flight/cooldown on rapid clicks', async () => {
    vi.mocked(customerFavoriteService.checkFavoriteStatus).mockResolvedValueOnce({
      productId: 'p-3',
      isFavorite: false,
    });
    vi.mocked(customerFavoriteService.addFavorite).mockResolvedValueOnce({
      productId: 'p-3',
      isFavorite: true,
    });

    render(<FavoriteButton productId="p-3" />);
    const btn = await screen.findByRole('button', { name: /Agregar a favoritos/i });

    fireEvent.click(btn);
    fireEvent.click(btn);
    fireEvent.click(btn);

    await waitFor(() => {
      expect(customerFavoriteService.addFavorite).toHaveBeenCalledTimes(1);
    });
  });
});
