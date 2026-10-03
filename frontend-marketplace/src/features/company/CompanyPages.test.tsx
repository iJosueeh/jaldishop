import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SobreNosotrosPage from '@/app/sobre-nosotros/page';
import TrabajaConNosotrosPage from '@/app/trabaja-con-nosotros/page';
import AccesibilidadPage from '@/app/accesibilidad/page';
import AyudaPage from '@/app/ayuda/page';

describe('Company Institutional Pages', () => {
  it('renders SobreNosotrosPage with mission and principles', () => {
    render(<SobreNosotrosPage />);

    expect(screen.getByRole('heading', { level: 1, name: /Sobre JaldiShop/i })).toBeInTheDocument();
    expect(screen.getByText(/Cero Sobreventa Garantizada/i)).toBeInTheDocument();
    expect(screen.getByText(/10 Minutos para Pagar con Calma/i)).toBeInTheDocument();
    expect(screen.getAllByText(/JaldiShop Plataformas Digitales S\.A\.C\./i).length).toBeGreaterThan(0);
  });

  it('renders TrabajaConNosotrosPage with culture and job areas', () => {
    render(<TrabajaConNosotrosPage />);

    expect(screen.getByRole('heading', { level: 1, name: /Trabaja con Nosotros/i })).toBeInTheDocument();
    expect(screen.getByText(/Rigor y Buenas Prácticas/i)).toBeInTheDocument();
    expect(screen.getByText(/talento@jaldishop\.com/i)).toBeInTheDocument();
  });

  it('renders AccesibilidadPage with WCAG 2.1 AA standards', () => {
    render(<AccesibilidadPage />);

    expect(screen.getByRole('heading', { level: 1, name: /Declaración de Accesibilidad/i })).toBeInTheDocument();
    expect(screen.getByText(/Contraste y Jerarquía Visual/i)).toBeInTheDocument();
    expect(screen.getByText(/Navegación Total por Teclado/i)).toBeInTheDocument();
    expect(screen.getByText(/accesibilidad@jaldishop\.com/i)).toBeInTheDocument();
  });

  it('renders AyudaPage with buyer and merchant FAQ', () => {
    render(<AyudaPage />);

    expect(screen.getByRole('heading', { level: 1, name: /Centro de Ayuda & FAQ/i })).toBeInTheDocument();
    expect(screen.getByText(/¿Qué es la reserva temporal de 10 minutos\?/i)).toBeInTheDocument();
    expect(screen.getByText(/¿Cómo evita JaldiShop la sobreventa en mi negocio\?/i)).toBeInTheDocument();
    expect(screen.getByText(/soporte@jaldishop\.com/i)).toBeInTheDocument();
  });
});
