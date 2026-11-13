import { CapacitySlot } from '../components/CapacitySlotPicker';

export interface CapacitySlotBackendResponse {
  storeId: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  effectiveCapacity: number;
  availableCapacity: number;
  reservedCapacity: number;
  committedCapacity: number;
  occupiedCapacity: number;
}

export interface CapacitySlotsBackendResponse {
  storeId: string;
  serviceDate: string;
  slots: CapacitySlotBackendResponse[];
}

export class CapacityServiceError extends Error {
  code: string;
  status: number;

  constructor(message: string, code: string, status: number) {
    super(message);
    this.name = 'CapacityServiceError';
    this.code = code;
    this.status = status;
  }
}

/**
 * Calculates current date in business timezone (America/Lima by default) formatted as YYYY-MM-DD.
 * Matches backend ListStoreCapacitySlotsService today calculation.
 */
export function getBusinessTodayDate(timeZone = 'America/Lima'): string {
  try {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());
  } catch {
    const d = new Date();
    return d.toISOString().split('T')[0];
  }
}

/**
 * Maps raw backend CapacitySlotResponse list to CapacitySlot format required by CapacitySlotPicker.
 */
export function mapBackendSlotsToPickerSlots(
  backendSlots: CapacitySlotBackendResponse[]
): CapacitySlot[] {
  if (!backendSlots || !Array.isArray(backendSlots)) {
    return [];
  }

  return backendSlots.map((slot) => {
    const startFormatted = slot.startTime.length >= 5 ? slot.startTime.substring(0, 5) : slot.startTime;
    const endFormatted = slot.endTime.length >= 5 ? slot.endTime.substring(0, 5) : slot.endTime;
    const timeRange = `${startFormatted} - ${endFormatted}`;
    const id = `${startFormatted}-${endFormatted}`;

    let status: 'available' | 'few_left' | 'sold_out' = 'available';
    if (slot.availableCapacity <= 0) {
      status = 'sold_out';
    } else if (slot.availableCapacity <= 2) {
      status = 'few_left';
    }

    return {
      id,
      timeRange,
      totalCapacity: slot.effectiveCapacity,
      reservedCount: slot.occupiedCapacity,
      status,
      estimatedDeliveryMinutes: 45,
      startTime: slot.startTime,
      endTime: slot.endTime,
      serviceDate: slot.serviceDate,
      availableCapacity: slot.availableCapacity,
    };
  });
}

export const capacityService = {
  /**
   * Fetches real capacity slots for a store on a given date via the customer BFF proxy.
   */
  async getCapacitySlots(
    storeId: string,
    date?: string
  ): Promise<CapacitySlotsBackendResponse> {
    const params = new URLSearchParams();
    params.set('storeId', storeId.trim());
    if (date) {
      params.set('date', date.trim());
    }

    const response = await fetch(`/api/customer/capacity/slots?${params.toString()}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const code = errorData.code || 'CAPACITY_ERROR';
      const message =
        errorData.message ||
        (response.status === 401
          ? 'Inicia sesión para consultar los horarios disponibles.'
          : 'No se pudieron consultar los horarios de entrega.');

      throw new CapacityServiceError(message, code, response.status);
    }

    return response.json();
  },
};
