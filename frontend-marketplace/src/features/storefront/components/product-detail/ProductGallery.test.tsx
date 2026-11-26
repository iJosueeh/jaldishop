import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductGallery } from './ProductGallery';

const sampleImages = [
  'https://images.unsplash.com/photo-1',
  'https://images.unsplash.com/photo-2',
  'https://images.unsplash.com/photo-3',
];

describe('ProductGallery', () => {
  it('renders unified detail placeholder when no images are provided', () => {
    render(<ProductGallery name="Pan Campesino" category="Panadería" />);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByText('Sin fotografía publicada')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Imagen siguiente/i })).not.toBeInTheDocument();
  });

  it('supports multi-image navigation via buttons, thumbnails, and keyboard arrows', () => {
    render(
      <ProductGallery
        name="Pan Campesino"
        category="Panadería"
        images={sampleImages}
      />
    );

    const prevBtn = screen.getByRole('button', { name: 'Imagen anterior' });
    const nextBtn = screen.getByRole('button', { name: 'Imagen siguiente' });
    expect(prevBtn.className).toContain('min-h-[44px]');
    expect(prevBtn.className).toContain('min-w-[44px]');
    expect(screen.getByText('1 / 3')).toBeInTheDocument();

    fireEvent.click(nextBtn);
    expect(screen.getByText('2 / 3')).toBeInTheDocument();

    const thumb3 = screen.getByRole('button', { name: 'Ver imagen 3 de 3' });
    fireEvent.click(thumb3);
    expect(screen.getByText('3 / 3')).toBeInTheDocument();
    expect(thumb3).toHaveAttribute('aria-current', 'true');

    const carousel = screen.getByRole('region', { name: 'Galería de imágenes de Pan Campesino' });
    fireEvent.keyDown(carousel, { key: 'ArrowLeft' });
    expect(screen.getByText('2 / 3')).toBeInTheDocument();
  });

  it('synchronizes with lightbox modal and restores focus on close', () => {
    render(
      <ProductGallery
        name="Pan Campesino"
        category="Panadería"
        images={sampleImages}
      />
    );

    const expandBtn = screen.getByRole('button', { name: 'Ampliar fotografía de Pan Campesino' });
    fireEvent.click(expandBtn);

    const dialog = screen.getByRole('dialog', { name: 'Vista ampliada de Pan Campesino' });
    expect(dialog).toBeInTheDocument();
    const closeBtn = screen.getByRole('button', { name: 'Cerrar vista ampliada' });
    expect(document.activeElement).toBe(closeBtn);

    fireEvent.keyDown(window, { key: 'ArrowRight' });
    expect(screen.getAllByText('2 / 3').length).toBeGreaterThanOrEqual(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(expandBtn);
  });

  it('dynamically filters broken slides on error and clamps index', () => {
    render(
      <ProductGallery
        name="Pan Campesino"
        category="Panadería"
        images={sampleImages}
      />
    );

    // Move to last slide (index 2: photo-3)
    fireEvent.click(screen.getByRole('button', { name: 'Ver imagen 3 de 3' }));
    expect(screen.getByText('3 / 3')).toBeInTheDocument();

    // Fail photo-3 -> should clamp cleanly to 2 / 2
    const mainImg = screen.getByRole('img', { name: 'Pan Campesino — imagen 3 de 3' });
    fireEvent.error(mainImg);
    expect(screen.getByText('2 / 2')).toBeInTheDocument();

    // Fail remaining two images -> should fall back to placeholder
    fireEvent.error(screen.getByRole('img', { name: 'Pan Campesino — imagen 2 de 2' }));
    fireEvent.error(screen.getByRole('img', { name: 'Pan Campesino' }));
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByText('Sin fotografía publicada')).toBeInTheDocument();
  });
});
