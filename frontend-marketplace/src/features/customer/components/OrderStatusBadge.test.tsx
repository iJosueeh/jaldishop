import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { OrderStatusBadge } from './OrderStatusBadge';

describe('OrderStatusBadge', () => {
  it('renders Confirmado for CONFIRMED status', () => {
    render(<OrderStatusBadge status="CONFIRMED" />);
    expect(screen.getByText('Confirmado')).toBeInTheDocument();
  });

  it('renders En preparación for IN_PREPARATION status', () => {
    render(<OrderStatusBadge status="IN_PREPARATION" />);
    expect(screen.getByText('En preparación')).toBeInTheDocument();
  });

  it('renders Listo para entrega / recojo for READY status', () => {
    render(<OrderStatusBadge status="READY" />);
    expect(screen.getByText('Listo para entrega / recojo')).toBeInTheDocument();
  });

  it('renders En camino for OUT_FOR_DELIVERY status', () => {
    render(<OrderStatusBadge status="OUT_FOR_DELIVERY" />);
    expect(screen.getByText('En camino')).toBeInTheDocument();
  });

  it('renders Entregado for COMPLETED status', () => {
    render(<OrderStatusBadge status="COMPLETED" />);
    expect(screen.getByText('Entregado')).toBeInTheDocument();
  });

  it('renders Cancelado for CANCELLED status', () => {
    render(<OrderStatusBadge status="CANCELLED" />);
    expect(screen.getByText('Cancelado')).toBeInTheDocument();
  });
});
