import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PaymentHubModal } from './PaymentHubModal';

describe('PaymentHubModal Component', () => {
  describe('Mercado Pago Real Flow', () => {
    it('renders official Mercado Pago checkout and hides simulated collection details', () => {
      render(
        <PaymentHubModal
          isOpen={true}
          onClose={vi.fn()}
          onPayWithMercadoPago={vi.fn()}
          onProceedToWhatsApp={vi.fn()}
          total={45.5}
          storeName="Panadería Don Pepe"
        />
      );

      // Official Gateway Presentation
      expect(screen.getByText('Pagar y Confirmar Pedido')).toBeInTheDocument();
      expect(screen.getByText('Pago seguro procesado por Mercado Pago')).toBeInTheDocument();
      expect(screen.getByText('Total a Pagar')).toBeInTheDocument();
      expect(screen.getByText('S/ 45.50')).toBeInTheDocument();
      expect(screen.getByText('Mercado Pago Checkout')).toBeInTheDocument();
      expect(screen.getByText('Pasarela oficial y segura')).toBeInTheDocument();
      expect(screen.getByText('Tarjetas Débito / Crédito')).toBeInTheDocument();
      expect(screen.getByText('Pagar con Mercado Pago')).toBeInTheDocument();

      // Regression: Simulated/unverified elements MUST be hidden
      expect(screen.queryByText('Yape')).not.toBeInTheDocument();
      expect(screen.queryByText('Plin')).not.toBeInTheDocument();
      expect(screen.queryByText('Banco / CCI')).not.toBeInTheDocument();
      expect(screen.queryByText('987 654 321')).not.toBeInTheDocument();
      expect(screen.queryByText('194-98765432-0-12')).not.toBeInTheDocument();
      expect(screen.queryByText('Simular Subida de Comprobante')).not.toBeInTheDocument();
      expect(screen.queryByText('Arrastra tu captura de pantalla o foto del comprobante aquí')).not.toBeInTheDocument();
    });

    it('triggers onPayWithMercadoPago callback when CTA is clicked', () => {
      const handlePayWithMP = vi.fn();
      render(
        <PaymentHubModal
          isOpen={true}
          onClose={vi.fn()}
          onPayWithMercadoPago={handlePayWithMP}
          total={70}
        />
      );

      const payButton = screen.getByText('Pagar con Mercado Pago');
      fireEvent.click(payButton);
      expect(handlePayWithMP).toHaveBeenCalledTimes(1);
    });

    it('disables pay button when isPaying is true', () => {
      const handlePayWithMP = vi.fn();
      render(
        <PaymentHubModal
          isOpen={true}
          onClose={vi.fn()}
          onPayWithMercadoPago={handlePayWithMP}
          isPaying={true}
          total={70}
        />
      );

      const button = screen.getByRole('button', { name: /pagar con mercado pago/i });
      expect(button).toBeDisabled();
      fireEvent.click(button);
      expect(handlePayWithMP).not.toHaveBeenCalled();
    });

    it('renders auxiliary WhatsApp coordination option under gateway button', () => {
      const handleWhatsApp = vi.fn();
      render(
        <PaymentHubModal
          isOpen={true}
          onClose={vi.fn()}
          onPayWithMercadoPago={vi.fn()}
          onProceedToWhatsApp={handleWhatsApp}
          total={60}
        />
      );

      const waButton = screen.getByText('¿Problemas con la pasarela? Solicitar coordinación por WhatsApp');
      expect(waButton).toBeInTheDocument();
      fireEvent.click(waButton);

      expect(handleWhatsApp).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'yape',
          receiptFile: null,
          total: 60,
        })
      );
    });
  });

  describe('Direct Coordination Mode (Without Mercado Pago)', () => {
    it('renders coordination request and does not claim purchase is confirmed', () => {
      const handleProceed = vi.fn();
      render(
        <PaymentHubModal
          isOpen={true}
          onClose={vi.fn()}
          onProceedToWhatsApp={handleProceed}
          total={35}
          storeName="Bodega Rosita"
        />
      );

      expect(screen.getByText('Coordinar Pedido')).toBeInTheDocument();
      expect(screen.getByText('Total Estimado')).toBeInTheDocument();
      expect(screen.getByText('S/ 35.00')).toBeInTheDocument();
      expect(screen.getByText('Coordinación directa')).toBeInTheDocument();

      // Regression: Must never claim purchase is confirmed
      expect(screen.queryByText('Confirmar Pedido por WhatsApp')).not.toBeInTheDocument();

      const coordButton = screen.getByText('Coordinar Pedido por WhatsApp');
      expect(coordButton).toBeInTheDocument();
      fireEvent.click(coordButton);

      expect(handleProceed).toHaveBeenCalledWith(
        expect.objectContaining({
          total: 35,
        })
      );
    });
  });
});
