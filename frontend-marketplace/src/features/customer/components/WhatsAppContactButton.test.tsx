import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { WhatsAppContactButton } from './WhatsAppContactButton';

describe('WhatsAppContactButton', () => {
  it('does not render if phone is null or empty', () => {
    const { container } = render(
      <WhatsAppContactButton
        phone={null}
        orderNumber="ORD-1234"
        storeName="Panadería Don Pepe"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders a WhatsApp link with sanitized phone and encoded message', () => {
    render(
      <WhatsAppContactButton
        phone="987 654 321"
        orderNumber="ORD-1234"
        storeName="Panadería Don Pepe"
      />
    );

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');

    const href = link.getAttribute('href');
    expect(href).toContain('https://wa.me/51987654321');
    expect(href).toContain('ORD-1234');
    expect(href).toContain(encodeURIComponent('Panadería Don Pepe'));
  });
});
