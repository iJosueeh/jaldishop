import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PaymentHubModal } from './PaymentHubModal';

describe('PaymentHubModal Component', () => {
  it('renders payment method options and total amount', () => {
    render(
      <PaymentHubModal
        isOpen={true}
        onClose={vi.fn()}
        onProceedToWhatsApp={vi.fn()}
        total={45.5}
        storeName="Panadería Don Pepe"
      />
    );

    expect(screen.getByText('Pagar y Confirmar Pedido')).toBeInTheDocument();
    expect(screen.getByText('S/ 45.50')).toBeInTheDocument();
    expect(screen.getByText('Yape')).toBeInTheDocument();
    expect(screen.getByText('Plin')).toBeInTheDocument();
    expect(screen.getByText('Banco / CCI')).toBeInTheDocument();
  });

  it('switches payment method tabs', () => {
    render(
      <PaymentHubModal
        isOpen={true}
        onClose={vi.fn()}
        onProceedToWhatsApp={vi.fn()}
        total={30}
      />
    );

    const plinTab = screen.getByText('Plin');
    fireEvent.click(plinTab);
    expect(screen.getByText('Paga con Plin')).toBeInTheDocument();

    const bankTab = screen.getByText('Banco / CCI');
    fireEvent.click(bankTab);
    expect(screen.getByText('Transferencia Bancaria BCP')).toBeInTheDocument();
  });

  it('triggers onProceedToWhatsApp callback with payment details', () => {
    const handleProceed = vi.fn();
    render(
      <PaymentHubModal
        isOpen={true}
        onClose={vi.fn()}
        onProceedToWhatsApp={handleProceed}
        total={25}
      />
    );

    const submitButton = screen.getByText('Confirmar Pedido por WhatsApp');
    fireEvent.click(submitButton);

    expect(handleProceed).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'yape',
        total: 25,
      })
    );
  });
});
