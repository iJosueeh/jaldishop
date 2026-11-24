import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StoreTrustCard } from './StoreTrustCard';
import { PublicStore, PublicStoreSettings } from '../../types/storefront.types';

describe('StoreTrustCard', () => {
  const store: PublicStore = {
    id: 's-1',
    name: 'Café Verde',
    slug: 'cafe-verde',
    address: 'Av. Larco 450, Miraflores',
    rating: 4.9,
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    deliveryFeeAmount: 5,
  };

  const settings: PublicStoreSettings = {
    cancellationPolicy: 'Cancelación sin costo hasta 2 horas antes.',
    refundPolicy: 'Reembolso íntegro según INDECOPI.',
    requireCustomerDni: false,
    allowOrderNotes: true,
    onlinePaymentsEnabled: true,
    cashOnDeliveryEnabled: true,
    acceptsPosOnDelivery: true,
    minDeliveryAmount: 20,
    estimatedPrepTimeMinutes: 25,
  };

  it('renders modern Midnight Greenhouse merchant card with logistics, payments, and collapsible policies', () => {
    render(<StoreTrustCard store={store} storeSettings={settings} />);

    expect(screen.getByText('Comercio verificado')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Café Verde' })).toHaveAttribute('href', '/tienda/cafe-verde');
    expect(screen.getByRole('link', { name: /Visitar tienda/i })).toHaveAttribute('href', '/tienda/cafe-verde');
    expect(screen.getByText('Av. Larco 450, Miraflores')).toBeInTheDocument();
    expect(screen.getByText('4.9')).toBeInTheDocument();
    expect(screen.getByText('Delivery y recojo en tienda')).toBeInTheDocument();
    expect(screen.getByText('Aprox. 25 min')).toBeInTheDocument();
    expect(screen.getByText('Pago online')).toBeInTheDocument();
    expect(screen.getByText('Efectivo')).toBeInTheDocument();
    expect(screen.getByText('POS contra entrega')).toBeInTheDocument();
    expect(screen.getByText('Políticas de cancelación y reembolso')).toBeInTheDocument();
  });
});
