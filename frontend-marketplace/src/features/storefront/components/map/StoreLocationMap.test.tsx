import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StoreLocationMap } from './StoreLocationMap';
import { PublicStore } from '../../types/storefront.types';

describe('StoreLocationMap Component', () => {
  const mockStore: PublicStore = {
    id: 'store-123',
    name: 'Panadería Artesanal Don Pepe',
    slug: 'don-pepe',
    address: 'Av. Las Magnolias 450, Miraflores',
    addressReference: 'Frente al parque central',
    latitude: -12.1192,
    longitude: -77.0295,
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
  };

  it('renders heading, address, reference and navigation links', () => {
    render(<StoreLocationMap store={mockStore} />);

    expect(screen.getByText('Dónde encontrarnos')).toBeInTheDocument();
    expect(screen.getByText(/Av\. Las Magnolias 450, Miraflores/)).toBeInTheDocument();
    expect(screen.getByText(/Frente al parque central/)).toBeInTheDocument();
    expect(screen.getByText('Cómo llegar')).toBeInTheDocument();
    expect(screen.getByText('Waze')).toBeInTheDocument();
    expect(screen.getByText('Copiar')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Cómo llegar' })).toHaveAttribute('href', 'https://www.google.com/maps/search/?api=1&query=-12.1192%2C-77.0295');
  });

  it('does not fabricate coordinates when none are provided', () => {
    const storeWithoutCoords = { ...mockStore, latitude: undefined, longitude: undefined };
    render(<StoreLocationMap store={storeWithoutCoords} />);

    expect(screen.getByText('La tienda aún no ha publicado su ubicación en el mapa.')).toBeInTheDocument();
    expect(screen.queryByText('Waze')).not.toBeInTheDocument();
  });
});
