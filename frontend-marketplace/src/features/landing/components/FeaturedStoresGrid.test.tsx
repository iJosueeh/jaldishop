import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FeaturedStoresGrid } from './FeaturedStoresGrid';

describe('FeaturedStoresGrid Component', () => {
  it('renders real empty state without demo stores when initialStores is empty', async () => {
    const ui = await FeaturedStoresGrid({ initialStores: [] });
    render(ui);

    expect(screen.getByText(/Aún no hay comercios destacados disponibles/i)).toBeInTheDocument();
    expect(screen.queryByText(/Tiendas de demostración/i)).not.toBeInTheDocument();
    expect(screen.queryByText('Panadería Don Pepe')).not.toBeInTheDocument();
  });

  it('renders stores slider when real stores exist', async () => {
    const ui = await FeaturedStoresGrid({
      initialStores: [
        {
          id: 'real-1',
          name: 'Bodega El Sol',
          slug: 'bodega-el-sol',
          status: 'ACTIVE',
          deliveryEnabled: true,
          pickupEnabled: true,
        },
      ],
    });
    render(ui);

    expect(screen.getByText('Bodega El Sol')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Explorar una tienda/i })).toHaveAttribute(
      'href',
      '/tienda/bodega-el-sol'
    );
  });
});
