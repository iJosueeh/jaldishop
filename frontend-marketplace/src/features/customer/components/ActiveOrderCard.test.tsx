import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ActiveOrderCard } from './ActiveOrderCard';
import { CustomerOrderSummary } from '../types/customer.types';

const mockOrder: CustomerOrderSummary = {
  id: 'order-123',
  orderNumber: 'ORD-20261025-0001',
  storeId: 'store-456',
  storeName: 'Panadería Los Andes',
  storeSlug: 'panaderia-los-andes',
  storeLogoUrl: null,
  status: 'IN_PREPARATION',
  deliveryMode: 'DELIVERY',
  serviceDate: '2026-10-25',
  serviceStartTime: '18:00:00',
  serviceEndTime: '19:00:00',
  totalAmount: 38.5,
  currency: 'PEN',
  totalItems: 3,
  confirmedAt: '2026-10-25T15:30:00Z',
};

describe('ActiveOrderCard', () => {
  it('renders store name, order number, and formatted price', () => {
    render(<ActiveOrderCard order={mockOrder} />);

    expect(screen.getByText('Panadería Los Andes')).toBeInTheDocument();
    expect(screen.getByText('#ORD-20261025-0001')).toBeInTheDocument();
    expect(screen.getByText('S/ 38.50')).toBeInTheDocument();
    expect(screen.getByText('3 artículos')).toBeInTheDocument();
    expect(screen.getByText('En preparación')).toBeInTheDocument();
    expect(screen.getByText('Ver seguimiento y detalle')).toBeInTheDocument();
  });
});
