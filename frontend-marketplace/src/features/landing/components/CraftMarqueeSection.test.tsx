import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CraftMarqueeSection, DEFAULT_ROTATING_PHRASES } from './CraftMarqueeSection';

describe('CraftMarqueeSection Component', () => {
  it('renders marquee section with live indicator and initial rotating phrase', () => {
    render(<CraftMarqueeSection />);

    expect(screen.getByText('EN VIVO')).toBeInTheDocument();
    expect(screen.getByText(DEFAULT_ROTATING_PHRASES[0])).toBeInTheDocument();
    expect(screen.getByText('Ver todas las tiendas')).toBeInTheDocument();
  });

  it('renders fixed general title when passed as prop', () => {
    const customTitle = 'Capacidad y pedidos en tiempo real';
    render(<CraftMarqueeSection title={customTitle} />);

    expect(screen.getByText(customTitle)).toBeInTheDocument();
  });

  it('renders diverse multi-category items (bakery, flowers, pottery, coffee)', () => {
    render(<CraftMarqueeSection />);

    // Check presence of first items from different categories
    expect(screen.getAllByText('Croissant Francés de Mantequilla').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Ramo Silvestre de Temporada').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Taza Cerámica Torneada a Mano').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Cold Brew Embotellado de Altura').length).toBeGreaterThan(0);
  });
});
