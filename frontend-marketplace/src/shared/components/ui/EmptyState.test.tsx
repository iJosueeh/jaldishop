import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EmptyState } from './EmptyState';

describe('EmptyState Component', () => {
  it('renders title and description correctly', () => {
    render(
      <EmptyState
        title="No se encontraron tiendas"
        description="Prueba con otro término de búsqueda."
      />
    );

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('No se encontraron tiendas')).toBeInTheDocument();
    expect(screen.getByText('Prueba con otro término de búsqueda.')).toBeInTheDocument();
  });

  it('renders action element when provided', () => {
    render(
      <EmptyState
        title="Sin pedidos"
        action={<button>Explorar catálogo</button>}
      />
    );

    expect(screen.getByRole('button', { name: 'Explorar catálogo' })).toBeInTheDocument();
  });

  it('renders with card variant styling', () => {
    const { container } = render(
      <EmptyState
        title="Catálogo vacío"
        variant="card"
      />
    );

    expect(container.firstChild).toHaveClass('rounded-3xl');
  });
});
