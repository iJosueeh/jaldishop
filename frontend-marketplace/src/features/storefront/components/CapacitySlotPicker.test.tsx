import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CapacitySlotPicker, DEFAULT_SLOTS } from './CapacitySlotPicker';

describe('CapacitySlotPicker Component', () => {
  it('renders slot time ranges and capacity indicators', () => {
    render(<CapacitySlotPicker slots={DEFAULT_SLOTS} />);

    expect(screen.getByText('09:00 - 10:00 AM')).toBeInTheDocument();
    expect(screen.getAllByText('10:00 - 11:00 AM').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Agotado')).toBeInTheDocument();
  });

  it('renders fulfillment toggle buttons', () => {
    const handleFulfillmentChange = vi.fn();
    render(
      <CapacitySlotPicker
        fulfillmentType="DELIVERY"
        onFulfillmentTypeChange={handleFulfillmentChange}
      />
    );

    const pickupButton = screen.getByText('Retiro en Tienda');
    fireEvent.click(pickupButton);
    expect(handleFulfillmentChange).toHaveBeenCalledWith('PICKUP');
  });

  it('triggers onSelectSlot when clicking an available slot', () => {
    const handleSelectSlot = vi.fn();
    render(
      <CapacitySlotPicker
        slots={DEFAULT_SLOTS}
        onSelectSlot={handleSelectSlot}
      />
    );

    const availableSlotButton = screen.getByText('11:00 - 12:00 PM');
    fireEvent.click(availableSlotButton);
    expect(handleSelectSlot).toHaveBeenCalled();
  });

  it('disables sold out slots from being clicked', () => {
    const handleSelectSlot = vi.fn();
    render(
      <CapacitySlotPicker
        slots={DEFAULT_SLOTS}
        onSelectSlot={handleSelectSlot}
      />
    );

    const soldOutButton = screen.getByText('09:00 - 10:00 AM').closest('button');
    expect(soldOutButton).toBeDisabled();
    if (soldOutButton) {
      fireEvent.click(soldOutButton);
    }
    expect(handleSelectSlot).not.toHaveBeenCalled();
  });
});
