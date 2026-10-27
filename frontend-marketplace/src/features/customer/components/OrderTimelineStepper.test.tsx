import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { OrderTimelineStepper } from './OrderTimelineStepper';

describe('OrderTimelineStepper', () => {
  it('renders all delivery steps in correct order', () => {
    render(
      <OrderTimelineStepper
        status="IN_PREPARATION"
        deliveryMode="DELIVERY"
      />
    );

    expect(screen.getByText('Confirmado')).toBeInTheDocument();
    expect(screen.getByText('En preparación')).toBeInTheDocument();
    expect(screen.getByText('Listo')).toBeInTheDocument();
    expect(screen.getByText('En camino')).toBeInTheDocument();
    expect(screen.getByText('Entregado')).toBeInTheDocument();
  });

  it('renders pickup steps when deliveryMode is PICKUP', () => {
    render(
      <OrderTimelineStepper
        status="CONFIRMED"
        deliveryMode="PICKUP"
      />
    );

    expect(screen.getByText('Confirmado')).toBeInTheDocument();
    expect(screen.getByText('En preparación')).toBeInTheDocument();
    expect(screen.getByText('Listo para recojo')).toBeInTheDocument();
    expect(screen.getByText('Recogido')).toBeInTheDocument();
  });

  it('renders cancellation notice when status is CANCELLED', () => {
    render(
      <OrderTimelineStepper
        status="CANCELLED"
        deliveryMode="DELIVERY"
        statusHistory={[
          {
            status: 'CANCELLED',
            reason: 'Cliente solicitó cancelación por cambio de dirección',
            changedAt: '2026-10-25T16:00:00Z',
          },
        ]}
      />
    );

    expect(screen.getByText('Pedido cancelado')).toBeInTheDocument();
    expect(
      screen.getByText('Cliente solicitó cancelación por cambio de dirección')
    ).toBeInTheDocument();
  });
});
