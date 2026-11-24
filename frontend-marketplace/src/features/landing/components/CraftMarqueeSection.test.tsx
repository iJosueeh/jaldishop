import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import {
  CraftMarqueeSection,
  DEFAULT_ROTATING_PHRASES,
  MarqueeItem,
} from './CraftMarqueeSection';

const realItems: MarqueeItem[] = [
  {
    id: 'prod-1',
    name: 'Torta de Chocolate Clásica',
    category: 'Tortas',
    store: 'Pastelería Delicia',
    storeSlug: 'pasteleria-delicia',
    productSlug: 'torta-de-chocolate-clasica',
    price: 'S/ 65.00',
    badge: 'Delivery disponible',
    badgeVariant: 'jade',
    imageUrl: null,
  },
];

describe('CraftMarqueeSection Component', () => {
  it('renders marquee section with live indicator and link to /tiendas', () => {
    render(<CraftMarqueeSection items={realItems} />);

    expect(screen.getByText('EN VIVO')).toBeInTheDocument();
    expect(screen.getByText(DEFAULT_ROTATING_PHRASES[0])).toBeInTheDocument();
    const allStoresLink = screen.getByRole('link', { name: /Ver todas las tiendas/i });
    expect(allStoresLink).toHaveAttribute('href', '/tiendas');
  });

  it('renders real product items linking to product detail page', () => {
    render(<CraftMarqueeSection items={realItems} />);

    expect(screen.getAllByText('Torta de Chocolate Clásica').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Pastelería Delicia').length).toBeGreaterThan(0);
    const productLinks = screen.getAllByRole('link', { name: /Torta de Chocolate Clásica/i });
    expect(productLinks[0]).toHaveAttribute(
      'href',
      '/tienda/pasteleria-delicia/producto/torta-de-chocolate-clasica'
    );
  });

  it('renders clean empty fallback without fake products when items is empty', () => {
    render(<CraftMarqueeSection items={[]} />);

    expect(screen.queryByText('Croissant Francés de Mantequilla')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ir al catálogo de productos/i })).toHaveAttribute(
      'href',
      '/productos'
    );
  });
});
