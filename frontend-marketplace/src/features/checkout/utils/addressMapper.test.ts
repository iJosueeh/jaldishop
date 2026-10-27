import { describe, expect, it } from 'vitest';
import {
  formatDeliveryAddressString,
  formatDeliveryReference,
  parseSlotTimeRange,
  mapCustomerAddressToCheckoutFields,
  mapDeliveryFormDataToCheckoutFields,
} from './addressMapper';
import { CustomerAddress } from '@/features/customer/types/customer.types';
import { DeliveryFormData } from '../types/checkout.types';

describe('addressMapper', () => {
  const mockAddress: CustomerAddress = {
    id: 'addr-1',
    alias: 'Casa',
    recipientName: 'Carlos Benítez',
    recipientPhone: '987654321',
    department: 'LIMA',
    province: 'LIMA',
    district: 'MIRAFLORES',
    ubigeoCode: '150122',
    addressLine: 'Av. Larco 1234, Dpto 402',
    reference: 'Frente al parque Kennedy',
    latitude: -12.1215,
    longitude: -77.0298,
    isDefault: true,
    createdAt: '2026-10-26T10:00:00Z',
    updatedAt: '2026-10-26T10:00:00Z',
  };

  it('formats delivery address string cleanly avoiding duplicate department/province for Lima', () => {
    const formatted = formatDeliveryAddressString(
      'Av. Larco 1234, Dpto 402',
      'MIRAFLORES',
      'LIMA',
      'LIMA'
    );
    expect(formatted).toBe('Av. Larco 1234, Dpto 402, MIRAFLORES, LIMA');
  });

  it('formats delivery address string for other provinces with 3 levels', () => {
    const formatted = formatDeliveryAddressString(
      'Calle Sol 100',
      'OLLANTAYTAMBO',
      'URUBAMBA',
      'CUSCO'
    );
    expect(formatted).toBe('Calle Sol 100, OLLANTAYTAMBO, URUBAMBA, CUSCO');
  });

  it('throws an explicit error without silent truncation when address exceeds 500 characters', () => {
    const hugeLine = 'A'.repeat(510);
    expect(() =>
      formatDeliveryAddressString(hugeLine, 'MIRAFLORES', 'LIMA', 'LIMA')
    ).toThrow(/excede el límite máximo permitido de 500 caracteres/i);
  });

  it('throws an explicit error without silent truncation when reference exceeds 300 characters', () => {
    const longRef = 'A'.repeat(305);
    expect(() => formatDeliveryReference(longRef)).toThrow(
      /excede el límite máximo permitido de 300 caracteres/i
    );
  });

  it('parses slot time range accurately into 24h LocalTime strings', () => {
    const res1 = parseSlotTimeRange('10:00 - 11:00 AM');
    expect(res1.startTime).toBe('10:00:00');
    expect(res1.endTime).toBe('11:00:00');

    const res2 = parseSlotTimeRange('02:00 - 03:00 PM');
    expect(res2.startTime).toBe('14:00:00');
    expect(res2.endTime).toBe('15:00:00');
  });

  it('maps CustomerAddress to InitiateCheckoutRequest fields', () => {
    const fields = mapCustomerAddressToCheckoutFields(mockAddress);
    expect(fields.deliveryAddress).toBe('Av. Larco 1234, Dpto 402, MIRAFLORES, LIMA');
    expect(fields.deliveryReference).toBe('Frente al parque Kennedy');
    expect(fields.customerName).toBe('Carlos Benítez');
    expect(fields.customerPhone).toBe('987654321');
    expect(fields.deliveryLatitude).toBe(-12.1215);
    expect(fields.deliveryLongitude).toBe(-77.0298);
  });

  it('maps DeliveryFormData to InitiateCheckoutRequest fields', () => {
    const formData: DeliveryFormData = {
      mode: 'manual',
      recipientName: 'Ana Gómez',
      recipientPhone: '912345678',
      department: 'AREQUIPA',
      province: 'AREQUIPA',
      district: 'YANAHUARA',
      ubigeoCode: '040126',
      addressLine: 'Calle San Francisco 200',
      reference: 'Casa de esquina',
      saveToAddresses: false,
    };

    const fields = mapDeliveryFormDataToCheckoutFields(formData);
    expect(fields.deliveryAddress).toBe('Calle San Francisco 200, YANAHUARA, AREQUIPA');
    expect(fields.customerName).toBe('Ana Gómez');
    expect(fields.customerPhone).toBe('912345678');
    expect(fields.deliveryReference).toBe('Casa de esquina');
    expect(fields.deliveryLatitude).toBeNull();
  });
});
