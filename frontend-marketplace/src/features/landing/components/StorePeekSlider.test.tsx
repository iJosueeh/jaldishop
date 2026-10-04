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
  it('only displays review data supplied by the store', () => {
    render(<StorePeekSlider stores={mockStores} />);
    expect(screen.getByText('4.9')).toBeInTheDocument();
    expect(screen.getByText('(150)')).toBeInTheDocument();
    const fallback = formatStoreData(mockStores[1]);
    expect(screen.queryByText(`(${fallback.reviewsCount})`)).not.toBeInTheDocument();
  });

  it('labels demos and hides their illustrative review data', () => {
    render(<StorePeekSlider stores={mockStores} isDemo />);
    expect(screen.getAllByText('Demostración')).toHaveLength(2);
    expect(screen.queryByText('4.9')).not.toBeInTheDocument();
    expect(screen.queryByText('(150)')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver demostración: Panadería Don Pepe' })).toHaveAttribute('href', '/tienda/panaderia-don-pepe');
  });

  it('renders store cards with proper titles and categories', () => {
    render(<StorePeekSlider stores={mockStores} />);

    expect(screen.getByText('Panadería Don Pepe')).toBeInTheDocument();
    expect(screen.getByText('Dulce Amor')).toBeInTheDocument();
    expect(screen.getByText('Panadería')).toBeInTheDocument();
    expect(screen.getByText('Repostería')).toBeInTheDocument();
  });

  it('correctly maps dynamic values when store fields are missing (formatStoreData)', () => {
    const rawIncompleteStore: Partial<PublicStore> = {
      slug: 'tienda-nueva',
    };

    const formatted = formatStoreData(rawIncompleteStore);

    expect(formatted.name).toBe('Comercio Local');
    expect(formatted.bannerUrl).toContain('unsplash.com');
    // Rating is realistic (between 4.7 and 5.0) and reviewsCount is positive
    expect(formatted.rating).toBeGreaterThanOrEqual(4.7);
    expect(formatted.rating).toBeLessThanOrEqual(5.0);
    expect(formatted.reviewsCount).toBeGreaterThan(0);
    expect(formatted.prepTime).toMatch(/\d+-\d+ min prep/);
    expect(formatted.badge).toBeDefined();
    expect(formatted.deliveryText).toBe('Envío disponible');
  });

  it('generates distinct deterministic metrics for different stores (no static repetition)', () => {
    const storeA = formatStoreData({ slug: 'pan-artesanal-lima' });
    const storeB = formatStoreData({ slug: 'cafe-alturas-cusco' });

    // Review counts should be dynamic and distinct between different stores
    expect(storeA.reviewsCount).not.toBe(storeB.reviewsCount);
  });

  it('infers craft category and emoji from store name and description keywords', () => {
    const bakery = formatStoreData({ name: 'El Horno de Masa Madre' });
    expect(bakery.category).toBe('Panadería & Masa Madre');
    expect(bakery.iconEmoji).toBe('🥐');

    const cafe = formatStoreData({ name: 'Barista Espresso Bar' });
    expect(cafe.category).toBe('Café de Especialidad');
    expect(cafe.iconEmoji).toBe('☕');

    const pastry = formatStoreData({ name: 'Postres & Alfajores de la Abuela' });
    expect(pastry.category).toBe('Pastelería & Postres');
    expect(pastry.iconEmoji).toBe('🍰');

    const pizza = formatStoreData({ name: 'Pizzería & Focaccia Rustica' });
    expect(pizza.category).toBe('Cocinas & Pizzas');
    expect(pizza.iconEmoji).toBe('🍕');
  });

  it('maps free delivery text when deliveryFee is 0 or deliveryFeeAmount is 0', () => {
    const freeDeliveryStore: Partial<PublicStore> = {
      name: 'Café Andino',
      deliveryFee: 0,
      deliveryEnabled: true,
    };
    expect(formatStoreData(freeDeliveryStore).deliveryText).toBe('Envío Gratis');

    const backendDtoStore = {
      name: 'Café Andino DTO',
      deliveryFeeAmount: 0,
      deliveryEnabled: true,
    } as unknown as Partial<PublicStore>;
    expect(formatStoreData(backendDtoStore).deliveryText).toBe('Envío Gratis');

    const feeStore = {
      name: 'Café Andino Fee',
      deliveryFeeAmount: 7.5,
      deliveryEnabled: true,
    } as unknown as Partial<PublicStore>;
    expect(formatStoreData(feeStore).deliveryText).toBe('Envío S/ 7.50');
  });

  it('scrolls with prev and next buttons', () => {
    render(<StorePeekSlider stores={mockStores} />);

    const nextBtn = screen.getByLabelText('Ver siguientes comercios');
    expect(nextBtn).toBeInTheDocument();
    fireEvent.click(nextBtn);

    const prevBtn = screen.getByLabelText('Ver comercios anteriores');
    expect(prevBtn).toBeInTheDocument();
  });

  it('filters stores by category pill', () => {
    render(<StorePeekSlider stores={mockStores} />);

    // Click on Repostería & Postres filter
    const reposteriaBtn = screen.getByText('Repostería & Postres');
    expect(reposteriaBtn).toBeInTheDocument();
    fireEvent.click(reposteriaBtn);

    // Dulce Amor should be visible, Don Pepe should be filtered out
    expect(screen.getByText('Dulce Amor')).toBeInTheDocument();
    expect(screen.queryByText('Panadería Don Pepe')).not.toBeInTheDocument();
  });
});
