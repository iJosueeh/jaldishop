import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductVisual } from './ProductVisual';

const product = {
  name: 'Pan del negocio',
  category: 'Panadería',
  imageBg: 'bg-white',
  imageUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff',
};

describe('ProductVisual', () => {
  it('identifies reference photos in demonstrations', () => {
    render(<ProductVisual {...product} isDemo />);
    expect(screen.getByRole('img', { name: 'Imagen de referencia: Pan del negocio' })).toBeInTheDocument();
    expect(screen.getByText('Imagen de referencia')).toBeInTheDocument();
  });

  it('renders the actual product photo without a demo label', () => {
    render(<ProductVisual {...product} />);
    expect(screen.getByRole('img', { name: product.name })).toBeInTheDocument();
    expect(screen.queryByText('Imagen de referencia')).not.toBeInTheDocument();
  });

  it('shows discrete photo count badge when imageCount > 1', () => {
    render(<ProductVisual {...product} imageCount={4} />);
    expect(screen.getByLabelText('4 fotografías')).toHaveTextContent('4');
  });

  it('falls back to unified decorative placeholder when an image fails without exposing alt text', () => {
    const { container } = render(<ProductVisual {...product} imageCount={3} isDemo />);
    fireEvent.error(screen.getByRole('img'));
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.queryByText('Imagen de referencia')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('3 fotografías')).not.toBeInTheDocument();
    expect(screen.queryByText('Sin fotografía del producto')).not.toBeInTheDocument();
    expect(container.querySelector('[aria-hidden="true"]')).toBeInTheDocument();
  });

  it('rejects malformed image URLs and renders placeholder immediately', () => {
    const { container } = render(<ProductVisual {...product} imageUrl="not-a-valid-url" />);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container.querySelector('[aria-hidden="true"]')).toBeInTheDocument();
  });
});
