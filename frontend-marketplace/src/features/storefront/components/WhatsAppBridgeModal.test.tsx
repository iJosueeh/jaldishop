import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WhatsAppBridgeModal } from './WhatsAppBridgeModal';

describe('WhatsAppBridgeModal Component', () => {
  const mockCart = [
    {
      product: {
        id: 'prod-1',
        name: 'Croissant Artesanal',
        description: 'Mantequilla francesa',
        price: 8.5,
        category: 'panaderia',
        imageBg: 'bg-amber-100',
        iconText: '🥐',
      },
      quantity: 2,
    },
  ];

  it('renders WhatsApp message preview with items and slot', () => {
    render(
      <WhatsAppBridgeModal
        isOpen={true}
        onClose={vi.fn()}
        cart={mockCart}
        storeName="Panadería Don Pepe"
        selectedSlot={{
          id: 'slot-1',
          timeRange: '10:00 - 11:00 AM',
          totalCapacity: 10,
          reservedCount: 5,
          status: 'available',
          estimatedDeliveryMinutes: 25,
        }}
      />
    );

    expect(screen.getByText('Panadería Don Pepe')).toBeInTheDocument();
    expect(screen.getByText('10:00 - 11:00 AM')).toBeInTheDocument();
    expect(screen.getByText('Abrir WhatsApp y Enviar Pedido')).toBeInTheDocument();
  });

  it('copies WhatsApp message to clipboard on click', () => {
    render(
      <WhatsAppBridgeModal
        isOpen={true}
        onClose={vi.fn()}
        cart={mockCart}
      />
    );

    const copyBtn = screen.getByText('Copiar texto');
    fireEvent.click(copyBtn);
    expect(screen.getByText('Mensaje copiado')).toBeInTheDocument();
  });
});
