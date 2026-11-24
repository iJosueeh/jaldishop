import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { StoresCatalogView } from './StoresCatalogView';
import { PublicStore, StoreCategory } from '../types/storefront.types';

const mockReplace = vi.fn();
let mockSearchParams = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  usePathname: () => '/tiendas',
  useSearchParams: () => mockSearchParams,
}));

const categories: StoreCategory[] = [
  { id: 'cat-1', name: 'Panadería', slug: 'panaderia' },
  { id: 'cat-2', name: 'Cafetería', slug: 'cafeteria' },
];

function makeStores(count: number): PublicStore[] {
  return Array.from({ length: count }).map((_, idx) => ({
    id: `s-${idx + 1}`,
    name: `Comercio ${String(idx + 1).padStart(2, '0')}`,
    slug: `comercio-${idx + 1}`,
    category: idx === 0 ? 'Cafetería' : 'Panadería',
    categoryIds: [idx === 0 ? 'cat-2' : 'cat-1'],
    status: 'ACTIVE',
    deliveryEnabled: idx % 2 === 0,
    pickupEnabled: true,
    deliveryFeeAmount: idx === 0 ? 0 : 6,
    whatsappNumber: idx === 0 ? '+51999111222' : undefined,
    address: idx === 0 ? 'Av. Larco 100' : undefined,
  }));
}

beforeEach(() => {
  vi.clearAllMocks();
  mockSearchParams = new URLSearchParams();
});

describe('StoresCatalogView Component', () => {
  it('filters stores by category, free shipping, and whatsapp in the sidebar', () => {
    render(<StoresCatalogView stores={makeStores(3)} categories={categories} />);

    expect(screen.getByText('3 comercios encontrados')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Tarifa máxima de envío/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Envío gratis' }));
    expect(screen.getByText('1 comercio encontrado')).toBeInTheDocument();
    expect(screen.getByText('Comercio 01')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Limpiar todo/i }));
    expect(screen.getByText('3 comercios encontrados')).toBeInTheDocument();
  });

  it('paginates results in batches of 9 stores with windowed page buttons', () => {
    render(<StoresCatalogView stores={makeStores(90)} categories={categories} />);

    expect(screen.getByText('Mostrando 1–9 de 90')).toBeInTheDocument();
    expect(screen.getByText('Comercio 01')).toBeInTheDocument();
    expect(screen.queryByText('Comercio 10')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Página 10' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Página 6' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Página 2' }));
    expect(screen.getByText('Mostrando 10–18 de 90')).toBeInTheDocument();
    expect(screen.getByText('Comercio 10')).toBeInTheDocument();
  });

  it('switches between grid and list layouts and opens mobile filter drawer', () => {
    render(<StoresCatalogView stores={makeStores(2)} categories={categories} />);

    const listBtn = screen.getByRole('button', { name: 'Vista en lista' });
    fireEvent.click(listBtn);
    expect(listBtn).toHaveAttribute('aria-pressed', 'true');

    fireEvent.click(screen.getByRole('button', { name: /Filtros/i }));
    expect(screen.getByRole('dialog', { name: /Filtros de tiendas/i })).toBeInTheDocument();
  });
});
