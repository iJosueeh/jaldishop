import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductsCatalogView } from './ProductsCatalogView';
import { MarketplaceProductItem } from '../types/storefront.types';

const mockProducts: MarketplaceProductItem[] = [
  {
    id: 'p-1',
    name: 'Pan Campesino',
    slug: 'pan-campesino',
    description: 'Masa madre',
    price: 14.5,
    priceCurrency: 'PEN',
    category: 'Panes',
    storeId: 's-1',
    storeName: 'Panadería Don Marco',
    storeSlug: 'panaderia-don-marco',
    deliveryEnabled: true,
    pickupEnabled: true,
  },
  {
    id: 'p-2',
    name: 'Cold Brew',
    slug: 'cold-brew',
    description: 'Café frío',
    price: 12.0,
    priceCurrency: 'PEN',
    category: 'Bebidas',
    storeId: 's-2',
    storeName: 'Café Altura',
    storeSlug: 'cafe-altura',
    deliveryEnabled: false,
    pickupEnabled: true,
  },
];

describe('ProductsCatalogView Component', () => {
  it('renders products and filters by search query and category', () => {
    render(<ProductsCatalogView products={mockProducts} />);

    expect(screen.getByText('2 productos disponibles')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Pan Campesino/i })).toHaveAttribute(
      'href',
      '/tienda/panaderia-don-marco/producto/pan-campesino'
    );

    fireEvent.click(screen.getByRole('button', { name: 'Bebidas' }));
    expect(screen.getByText('1 producto disponible')).toBeInTheDocument();
    expect(screen.getByText('Cold Brew')).toBeInTheDocument();
    expect(screen.queryByText('Pan Campesino')).not.toBeInTheDocument();
  });
});
