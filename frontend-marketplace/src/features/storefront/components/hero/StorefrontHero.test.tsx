import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StorefrontHero } from '../StorefrontHero';
import { StoreBanner } from './StoreBanner';
import { StoreLogo } from './StoreLogo';
import { StoreInfoCard } from './StoreInfoCard';
import { PublicStore } from '../../types/storefront.types';

describe('Storefront Hero Modular Components', () => {
  const mockStore: PublicStore = {
    id: 'store-123',
    name: 'Tiendita Don Pepe',
    slug: 'tiendita-don-pepe',
    description: 'Pastelería y panadería artesanal con masa madre.',
    bannerUrl: 'https://images.unsplash.com/photo-banner.jpg',
    logoUrl: 'https://images.unsplash.com/photo-logo.jpg',
    status: 'ACTIVE',
    category: 'Panadería Artesanal',
    rating: 4.9,
    reviewsCount: 38,
    preparationTimeMinutes: 25,
    deliveryEnabled: true,
    pickupEnabled: true,
    address: 'Av. Primavera 450, Surco',
    phone: '+51987654321',
  };

  it('StoreBanner renders navigation link and action buttons', () => {
    render(<StoreBanner bannerUrl={mockStore.bannerUrl} storeName={mockStore.name} />);

    expect(screen.getByText('Volver al Marketplace')).toBeInTheDocument();
    expect(screen.getByTitle('Compartir tienda')).toBeInTheDocument();
  });

  it('StoreLogo renders image when available and fallback initials when not', () => {
    const { rerender } = render(
      <StoreLogo logoUrl={mockStore.logoUrl} storeName={mockStore.name} />
    );
    expect(screen.getByAltText(`Logo de ${mockStore.name}`)).toBeInTheDocument();

    rerender(<StoreLogo logoUrl={undefined} storeName={mockStore.name} />);
    expect(screen.getByText('TD')).toBeInTheDocument();
  });

  it('StoreInfoCard renders title, status badge and operational metadata', () => {
    render(<StoreInfoCard store={mockStore} />);

    expect(screen.getByText('Tiendita Don Pepe')).toBeInTheDocument();
    expect(screen.getByText('Tienda activa')).toBeInTheDocument();
    expect(screen.getByText('Panadería Artesanal')).toBeInTheDocument();
    expect(screen.getByText('4.9')).toBeInTheDocument();
    expect(screen.getByText('Ver ubicación')).toBeInTheDocument();
    expect(screen.getByText('Av. Primavera 450, Surco')).toBeInTheDocument();
    expect(screen.getByText('Ver productos')).toBeInTheDocument();
  });

  it('StorefrontHero orchestrates StoreBanner, StoreLogo and StoreInfoCard together', () => {
    render(<StorefrontHero store={mockStore} />);

    expect(screen.getByText('Tiendita Don Pepe')).toBeInTheDocument();
    expect(screen.getByText('Volver al Marketplace')).toBeInTheDocument();
    expect(screen.getByText('Tienda activa')).toBeInTheDocument();
  });

  it('shows published modalities without implying opening hours', () => {
    render(<StoreInfoCard store={{ ...mockStore, deliveryEnabled: false, pickupEnabled: true }} />);
    expect(screen.getByText('Recojo en tienda')).toBeInTheDocument();
    expect(screen.queryByText('Entrega a domicilio')).not.toBeInTheDocument();
    expect(screen.queryByText('Abierto hoy')).not.toBeInTheDocument();
  });
});
