import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { ProductsCatalogView } from './ProductsCatalogView';
import { MarketplaceProductItem } from '../types/storefront.types';

const mockReplace = vi.fn();
let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  usePathname: () => '/productos',
  useSearchParams: () => mockSearchParams,
}));

const mockProducts: MarketplaceProductItem[] = [
  {
    id: 'p-1',
    name: 'Pan Campesino',
    slug: 'pan-campesino',
    description: 'Masa madre artesanal',
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
    description: 'Café frío de altura',
    price: 12.0,
    priceCurrency: 'PEN',
    category: 'Bebidas',
    storeId: 's-2',
    storeName: 'Café Altura',
    storeSlug: 'cafe-altura',
    deliveryEnabled: false,
    pickupEnabled: true,
  },
  {
    id: 'p-3',
    name: 'Torta de Chocolate',
    slug: 'torta-chocolate',
    description: 'Cacao 70%',
    price: 45.0,
    priceCurrency: 'PEN',
    category: 'Pasteles',
    storeId: 's-1',
    storeName: 'Panadería Don Marco',
    storeSlug: 'panaderia-don-marco',
    deliveryEnabled: true,
    pickupEnabled: false,
  },
];

function makeManyProducts(count: number): MarketplaceProductItem[] {
  return Array.from({ length: count }).map((_, idx) => ({
    id: `prod-${idx + 1}`,
    name: `Producto ${String(idx + 1).padStart(2, '0')}`,
    slug: `producto-${idx + 1}`,
    description: `Descripción ${idx + 1}`,
    price: 10 + idx,
    priceCurrency: 'PEN',
    category: idx % 2 === 0 ? 'Panes' : 'Bebidas',
    storeId: 's-1',
    storeName: 'Panadería Don Marco',
    storeSlug: 'panaderia-don-marco',
    deliveryEnabled: true,
    pickupEnabled: true,
  }));
}

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
});

describe('ProductsCatalogView Component', () => {
  it('renders products, links correctly, and filters by category + syncs URL', () => {
    render(<ProductsCatalogView products={mockProducts} />);

    expect(screen.getByText('3 productos disponibles')).toBeInTheDocument();
    expect(screen.getByText('Mostrando 1–3 de 3')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Pan Campesino/i })).toHaveAttribute(
      'href',
      '/tienda/panaderia-don-marco/producto/pan-campesino'
    );

    fireEvent.click(screen.getByRole('button', { name: 'Bebidas' }));
    expect(screen.getByText('1 producto disponible')).toBeInTheDocument();
    expect(screen.getByText('Cold Brew')).toBeInTheDocument();
    expect(screen.queryByText('Pan Campesino')).not.toBeInTheDocument();
    expect(mockReplace).toHaveBeenCalledWith('/productos?categoria=Bebidas', {
      scroll: false,
    });
  });

  it('hydrates valid URL searchParams and ignores invalid/unknown values', () => {
    mockSearchParams = new URLSearchParams(
      'q=Cold&entrega=invalido&precioMax=-20&orden=desconocido&page=0'
    );

    render(<ProductsCatalogView products={mockProducts} />);

    expect(screen.getByText('1 producto disponible')).toBeInTheDocument();
    expect(screen.getByText('Cold Brew')).toBeInTheDocument();
    expect(screen.getByLabelText('Ordenar productos')).toHaveValue('relevancia');
  });

  it('filters by delivery mode, store, max price, removes chips, and resets', () => {
    render(<ProductsCatalogView products={mockProducts} />);

    fireEvent.click(screen.getByRole('button', { name: 'Delivery' }));
    expect(screen.getByText('2 productos disponibles')).toBeInTheDocument();
    expect(screen.queryByText('Cold Brew')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Tienda/i }));
    fireEvent.change(screen.getByLabelText('Filtrar por tienda'), {
      target: { value: 'panaderia-don-marco' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Precio máximo/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Hasta S/ 15' }));
    expect(screen.getByText('1 producto disponible')).toBeInTheDocument();
    expect(screen.getByText('Pan Campesino')).toBeInTheDocument();

    // Remove price chip individually
    fireEvent.click(screen.getByRole('button', { name: /Quitar filtro Precio/i }));
    expect(screen.getByText('2 productos disponibles')).toBeInTheDocument();

    // Clear all
    fireEvent.click(screen.getAllByRole('button', { name: 'Limpiar todo' })[0]);
    expect(screen.getByText('3 productos disponibles')).toBeInTheDocument();
    expect(mockReplace).toHaveBeenLastCalledWith('/productos', { scroll: false });
  });

  it('sorts products by price and name, and switches grid/list layout', () => {
    render(<ProductsCatalogView products={mockProducts} />);

    const sortSelect = screen.getByLabelText('Ordenar productos');
    fireEvent.change(sortSelect, { target: { value: 'price_asc' } });
    expect(mockReplace).toHaveBeenLastCalledWith('/productos?orden=price_asc', {
      scroll: false,
    });

    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveTextContent('Cold Brew');
    expect(links[2]).toHaveTextContent('Torta de Chocolate');

    const listBtn = screen.getByRole('button', { name: 'Vista en lista' });
    fireEvent.click(listBtn);
    expect(listBtn).toHaveAttribute('aria-pressed', 'true');
  });

  it('paginates in batches of 12 and resets page to 1 when a filter changes', () => {
    render(<ProductsCatalogView products={makeManyProducts(26)} />);

    expect(screen.getByText('Mostrando 1–12 de 26')).toBeInTheDocument();
    expect(screen.getByText('Producto 01')).toBeInTheDocument();
    expect(screen.queryByText('Producto 13')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Página 2' }));
    expect(screen.getByText('Mostrando 13–24 de 26')).toBeInTheDocument();
    expect(mockReplace).toHaveBeenLastCalledWith('/productos?page=2', {
      scroll: false,
    });

    fireEvent.click(screen.getByRole('button', { name: 'Panes' }));
    expect(mockReplace).toHaveBeenLastCalledWith('/productos?categoria=Panes', {
      scroll: false,
    });
  });

  it('opens mobile filter drawer, closes on Escape, and restores focus to trigger', async () => {
    render(<ProductsCatalogView products={mockProducts} />);

    const trigger = screen.getByRole('button', { name: /Filtros/i });
    fireEvent.click(trigger);

    expect(screen.getByRole('dialog', { name: /Filtros de productos/i })).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => {
      expect(
        screen.queryByRole('dialog', { name: /Filtros de productos/i })
      ).not.toBeInTheDocument();
    });
    expect(document.activeElement).toBe(trigger);
  });

  it('shows actionable empty state with blocking filter details and one-click reset', () => {
    render(<ProductsCatalogView products={mockProducts} />);

    fireEvent.change(screen.getByLabelText('Buscar producto o tienda'), {
      target: { value: 'inexistente' },
    });

    expect(screen.getByText('No encontramos productos con esos filtros')).toBeInTheDocument();
    expect(
      screen.getByText(/Ningún producto coincide con Búsqueda: "inexistente"/i)
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(screen.getByText('3 productos disponibles')).toBeInTheDocument();
  });

  it('handles missing price, no active delivery/pickup mode, and broken image fallback', () => {
    const edgeProducts: MarketplaceProductItem[] = [
      {
        ...mockProducts[0],
        price: Number.NaN,
        deliveryEnabled: false,
        pickupEnabled: false,
        imageUrl: 'https://broken.example.com/pan.jpg',
        images: ['https://broken.example.com/pan.jpg', 'https://broken.example.com/pan-2.jpg'],
      },
    ];

    render(<ProductsCatalogView products={edgeProducts} />);
    const card = screen.getByRole('link', { name: /Pan Campesino/i });
    expect(within(card).getByText('Consultar')).toBeInTheDocument();
    expect(within(card).queryByText('Delivery')).not.toBeInTheDocument();
    expect(within(card).queryByText('Retiro')).not.toBeInTheDocument();

    const img = screen.getByRole('img', { name: 'Pan Campesino' });
    fireEvent.error(img);
    expect(screen.queryByRole('img', { name: 'Pan Campesino' })).not.toBeInTheDocument();
  });
});
