import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CheckoutOrderSummary } from './CheckoutOrderSummary';
import { CartItem } from '@/features/storefront/types/storefront.types';

const sampleItems: CartItem[] = [
  {
    quantity: 2,
    product: {
      id: 'prod-1',
      variantId: 'var-1',
      name: 'Torta de Chocolate',
      slug: 'torta-chocolate',
      price: 50,
      priceCurrency: 'PEN',
      available: true,
    } as CartItem['product'],
  },
];

describe('CheckoutOrderSummary promo integration', () => {
  it('computes discounted service fee and total before server reservation', () => {
    render(
      <CheckoutOrderSummary
        storeName="Dulce Deleite"
        items={sampleItems}
        currency="PEN"
        subtotal={100}
        deliveryFee={10}
        serviceFeeRate={5}
        fulfillmentType="DELIVERY"
        checkoutResponse={null}
        discountCode="VERANO20"
        onDiscountCodeChange={vi.fn()}
        appliedPromo={{ code: 'VERANO20', discountAmount: 20 }}
        onClearDiscount={vi.fn()}
      />
    );

    expect(screen.getByText('Descuento (VERANO20)')).toBeInTheDocument();
    expect(screen.getByText('-S/ 20.00')).toBeInTheDocument();
    // 5% of discounted subtotal (100 - 20 = 80) = S/ 4.00; total = 80 + 10 + 4 = 94.00
    expect(screen.getByText('S/ 4.00')).toBeInTheDocument();
    expect(screen.getByText('S/ 94.00')).toBeInTheDocument();
    // Input is hidden while a promo chip is active (no stacking)
    expect(screen.queryByLabelText('Código de descuento')).not.toBeInTheDocument();
  });

  it('calls onClearDiscount when Quitar is clicked and triggers onApplyDiscount on Enter', () => {
    const onClear = vi.fn();
    const onApply = vi.fn();

    const { rerender } = render(
      <CheckoutOrderSummary
        storeName="Dulce Deleite"
        items={sampleItems}
        currency="PEN"
        subtotal={100}
        deliveryFee={0}
        fulfillmentType="PICKUP"
        checkoutResponse={null}
        discountCode="VERANO20"
        onDiscountCodeChange={vi.fn()}
        appliedPromo={{ code: 'VERANO20', discountAmount: 20 }}
        onClearDiscount={onClear}
        onApplyDiscount={onApply}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Quitar' }));
    expect(onClear).toHaveBeenCalledTimes(1);

    rerender(
      <CheckoutOrderSummary
        storeName="Dulce Deleite"
        items={sampleItems}
        currency="PEN"
        subtotal={100}
        deliveryFee={0}
        fulfillmentType="PICKUP"
        checkoutResponse={null}
        discountCode="OTRO10"
        onDiscountCodeChange={vi.fn()}
        appliedPromo={null}
        promoError="Este código promocional ha expirado."
        onClearDiscount={onClear}
        onApplyDiscount={onApply}
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Este código promocional ha expirado.');
    const input = screen.getByLabelText('Código de descuento');
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    expect(onApply).toHaveBeenCalledTimes(1);
  });

  it('normaliza pricing.taxRate = 18 del servidor como IGV incluido (18%) y nunca 1800%', () => {
    render(
      <CheckoutOrderSummary
        storeName="Gourmet SAC"
        items={sampleItems}
        currency="PEN"
        subtotal={100}
        deliveryFee={0}
        taxRate={18}
        fulfillmentType="PICKUP"
        checkoutResponse={{
          reservationId: 'res-1',
          reservationExpiresAt: new Date(Date.now() + 600_000).toISOString(),
          storeId: 'store-1',
          storeName: 'Gourmet SAC',
          fulfillmentType: 'PICKUP',
          serviceDate: '2026-11-25',
          customerInfo: { customerName: 'Ana', customerPhone: '999888777', customerEmail: 'ana@test.com' },
          items: [],
          pricing: {
            currency: 'PEN',
            productsSubtotalAmount: 100,
            discountAmount: 0,
            deliveryFeeAmount: 0,
            serviceFeeAmount: 0,
            taxRate: 18,
            includedTaxAmount: 15.25,
            totalAmount: 100,
          },
        }}
      />
    );

    expect(screen.getByText('IGV incluido (18%)')).toBeInTheDocument();
    expect(screen.queryByText(/1800%/)).not.toBeInTheDocument();
    expect(screen.getByText('S/ 15.25')).toBeInTheDocument();
  });

  it('muestra IGV (Exonerado 0%) para tiendas Nuevo RUS con taxRate = 0', () => {
    render(
      <CheckoutOrderSummary
        storeName="Bodega Luz"
        items={sampleItems}
        currency="PEN"
        subtotal={100}
        deliveryFee={0}
        taxRate={0}
        fulfillmentType="PICKUP"
        checkoutResponse={null}
      />
    );

    expect(screen.getByText('IGV (Exonerado 0%)')).toBeInTheDocument();
    expect(screen.queryByText(/IGV incluido/)).not.toBeInTheDocument();
  });
});
