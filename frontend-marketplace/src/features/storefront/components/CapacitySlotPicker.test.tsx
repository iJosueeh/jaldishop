import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CapacitySlotPicker, CapacitySlot } from './CapacitySlotPicker';

const TEST_SLOTS: CapacitySlot[] = [
  {
    id: 'slot-1',
    timeRange: '09:00 - 10:00 AM',
    totalCapacity: 12,
    reservedCount: 12,
    status: 'sold_out',
    estimatedDeliveryMinutes: 25,
  },
  {
    id: 'slot-2',
    timeRange: '10:00 - 11:00 AM',
    totalCapacity: 15,
    reservedCount: 13,
    status: 'few_left',
    estimatedDeliveryMinutes: 30,
  },
  {
    id: 'slot-3',
    timeRange: '11:00 - 12:00 PM',
    totalCapacity: 15,
    reservedCount: 8,
    status: 'available',
    estimatedDeliveryMinutes: 35,
  },
  {
    id: 'slot-4',
    timeRange: '12:00 - 01:00 PM',
    totalCapacity: 10,
    reservedCount: 4,
    status: 'available',
    estimatedDeliveryMinutes: 40,
  },
  {
    id: 'slot-5',
    timeRange: '01:00 - 02:00 PM',
    totalCapacity: 10,
    reservedCount: 9,
    status: 'few_left',
    estimatedDeliveryMinutes: 35,
  },
];

describe('CapacitySlotPicker Component', () => {
  it('renders slot time ranges and capacity indicators', () => {
    render(<CapacitySlotPicker slots={TEST_SLOTS} />);

    expect(screen.getByText('09:00 - 10:00 AM')).toBeInTheDocument();
    expect(screen.getAllByText('10:00 - 11:00 AM').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Agotado')).toBeInTheDocument();
  });

  it('renders fulfillment toggle buttons', () => {
    const handleFulfillmentChange = vi.fn();
    render(
      <CapacitySlotPicker
        slots={TEST_SLOTS}
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
        slots={TEST_SLOTS}
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
        slots={TEST_SLOTS}
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
