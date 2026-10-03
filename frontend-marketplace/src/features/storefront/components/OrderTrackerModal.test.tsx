import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { OrderTrackerModal } from './OrderTrackerModal';
import { CapacitySlot } from './CapacitySlotPicker';
import { CartItem } from './CatalogContainer';

const mockSlot: CapacitySlot = {
  id: 'slot-2',
  timeRange: '10:00 - 11:00 AM',
  status: 'AVAILABLE',
  currentOrders: 6,
  maxCapacity: 12,
  isPopular: true,
};

const mockCart: CartItem[] = [
  {
    product: {
      id: 'prod-01',
      name: 'Croissant Artesanal',
      description: 'Hojaldre 100% mantequilla',
      price: 8.5,
      category: 'panaderia',
      imageBg: 'bg-amber-50',
      iconText: '🥐',
    },
    quantity: 2,
    notes: 'Bien doradito',
  },
];

describe('OrderTrackerModal Component', () => {
  it('renders 4-step live order tracker when open', () => {
    render(
      <OrderTrackerModal
        isOpen={true}
        onClose={vi.fn()}
        orderId="JALDI-1048"
        storeName="Panadería Don Pepe"
        cart={mockCart}
        selectedSlot={mockSlot}
        fulfillmentType="DELIVERY"
        deliveryFee={5}
        paymentMethod="yape"
      />
    );

    // Verify modal header
    expect(screen.getByText('Panadería Don Pepe')).toBeInTheDocument();
    expect(screen.getByText(/Pedido #JALDI-1048/i)).toBeInTheDocument();

    // Verify 4 steps are rendered
    expect(screen.getAllByText('Pago Confirmado').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('En Preparación')).toBeInTheDocument();
    expect(screen.getByText('En Camino')).toBeInTheDocument();
    expect(screen.getByText('Entregado')).toBeInTheDocument();
  });

  it('switches to Boarding Pass digital receipt and displays QR & items', () => {
    render(
      <OrderTrackerModal
        isOpen={true}
        onClose={vi.fn()}
        orderId="JALDI-1048"
        storeName="Panadería Don Pepe"
        cart={mockCart}
        selectedSlot={mockSlot}
        fulfillmentType="DELIVERY"
        deliveryFee={5}
        paymentMethod="yape"
      />
    );

    // Switch to Receipt Tab
    const receiptTabButton = screen.getByText(/Recibo Boarding Pass/i);
    fireEvent.click(receiptTabButton);

    // Verify Boarding Pass elements
    expect(screen.getByText(/JaldiShop Boarding Pass/i)).toBeInTheDocument();
    expect(screen.getByText(/TICKET #JALDI-1048/i)).toBeInTheDocument();
    expect(screen.getByText('10:00 - 11:00 AM')).toBeInTheDocument();
    expect(screen.getByText(/2x Croissant Artesanal/i)).toBeInTheDocument();
    expect(screen.getByText(/Código QR de Verificación/i)).toBeInTheDocument();
  });

  it('allows clicking simulator step buttons to change order step', () => {
    render(
      <OrderTrackerModal
        isOpen={true}
        onClose={vi.fn()}
        orderId="JALDI-1048"
        storeName="Panadería Don Pepe"
        cart={mockCart}
        selectedSlot={mockSlot}
        fulfillmentType="DELIVERY"
        deliveryFee={5}
        paymentMethod="transferencia"
      />
    );

    // Click Paso 2 (En Preparación)
    const step2Btn = screen.getByText('Paso 2');
    fireEvent.click(step2Btn);

    // Should indicate Step 2 of 4
    expect(screen.getByText('Paso 2 de 4')).toBeInTheDocument();
  });
});
