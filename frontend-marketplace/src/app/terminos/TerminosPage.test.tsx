import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import TerminosPage from './page';

// Mock IntersectionObserver
class MockIntersectionObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
window.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;

describe('TerminosPage (/terminos)', () => {
  it('renders terms page with main heading, clauses, and footer signoff', () => {
    render(<TerminosPage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /Términos y Condiciones de Uso del Servicio/i,
      })
    ).toBeInTheDocument();

    expect(screen.getByText(/Conforme a Ley N° 29571/i)).toBeInTheDocument();
    expect(screen.getAllByText(/1. Naturaleza de la Plataforma e Intermediación Digital/i)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/JaldiShop Plataformas Digitales S.A.C./i)[0]).toBeInTheDocument();
  });
});
