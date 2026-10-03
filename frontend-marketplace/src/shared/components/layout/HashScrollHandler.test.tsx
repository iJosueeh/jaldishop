import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { HashScrollHandler } from './HashScrollHandler';

describe('HashScrollHandler Component', () => {
  it('renders without crashing on mount', () => {
    const { container } = render(<HashScrollHandler />);
    expect(container).toBeEmptyDOMElement();
  });

  it('handles hash scroll when element exists in DOM', () => {
    // Mock target element
    const mockElement = document.createElement('div');
    mockElement.id = 'como-funciona';
    mockElement.scrollIntoView = vi.fn();
    document.body.appendChild(mockElement);

    window.location.hash = '#como-funciona';

    render(<HashScrollHandler />);

    // Cleanup
    document.body.removeChild(mockElement);
    window.location.hash = '';
  });
});
