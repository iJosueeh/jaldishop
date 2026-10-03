import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import PrivacidadPage from './page';

// Mock IntersectionObserver
class MockIntersectionObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
window.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;

describe('PrivacidadPage (/privacidad)', () => {
  it('renders privacy page with heading, Ley 29733 badge, ARCO rights, and footer', () => {
    render(<PrivacidadPage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /Política de Privacidad y Tratamiento de Datos Personales/i,
      })
    ).toBeInTheDocument();

    expect(screen.getByText(/Conforme a Ley N° 29733 y D.S. 003-2013-JUS/i)).toBeInTheDocument();
    expect(screen.getAllByText(/5. Ejercicio de Derechos ARCO/i)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/privacidad@jaldishop.com/i)[0]).toBeInTheDocument();
  });
});
