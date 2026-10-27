import { CustomerAddress } from '@/features/customer/types/customer.types';
import { DeliveryFormData } from '../types/checkout.types';

export const MAX_DELIVERY_ADDRESS_LENGTH = 500;
export const MAX_DELIVERY_REFERENCE_LENGTH = 300;

/**
 * Normalizes full address line into the format required by backend checkout snapshot.
 * Format: "Calle / Av / Nro, Distrito, Provincia, Departamento" (máx 500 caracteres).
 * Throws an explicit error if the formatted address exceeds 500 characters instead of truncating silently.
 */
export function formatDeliveryAddressString(
  addressLine: string,
  district: string,
  province: string,
  department: string
): string {
  const cleanLine = (addressLine || '').trim();
  const cleanDist = (district || '').trim();
  const cleanProv = (province || '').trim();
  const cleanDept = (department || '').trim();

  let location = '';
  if (cleanDept && cleanProv && cleanDept === cleanProv) {
    location = `${cleanDist}, ${cleanDept}`;
  } else if (cleanDept && cleanProv) {
    location = `${cleanDist}, ${cleanProv}, ${cleanDept}`;
  } else {
    location = [cleanDist, cleanProv, cleanDept].filter(Boolean).join(', ');
  }

  const result = location ? `${cleanLine}, ${location}` : cleanLine;

  if (result.length > MAX_DELIVERY_ADDRESS_LENGTH) {
    throw new Error(
      `La dirección completa de entrega (${result.length} caracteres) excede el límite máximo permitido de ${MAX_DELIVERY_ADDRESS_LENGTH} caracteres. Por favor abrevia algunos datos.`
    );
  }

  return result;
}

/**
 * Normalizes delivery reference up to 300 characters.
 * Throws an explicit error if reference exceeds 300 characters instead of truncating silently.
 */
export function formatDeliveryReference(reference?: string | null): string | null {
  if (!reference || !reference.trim()) return null;
  const trimmed = reference.trim();

  if (trimmed.length > MAX_DELIVERY_REFERENCE_LENGTH) {
    throw new Error(
      `La referencia de entrega (${trimmed.length} caracteres) excede el límite máximo permitido de ${MAX_DELIVERY_REFERENCE_LENGTH} caracteres.`
    );
  }

  return trimmed;
}

/**
 * Parses a slot timeRange string like "10:00 - 11:00 AM" or "14:00 - 15:00" into 24h LocalTime strings.
 */
export function parseSlotTimeRange(timeRange?: string): { startTime?: string; endTime?: string } {
  if (!timeRange) return {};
  const parts = timeRange.split('-');
  if (parts.length < 2) return {};

  const cleanStart = parts[0].trim();
  const cleanEnd = parts[1].trim();

  const to24h = (str: string, referenceMeridiem?: string) => {
    let hasPM = /pm/i.test(str);
    let hasAM = /am/i.test(str);
    if (!hasPM && !hasAM && referenceMeridiem) {
      hasPM = /pm/i.test(referenceMeridiem);
      hasAM = /am/i.test(referenceMeridiem);
    }
    const match = str.match(/(\d{1,2}):(\d{2})/);
    if (!match) return undefined;
    let h = parseInt(match[1], 10);
    const m = match[2];
    if (hasPM && h < 12) h += 12;
    if (hasAM && h === 12) h = 0;
    return `${h.toString().padStart(2, '0')}:${m}:00`;
  };

  const endMeridiem = cleanEnd.match(/[ap]m/i)?.[0];

  return {
    startTime: to24h(cleanStart, endMeridiem),
    endTime: to24h(cleanEnd),
  };
}

/**
 * Maps a saved CustomerAddress directly to the fields expected by InitiateCheckoutRequest.
 */
export function mapCustomerAddressToCheckoutFields(address: CustomerAddress) {
  return {
    deliveryAddress: formatDeliveryAddressString(
      address.addressLine,
      address.district,
      address.province,
      address.department
    ),
    deliveryReference: formatDeliveryReference(address.reference),
    customerName: (address.recipientName || '').trim(),
    customerPhone: (address.recipientPhone || '').trim(),
    deliveryLatitude: address.latitude ?? null,
    deliveryLongitude: address.longitude ?? null,
  };
}

/**
 * Maps DeliveryFormData (either saved or manual input) to the checkout fields.
 */
export function mapDeliveryFormDataToCheckoutFields(data: DeliveryFormData) {
  return {
    deliveryAddress: formatDeliveryAddressString(
      data.addressLine,
      data.district,
      data.province,
      data.department
    ),
    deliveryReference: formatDeliveryReference(data.reference),
    customerName: (data.recipientName || '').trim(),
    customerPhone: (data.recipientPhone || '').trim(),
    deliveryLatitude: data.latitude ?? null,
    deliveryLongitude: data.longitude ?? null,
  };
}
