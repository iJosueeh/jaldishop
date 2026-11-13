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

  it('renders loading state when isLoading is true', () => {
    render(<CapacitySlotPicker slots={[]} isLoading={true} onDateChange={vi.fn()} />);
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(
      screen.getByText('Consultando disponibilidad de horarios en tiempo real...')
    ).toBeInTheDocument();
  });

  it('renders error state and handles onRetry', () => {
    const handleRetry = vi.fn();
    render(
      <CapacitySlotPicker
        slots={[]}
        error="Fallo al cargar horarios."
        onRetry={handleRetry}
        onDateChange={vi.fn()}
      />
    );
    expect(screen.getByText('Fallo al cargar horarios.')).toBeInTheDocument();
    const retryBtn = screen.getByRole('button', { name: 'Reintentar' });
    fireEvent.click(retryBtn);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });

  it('renders date selector and triggers onDateChange', () => {
    const handleDateChange = vi.fn();
    render(
      <CapacitySlotPicker
        slots={TEST_SLOTS}
        selectedDate="2026-11-13"
        onDateChange={handleDateChange}
      />
    );

    const dateInput = screen.getByLabelText(/Fecha:/);
    expect(dateInput).toHaveValue('2026-11-13');
    fireEvent.change(dateInput, { target: { value: '2026-11-14' } });
    expect(handleDateChange).toHaveBeenCalledWith('2026-11-14');
  });

  it('shows warning banner when all slots are sold out', () => {
    const allSoldOutSlots: CapacitySlot[] = [
      {
        id: 'slot-1',
        timeRange: '09:00 - 10:00 AM',
        totalCapacity: 10,
        reservedCount: 10,
        status: 'sold_out',
        estimatedDeliveryMinutes: 30,
      },
    ];

    render(<CapacitySlotPicker slots={allSoldOutSlots} />);
    expect(
      screen.getByText(
        'Todos los horarios para esta fecha están agotados. Selecciona otra fecha para continuar.'
      )
    ).toBeInTheDocument();
  });
});
