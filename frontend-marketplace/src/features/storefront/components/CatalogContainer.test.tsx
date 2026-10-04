import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CatalogContainer } from './CatalogContainer';
import { StorefrontBadges } from './StorefrontBadges';
import { CapacitySlotPicker } from './CapacitySlotPicker';

describe('Storefront without fabricated data', () => {
  it('shows unavailable catalog and schedules without sample products or checkout', () => {
    render(<CatalogContainer storeName="Business Toddy" />);
    expect(screen.getByText('El catálogo todavía no está disponible')).toBeInTheDocument();
    expect(screen.getByText('Horarios de pedido no disponibles')).toBeInTheDocument();
    expect(screen.queryByText('Croissant Artesanal de Mantequilla')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Añadir' })).not.toBeInTheDocument();
    expect(screen.queryByText(/reserva garantizada/)).not.toBeInTheDocument();
    expect(screen.queryByText('09:00 - 10:00 AM')).not.toBeInTheDocument();
  });

  it('distinguishes a known empty catalog from an unavailable catalog', () => {
    render(<CatalogContainer storeName="Business Toddy" products={[]} slots={[]} />);
    expect(screen.getByText('Esta tienda aún no tiene productos publicados')).toBeInTheDocument();
    expect(screen.getByText('No hay horarios disponibles')).toBeInTheDocument();
  });

  it('identifies a catalog request failure and offers retry', () => {
    render(<CatalogContainer storeName="Business Toddy" catalogError capacityError />);
    expect(screen.getByText('No pudimos cargar los productos')).toBeInTheDocument();
    expect(screen.getByText('No pudimos consultar los horarios')).toBeInTheDocument();
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
