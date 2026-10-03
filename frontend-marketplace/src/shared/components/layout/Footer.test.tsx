import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Footer } from './Footer';

describe('Footer Component', () => {
  it('renders brand identity and value props', () => {
    render(<Footer />);

    expect(screen.getByText(/10 min para pagar/i)).toBeInTheDocument();
    expect(screen.getByText(/Tiendas y pedidos activos en tiempo real/i)).toBeInTheDocument();
  });

  it('renders official store_categories and merchant links', () => {
    render(<Footer />);

    expect(screen.getByText('Explorar Tiendas')).toBeInTheDocument();
    expect(screen.getByText('Restaurantes & Cafeterías')).toBeInTheDocument();
    expect(screen.getByText('Moda, Textil & Calzado')).toBeInTheDocument();
    expect(screen.getByText('Hogar, Decoración & Cerámica')).toBeInTheDocument();
    expect(screen.getByText('Floristerías & Arreglos')).toBeInTheDocument();
    expect(screen.getByText('Para Comercios')).toBeInTheDocument();
    expect(screen.getByText('Portal MYPE')).toBeInTheDocument();
  });

  it('renders company and help links without technical documentation', () => {
    render(<Footer />);

    expect(screen.getByText('Compañía')).toBeInTheDocument();

    const aboutLink = screen.getByRole('link', { name: /Sobre Nosotros/i });
    expect(aboutLink).toHaveAttribute('href', '/sobre-nosotros');

    const jobsLink = screen.getByRole('link', { name: /Trabaja con Nosotros/i });
    expect(jobsLink).toHaveAttribute('href', '/trabaja-con-nosotros');

    const helpLink = screen.getByRole('link', { name: /Centro de Ayuda & FAQ/i });
    expect(helpLink).toHaveAttribute('href', '/ayuda');

    // Confirm no GitHub or technical documentation links exist
    expect(screen.queryByText(/GitHub/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Documentación Técnica/i)).not.toBeInTheDocument();
  });

  it('renders legal and accessibility compliance links', () => {
    render(<Footer />);

    const termsLink = screen.getByRole('link', { name: /Términos y Condiciones/i });
    expect(termsLink).toHaveAttribute('href', '/terminos');

    const privacyLink = screen.getByRole('link', { name: /Política de Privacidad/i });
    expect(privacyLink).toHaveAttribute('href', '/privacidad');

    const a11yLink = screen.getByRole('link', { name: /Accesibilidad Web/i });
    expect(a11yLink).toHaveAttribute('href', '/accesibilidad');

    const claimsLink = screen.getByRole('link', { name: /Libro de Reclamaciones/i });
    expect(claimsLink).toHaveAttribute('href', '/libro-de-reclamaciones');

    expect(screen.getByText(/JaldiShop Plataformas Digitales S\.A\.C\./i)).toBeInTheDocument();
  });
});
