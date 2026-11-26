import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { JaldiToaster } from './JaldiToaster';

const toasterPropsSpy = vi.fn();

vi.mock('sonner', () => ({
  Toaster: (props: Record<string, unknown>) => {
    toasterPropsSpy(props);
    return <div data-testid="mock-sonner-toaster" data-position={String(props.position)} />;
  },
}));

describe('JaldiToaster', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('configures visibleToasts=3, closeButton, offsets and desktop top-right position', () => {
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));

    render(<JaldiToaster />);

    expect(screen.getByTestId('mock-sonner-toaster')).toHaveAttribute('data-position', 'top-right');
    expect(toasterPropsSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        position: 'top-right',
        visibleToasts: 3,
        closeButton: true,
        offset: { top: 80, right: 16 },
        mobileOffset: { top: 76, left: 12, right: 12 },
      })
    );
  });

  it('switches to top-center on mobile and propagates prefers-reduced-motion', () => {
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: true,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));

    render(<JaldiToaster />);

    expect(screen.getByTestId('mock-sonner-toaster')).toHaveAttribute('data-position', 'top-center');
    expect(toasterPropsSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        position: 'top-center',
        'data-reduced-motion': 'true',
      })
    );
  });
});
