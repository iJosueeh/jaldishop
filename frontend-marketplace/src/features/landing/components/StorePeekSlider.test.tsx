import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { StorePeekSlider, formatStoreData } from './StorePeekSlider';
import { PublicStore } from '@/features/storefront/types/storefront.types';

const mockStores: PublicStore[] = [
  {
    id: 'store-1',
    name: 'Panadería Don Pepe',
    slug: 'panaderia-don-pepe',
    category: 'Panadería',
    tagline: 'Masa madre tradicional',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    deliveryFee: 5,
    rating: 4.9,
    reviewsCount: 150,
  },
  {
    id: 'store-2',
    name: 'Dulce Amor',
    slug: 'dulce-amor',
    category: 'Repostería',
    status: 'ACTIVE',
    deliveryEnabled: false,
    pickupEnabled: true,
  },
];

describe('StorePeekSlider Component', () => {
  it('only displays review data when supplied by the backend', () => {
    render(<StorePeekSlider stores={mockStores} />);
    expect(screen.getByText('4.9')).toBeInTheDocument();
    expect(screen.getByText('(150)')).toBeInTheDocument();
  });

  it('does not fabricate ratings or reviewsCount when backend omits them', () => {
    const formatted = formatStoreData({ slug: 'tienda-nueva' });
    expect(formatted.rating).toBeNull();
    expect(formatted.reviewsCount).toBeNull();
    expect(formatted.bannerUrl).toBeNull();
    expect(formatted.prepTime).toBe('Turnos programados');
    expect(formatted.deliveryText).toBe('Envío disponible');
  });

  it('renders store cards with proper titles and categories', () => {
    render(<StorePeekSlider stores={mockStores} />);

    expect(screen.getByText('Panadería Don Pepe')).toBeInTheDocument();
    expect(screen.getByText('Dulce Amor')).toBeInTheDocument();
    expect(screen.getByText('Panadería')).toBeInTheDocument();
    expect(screen.getByText('Repostería')).toBeInTheDocument();
  });

  it('filters stores by category pill and shows empty state when no match', () => {
    render(<StorePeekSlider stores={mockStores} />);

    fireEvent.click(screen.getByRole('button', { name: /Café de Especialidad/i }));
    expect(screen.getByText('No hay comercios en esta categoría')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Ver todos los comercios/i }));
    expect(screen.getByText('Panadería Don Pepe')).toBeInTheDocument();
  });
});
