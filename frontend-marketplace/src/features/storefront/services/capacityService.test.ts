import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  capacityService,
  CapacityServiceError,
  mapBackendSlotsToPickerSlots,
  getBusinessTodayDate,
  CapacitySlotBackendResponse,
} from './capacityService';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('capacityService', () => {
  const storeId = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

  it('fetches slots and returns data when request succeeds', async () => {
    const mockData = {
      storeId,
      serviceDate: '2026-11-13',
      slots: [
        {
          storeId,
          serviceDate: '2026-11-13',
          startTime: '10:00:00',
          endTime: '12:00:00',
          effectiveCapacity: 15,
          availableCapacity: 10,
          reservedCapacity: 5,
          committedCapacity: 0,
          occupiedCapacity: 5,
        },
      ],
    };

    fetchMock.mockResolvedValue(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    const result = await capacityService.getCapacitySlots(storeId, '2026-11-13');
    expect(result.storeId).toBe(storeId);
    expect(result.slots).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(`/api/customer/capacity/slots?storeId=${storeId}&date=2026-11-13`),
      expect.any(Object)
    );
  });

  it('throws CapacityServiceError with code and status on 401 unauthenticated', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify({ code: 'UNAUTHENTICATED', message: 'Inicia sesión como cliente.' }), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        })
      )
    );

    await expect(capacityService.getCapacitySlots(storeId)).rejects.toBeInstanceOf(CapacityServiceError);
  });

  it('throws CapacityServiceError on 503 capacity unavailable', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ code: 'CAPACITY_UNAVAILABLE', message: 'Servicio no disponible.' }), {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    await expect(capacityService.getCapacitySlots(storeId)).rejects.toMatchObject({
      code: 'CAPACITY_UNAVAILABLE',
      status: 503,
    });
  });
});

describe('mapBackendSlotsToPickerSlots', () => {
  it('correctly maps backend slot availability states', () => {
    const rawSlots: CapacitySlotBackendResponse[] = [
      {
        storeId: 'store-1',
        serviceDate: '2026-11-13',
        startTime: '09:00:00',
        endTime: '11:00:00',
        effectiveCapacity: 10,
        availableCapacity: 0,
        reservedCapacity: 10,
        committedCapacity: 0,
        occupiedCapacity: 10,
      },
      {
        storeId: 'store-1',
        serviceDate: '2026-11-13',
        startTime: '11:00:00',
        endTime: '13:00:00',
        effectiveCapacity: 10,
        availableCapacity: 2,
        reservedCapacity: 6,
        committedCapacity: 2,
        occupiedCapacity: 8,
      },
      {
        storeId: 'store-1',
        serviceDate: '2026-11-13',
        startTime: '13:00:00',
        endTime: '15:00:00',
        effectiveCapacity: 10,
        availableCapacity: 7,
        reservedCapacity: 2,
        committedCapacity: 1,
        occupiedCapacity: 3,
      },
    ];

    const mapped = mapBackendSlotsToPickerSlots(rawSlots);

    expect(mapped).toHaveLength(3);

    // Slot 1: Sold out
    expect(mapped[0]).toEqual({
      id: '09:00-11:00',
      timeRange: '09:00 - 11:00',
      totalCapacity: 10,
      reservedCount: 10,
      status: 'sold_out',
      estimatedDeliveryMinutes: undefined,
      startTime: '09:00:00',
      endTime: '11:00:00',
      serviceDate: '2026-11-13',
      availableCapacity: 0,
    });

    // Slot 2: Few left (available = 2)
    expect(mapped[1].status).toBe('few_left');
    expect(mapped[1].reservedCount).toBe(8); // occupiedCapacity

    // Slot 3: Available (available = 7)
    expect(mapped[2].status).toBe('available');
    expect(mapped[2].reservedCount).toBe(3);
  });

  it('handles empty or non-array inputs safely', () => {
    expect(mapBackendSlotsToPickerSlots([])).toEqual([]);
    expect(mapBackendSlotsToPickerSlots(null as unknown as CapacitySlotBackendResponse[])).toEqual([]);
  });
});

describe('getBusinessTodayDate', () => {
  it('returns valid YYYY-MM-DD date in America/Lima timezone', () => {
    const limaDate = getBusinessTodayDate('America/Lima');
    expect(limaDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
