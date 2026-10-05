import React from 'react';
import { describe, it, expect } from 'vitest';
import { render as testingRender, screen, fireEvent } from '@testing-library/react';
import { StorefrontCartProvider } from '../cart/StorefrontCartProvider';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CatalogContainer } from './CatalogContainer';
import { StorefrontBadges } from './StorefrontBadges';
import { CapacitySlotPicker } from './CapacitySlotPicker';

function render(ui: React.ReactNode) {
  return testingRender(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{ui}</QueryClientProvider>);
}

describe('Storefront without fabricated data', () => {
  it('filters by category IDs and resets the active tab with the search filters', () => {
    render(<StorefrontCartProvider><CatalogContainer storeName="Toddy" products={[
      { id: 'p1', name: 'Tacos', description: '', price: 12, categoryId: 'c1', category: 'Comida mexicana' },
      { id: 'p2', name: 'Limonada', description: '', price: 5, categoryId: 'c2', category: 'Bebidas' },
    ]} /></StorefrontCartProvider>);
    fireEvent.click(screen.getByRole('button', { name: /Bebidas/ }));
    expect(screen.queryByRole('heading', { name: 'Tacos' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Limonada' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Buscar productos en el catálogo'), { target: { value: 'sin coincidencias' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ver todos los productos' }));
    expect(screen.getByRole('button', { name: /Todos los productos/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('heading', { name: 'Tacos' })).toBeInTheDocument();
  });
  it('shows unavailable catalog and schedules without sample products or checkout', () => {
    render(<StorefrontCartProvider><CatalogContainer storeName="Business Toddy" /></StorefrontCartProvider>);
    expect(screen.getByText('El catálogo todavía no está disponible')).toBeInTheDocument();
    expect(screen.queryByText('Horarios de pedido no disponibles')).not.toBeInTheDocument();
    expect(screen.queryByText('Croissant Artesanal de Mantequilla')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Añadir' })).not.toBeInTheDocument();
    expect(screen.queryByText(/reserva garantizada/)).not.toBeInTheDocument();
    expect(screen.queryByText('09:00 - 10:00 AM')).not.toBeInTheDocument();
  });

  it('distinguishes a known empty catalog from an unavailable catalog', () => {
    render(<StorefrontCartProvider><CatalogContainer storeName="Business Toddy" products={[]} slots={[]} /></StorefrontCartProvider>);
    expect(screen.getByText('Esta tienda aún no tiene productos publicados')).toBeInTheDocument();
    expect(screen.queryByText('No hay horarios disponibles')).not.toBeInTheDocument();
  });

  it('identifies a catalog request failure and offers retry', () => {
    render(<StorefrontCartProvider><CatalogContainer storeName="Business Toddy" catalogError capacityError /></StorefrontCartProvider>);
    expect(screen.getByText('No pudimos cargar los productos')).toBeInTheDocument();
    expect(screen.queryByText('No pudimos consultar los horarios')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver a intentar' })).toBeInTheDocument();
  });

  it('does not invent contact, opening hours, rates or pickup', () => {
    render(<StorefrontBadges store={{ id: '1', name: 'Toddy', slug: 'toddy', status: 'ACTIVE', deliveryEnabled: false, pickupEnabled: false }} />);
    expect(screen.getByText('Horario de atención no publicado')).toBeInTheDocument();
    expect(screen.getByText('Teléfono de contacto no publicado')).toBeInTheDocument();
    expect(screen.getByText('Sin modalidades habilitadas')).toBeInTheDocument();
    expect(screen.queryByText(/08:00/)).not.toBeInTheDocument();
    expect(screen.queryByText(/S\/ 0/)).not.toBeInTheDocument();
  });

  it('does not populate the slot picker when no schedules are supplied', () => {
    render(<CapacitySlotPicker />);
    expect(screen.getByText('No hay horarios disponibles')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
