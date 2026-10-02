import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from './Badge';

describe('Badge Component', () => {
  it('renders badge children text', () => {
    render(<Badge>Cupos abiertos</Badge>);
    expect(screen.getByText('Cupos abiertos')).toBeInTheDocument();
  });

  it('applies variant classes correctly', () => {
    const { rerender } = render(<Badge variant="jade">Activo</Badge>);
    expect(screen.getByText('Activo')).toHaveClass('bg-[#f0fdfa]');

    rerender(<Badge variant="terracotta">Hold 10m</Badge>);
    expect(screen.getByText('Hold 10m')).toHaveClass('bg-[#fff7ed]');
  });
});
