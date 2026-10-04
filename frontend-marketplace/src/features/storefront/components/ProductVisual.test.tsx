import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductVisual } from './ProductVisual';

const product = { name: 'Pan del negocio', iconText: '🥖', imageBg: 'bg-white', imageUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff' };

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

  it('falls back gracefully when an image fails', () => {
    render(<ProductVisual {...product} isDemo />);
    fireEvent.error(screen.getByRole('img'));
    expect(screen.getByText('Sin fotografía del producto')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.queryByText('Imagen de referencia')).not.toBeInTheDocument();
  });
});
