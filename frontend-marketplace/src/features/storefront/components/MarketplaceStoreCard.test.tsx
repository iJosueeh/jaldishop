import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MarketplaceStoreCard } from './MarketplaceStoreCard';
import { PublicStore } from '../types/storefront.types';

const sampleStore: PublicStore = {
  id: 's-1',
  name: 'Pastelería Delicia',
  slug: 'pasteleria-delicia',
  description: 'Tortas y bocaditos artesanales',
  address: 'Av. Benavides 456',
  addressReference: 'Ovalo Higuereta',
  whatsappNumber: '+51987654321',
  instagramUrl: 'https://instagram.com/delicia',
  pickupEnabled: true,
  deliveryEnabled: true,
  deliveryFeeAmount: 5,
  status: 'ACTIVE',
};

describe('MarketplaceStoreCard Component', () => {
  it('renders merchant preview details: status, address, whatsapp, pickup and delivery chips', () => {
    render(<MarketplaceStoreCard rawStore={sampleStore} />);

    expect(screen.getByText('Pastelería Delicia')).toBeInTheDocument();
    expect(screen.getByText('Abierto ahora')).toBeInTheDocument();
    expect(screen.getByText(/Av\. Benavides 456/i)).toBeInTheDocument();
    expect(screen.getByText(/\+51987654321/)).toBeInTheDocument();
    expect(screen.getByText('Instagram')).toBeInTheDocument();
    expect(screen.getByText('Pickup')).toBeInTheDocument();
    expect(screen.getByText(/Envío S\/ 5\.00/i)).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('href', '/tienda/pasteleria-delicia');
  });
});
