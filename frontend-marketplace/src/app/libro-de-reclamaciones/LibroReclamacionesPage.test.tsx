import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import LibroReclamacionesPage from './page';

describe('LibroReclamacionesPage (/libro-de-reclamaciones)', () => {
  it('renders Libro de Reclamaciones Virtual page with official D.S. 011-2011-PCM compliance header and form', () => {
    render(<LibroReclamacionesPage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /Libro de Reclamaciones Virtual/i,
      })
    ).toBeInTheDocument();

    expect(screen.getByText(/Conforme a D.S. 011-2011-PCM/i)).toBeInTheDocument();
    expect(screen.getByText(/Hoja de Reclamación Virtual Oficial/i)).toBeInTheDocument();
    expect(screen.getByText(/1. Identificación del Consumidor Reclamante/i)).toBeInTheDocument();
  });
});
