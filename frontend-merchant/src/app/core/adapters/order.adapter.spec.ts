import { describe, expect, it } from 'vitest';
import {
  formatDeliveryTimeLabel,
  mapDetailToDashboardPriorityOrder,
  mapDetailToMerchantOrder,
  mapOrderItemResponseToOrderItem,
  mapSummaryToMerchantOrder,
} from './order.adapter';
import {
  MerchantOrderDetailResponse,
  MerchantOrderItemResponse,
  MerchantOrderSummaryResponse,
} from '../models/order.models';

describe('OrderAdapter', () => {
  const sampleOrderItem: MerchantOrderItemResponse = {
    id: 'f87d4001-fce4-4363-95ad-e0d00f8ab881',
    variantId: '80acf0ba-5a79-4888-b00d-b5aef8fcf301',
    productName: 'Torta de Chocolate',
    variantName: 'Entera 8-10 porciones',
    quantity: 2,
    unitPriceAmount: 55.0,
    subtotalAmount: 110.0,
  };

  const sampleSummaryPickup: MerchantOrderSummaryResponse = {
    id: '99acf0ba-5a79-4888-b00d-b5aef8fcf901',
    orderNumber: 'JAL-2026-000001',
    storeId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    customerUserId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    status: 'CONFIRMED',
    deliveryMode: 'PICKUP',
    serviceDate: '2026-11-05',
    serviceStartTime: '10:00:00',
    serviceEndTime: '12:00:00',
    customerName: 'Carlos Mendoza',
    customerPhone: '987654321',
    deliveryAddress: null,
    deliveryReference: null,
    totalAmount: 110.0,
    currency: 'PEN',
    totalItems: 2,
    confirmedAt: '2026-11-01T14:30:00Z',
    updatedAt: '2026-11-01T14:30:00Z',
    items: [sampleOrderItem],
  };

  const sampleSummaryDelivery: MerchantOrderSummaryResponse = {
    id: '99acf0ba-5a79-4888-b00d-b5aef8fcf902',
    orderNumber: 'JAL-2026-000002',
    storeId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    customerUserId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    status: 'IN_PREPARATION',
    deliveryMode: 'DELIVERY',
    serviceDate: '2026-11-06',
    serviceStartTime: '14:30:00',
    serviceEndTime: '16:00:00',
    customerName: 'Lucía Fernández',
    customerPhone: null,
    deliveryAddress: 'Av. Larco 456, Miraflores',
    deliveryReference: 'Dpto 301, frente al parque',
    totalAmount: 45.5,
    currency: 'PEN',
    totalItems: 1,
    confirmedAt: '2026-11-02T10:15:00Z',
    updatedAt: '2026-11-02T10:20:00Z',
    items: [
      {
        id: 'f87d4001-fce4-4363-95ad-e0d00f8ab882',
        variantId: '80acf0ba-5a79-4888-b00d-b5aef8fcf302',
        productName: 'Cheesecake de Fresa',
        variantName: '',
        quantity: 1,
        unitPriceAmount: 45.5,
        subtotalAmount: 45.5,
      },
    ],
  };

  const sampleDetailDelivery: MerchantOrderDetailResponse = {
    id: '99acf0ba-5a79-4888-b00d-b5aef8fcf903',
    orderNumber: 'JAL-2026-000003',
    storeId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5',
    customerUserId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e2',
    paymentId: '77acf0ba-5a79-4888-b00d-b5aef8fcf701',
    capacityReservationId: '66acf0ba-5a79-4888-b00d-b5aef8fcf601',
    status: 'READY',
    deliveryMode: 'DELIVERY',
    serviceDate: '2026-11-07',
    serviceStartTime: '09:00:00',
    serviceEndTime: '11:00:00',
    customerName: 'Valeria Castro',
    customerPhone: '912345678',
    customerEmail: 'valeria@example.pe',
    deliveryAddress: 'Calle Las Begonias 123, San Isidro',
    deliveryReference: 'Torre Empresarial, Piso 12',
    deliveryLatitude: -12.0931,
    deliveryLongitude: -77.0254,
    currency: 'PEN',
    productsSubtotalAmount: 110.0,
    discountAmount: 10.0,
    discountCode: 'PROMO10',
    deliveryFeeAmount: 8.0,
    includedTaxAmount: 16.47,
    totalAmount: 108.0,
    confirmedAt: '2026-11-03T08:00:00Z',
    updatedAt: '2026-11-03T08:45:00Z',
    items: [sampleOrderItem],
    statusHistory: [
      {
        id: '11acf0ba-5a79-4888-b00d-b5aef8fcf101',
        changedByUserId: null,
        status: 'CONFIRMED',
        reason: 'Pago acreditado',
        changedAt: '2026-11-03T08:00:00Z',
      },
      {
        id: '11acf0ba-5a79-4888-b00d-b5aef8fcf102',
        changedByUserId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
        status: 'IN_PREPARATION',
        reason: 'Elaboración iniciada',
        changedAt: '2026-11-03T08:15:00Z',
      },
      {
        id: '11acf0ba-5a79-4888-b00d-b5aef8fcf103',
        changedByUserId: '80acf0ba-5a79-4888-b00d-b5aef8fcf9e1',
        status: 'READY',
        reason: 'Listo para despacho',
        changedAt: '2026-11-03T08:45:00Z',
      },
    ],
  };

  describe('formatDeliveryTimeLabel', () => {
    it('debe formatear inicio y fin cortando segundos a HH:mm', () => {
      const label = formatDeliveryTimeLabel('2026-11-05', '10:00:00', '12:00:00');
      expect(label).toBe('10:00 - 12:00');
    });

    it('debe formatear correctamente cuando solo se dispone de hora de inicio', () => {
      const label = formatDeliveryTimeLabel('2026-11-05', '14:30:00', null);
      expect(label).toBe('14:30');
    });

    it('debe retornar "Horario no definido" cuando no hay horas especificadas', () => {
      const label = formatDeliveryTimeLabel('2026-11-05', null, null);
      expect(label).toBe('Horario no definido');
    });
  });

  describe('mapOrderItemResponseToOrderItem', () => {
    it('debe mapear correctamente un ítem con variante y valores numéricos', () => {
      const item = mapOrderItemResponseToOrderItem(sampleOrderItem);
      expect(item.name).toBe('Torta de Chocolate');
      expect(item.variant).toBe('Entera 8-10 porciones');
      expect(item.quantity).toBe(2);
      expect(item.unitPrice).toBe(55.0);
      expect(item.totalPrice).toBe(110.0);
    });

    it('debe omitir variant cuando es cadena vacía o nula', () => {
      const item = mapOrderItemResponseToOrderItem({
        ...sampleOrderItem,
        variantName: '',
      });
      expect(item.variant).toBeUndefined();
    });

    it('debe manejar importes numéricos o strings provenientes de JSON de forma segura', () => {
      const item = mapOrderItemResponseToOrderItem({
        ...sampleOrderItem,
        unitPriceAmount: '25.50' as unknown as number,
        subtotalAmount: '51.00' as unknown as number,
      });
      expect(item.unitPrice).toBe(25.5);
      expect(item.totalPrice).toBe(51.0);
    });
  });

  describe('mapSummaryToMerchantOrder', () => {
    it('debe transformar pedido PICKUP preservando identificador, orderNumber y estado CONFIRMED', () => {
      const order = mapSummaryToMerchantOrder(sampleSummaryPickup);

      expect(order.id).toBe(sampleSummaryPickup.id);
      expect(order.orderNumber).toBe('JAL-2026-000001');
      expect(order.status).toBe('CONFIRMED');
      expect(order.deliveryMode).toBe('PICKUP');
      expect(order.customerName).toBe('Carlos Mendoza');
      expect(order.customerPhone).toBe('987654321');
      expect(order.deliveryAddress).toBeUndefined();
      expect(order.deliveryReference).toBeUndefined();
      expect(order.totalAmount).toBe(110.0);
      expect(order.isUrgent).toBe(false);
      expect(order.deliveryTimeLabel).toBe('10:00 - 12:00');
      expect(order.items.length).toBe(1);
      expect(order.items[0].name).toBe('Torta de Chocolate');
      expect(order.createdAt).toBe('2026-11-01T14:30:00Z');
    });

    it('no debe inventar método de pago ni estado de pago al no existir en el DTO', () => {
      const order = mapSummaryToMerchantOrder(sampleSummaryPickup);

      // Verificación estricta: ausencia de valores inventados
      expect(order.paymentMethod).toBeUndefined();
      expect(order.paymentLabel).toBeUndefined();
    });

    it('no debe inventar canal comercial al no ser informado por el backend', () => {
      const order = mapSummaryToMerchantOrder(sampleSummaryPickup);

      // Verificación estricta: canal ausente para no simular procedencia comercial
      expect(order.channel).toBeUndefined();
      expect(order.channelLabel).toBeUndefined();
    });

    it('debe transformar pedido DELIVERY con dirección, referencia y teléfono nulo', () => {
      const order = mapSummaryToMerchantOrder(sampleSummaryDelivery);

      expect(order.id).toBe(sampleSummaryDelivery.id);
      expect(order.orderNumber).toBe('JAL-2026-000002');
      expect(order.status).toBe('IN_PREPARATION');
      expect(order.deliveryMode).toBe('DELIVERY');
      expect(order.customerName).toBe('Lucía Fernández');
      expect(order.customerPhone).toBeUndefined();
      expect(order.deliveryAddress).toBe('Av. Larco 456, Miraflores');
      expect(order.deliveryReference).toBe('Dpto 301, frente al parque');
      expect(order.totalAmount).toBe(45.5);
      expect(order.deliveryTimeLabel).toBe('14:30 - 16:00');
      expect(order.items.length).toBe(1);
      expect(order.items[0].name).toBe('Cheesecake de Fresa');
    });

    it('debe manejar la ausencia de productos en el resumen sin arrojar excepciones', () => {
      const summaryWithoutItems: MerchantOrderSummaryResponse = {
        ...sampleSummaryPickup,
        items: [] as MerchantOrderItemResponse[],
      };

      const order = mapSummaryToMerchantOrder(summaryWithoutItems);
      expect(order.items).toEqual([]);
      expect(order.totalAmount).toBe(110.0);
    });
  });

  describe('mapDetailToMerchantOrder', () => {
    it('debe transformar respuesta de detalle en MerchantOrder preservando datos reales sin inventar pago ni canal', () => {
      const order = mapDetailToMerchantOrder(sampleDetailDelivery);

      expect(order.id).toBe(sampleDetailDelivery.id);
      expect(order.orderNumber).toBe('JAL-2026-000003');
      expect(order.status).toBe('READY');
      expect(order.deliveryMode).toBe('DELIVERY');
      expect(order.customerName).toBe('Valeria Castro');
      expect(order.customerPhone).toBe('912345678');
      expect(order.deliveryAddress).toBe('Calle Las Begonias 123, San Isidro');
      expect(order.deliveryReference).toBe('Torre Empresarial, Piso 12');
      expect(order.totalAmount).toBe(108.0);
      expect(order.deliveryTimeLabel).toBe('09:00 - 11:00');
      expect(order.items.length).toBe(1);
      expect(order.items[0].name).toBe('Torta de Chocolate');

      // Verificación de no invención
      expect(order.paymentMethod).toBeUndefined();
      expect(order.paymentLabel).toBeUndefined();
      expect(order.channel).toBeUndefined();
      expect(order.channelLabel).toBeUndefined();
    });
  });

  describe('mapDetailToDashboardPriorityOrder', () => {
    it('debe transformar respuesta de detalle a la estructura del Drawer lateral sin datos inventados', () => {
      const drawerOrder = mapDetailToDashboardPriorityOrder(sampleDetailDelivery);

      expect(drawerOrder.id).toBe(sampleDetailDelivery.id);
      expect(drawerOrder.orderNumber).toBe('JAL-2026-000003');
      expect(drawerOrder.customerName).toBe('Valeria Castro');
      expect(drawerOrder.channel).toBeUndefined();
      expect(drawerOrder.channelLabel).toBeUndefined();
      expect(drawerOrder.channelIcon).toBeUndefined();
      expect(drawerOrder.icon).toBe('matRestaurantOutline');
      expect(drawerOrder.itemSummary).toBe('2x Torta de Chocolate');
      expect(drawerOrder.deliveryMode).toBe('DELIVERY');
      expect(drawerOrder.deliveryAddress).toBe('Calle Las Begonias 123, San Isidro');
      expect(drawerOrder.deliveryReference).toBe('Torre Empresarial, Piso 12');
      expect(drawerOrder.status).toBe('READY');
      expect(drawerOrder.totalAmount).toBe(108.0);
      expect(drawerOrder.items?.length).toBe(1);
    });

    it('debe generar itemSummary con "Sin productos detallados" cuando no hay ítems', () => {
      const emptyDetail: MerchantOrderDetailResponse = {
        ...sampleDetailDelivery,
        items: [],
      };
      const drawerOrder = mapDetailToDashboardPriorityOrder(emptyDetail);
      expect(drawerOrder.itemSummary).toBe('Sin productos detallados');
      expect(drawerOrder.items).toEqual([]);
    });
  });
});
