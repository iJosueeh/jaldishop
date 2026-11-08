import {
  DeliveryMode,
  MerchantOrder,
  MerchantOrderDetailResponse,
  MerchantOrderItemResponse,
  MerchantOrderSummaryResponse,
  OrderItem,
  OrderStatus,
} from '../models/order.models';
import { DashboardPriorityOrder, OrderItemDetail } from '../models/dashboard.models';

export function mapOrderItemResponseToOrderItem(item: MerchantOrderItemResponse): OrderItem {
  return {
    name: item.productName,
    variant: item.variantName ? item.variantName : undefined,
    quantity: item.quantity,
    unitPrice: typeof item.unitPriceAmount === 'number' ? item.unitPriceAmount : Number(item.unitPriceAmount) || 0,
    totalPrice: typeof item.subtotalAmount === 'number' ? item.subtotalAmount : Number(item.subtotalAmount) || 0,
  };
}

export function formatDeliveryTimeLabel(
  serviceDate: string,
  startTime?: string | null,
  endTime?: string | null,
): string {
  if (startTime && endTime) {
    const start = startTime.slice(0, 5);
    const end = endTime.slice(0, 5);
    return `${start} - ${end}`;
  }
  if (startTime) {
    return startTime.slice(0, 5);
  }
  return 'Horario no definido';
}

export function mapSummaryToMerchantOrder(summary: MerchantOrderSummaryResponse): MerchantOrder {
  const items = Array.isArray(summary.items)
    ? summary.items.map(mapOrderItemResponseToOrderItem)
    : [];

  return {
    id: summary.id,
    orderNumber: summary.orderNumber,
    customerName: summary.customerName,
    customerPhone: summary.customerPhone ?? undefined,
    channel: undefined,
    channelLabel: undefined,
    deliveryMode: summary.deliveryMode,
    deliveryAddress: summary.deliveryAddress ?? undefined,
    deliveryReference: summary.deliveryReference ?? undefined,
    scheduledTime: summary.serviceStartTime ? summary.serviceStartTime.slice(0, 5) : undefined,
    deliveryTimeLabel: formatDeliveryTimeLabel(
      summary.serviceDate,
      summary.serviceStartTime,
      summary.serviceEndTime,
    ),
    isUrgent: false,
    urgentLabel: undefined,
    status: summary.status,
    paymentMethod: undefined,
    paymentLabel: undefined,
    totalAmount: typeof summary.totalAmount === 'number' ? summary.totalAmount : Number(summary.totalAmount) || 0,
    notes: undefined,
    items,
    createdAt: summary.confirmedAt || summary.updatedAt,
  };
}

export function mapDetailToMerchantOrder(detail: MerchantOrderDetailResponse): MerchantOrder {
  const items = Array.isArray(detail.items)
    ? detail.items.map(mapOrderItemResponseToOrderItem)
    : [];

  return {
    id: detail.id,
    orderNumber: detail.orderNumber,
    customerName: detail.customerName,
    customerPhone: detail.customerPhone ?? undefined,
    channel: undefined,
    channelLabel: undefined,
    deliveryMode: detail.deliveryMode,
    deliveryAddress: detail.deliveryAddress ?? undefined,
    deliveryReference: detail.deliveryReference ?? undefined,
    scheduledTime: detail.serviceStartTime ? detail.serviceStartTime.slice(0, 5) : undefined,
    deliveryTimeLabel: formatDeliveryTimeLabel(
      detail.serviceDate,
      detail.serviceStartTime,
      detail.serviceEndTime,
    ),
    isUrgent: false,
    urgentLabel: undefined,
    status: detail.status,
    paymentMethod: undefined,
    paymentLabel: undefined,
    totalAmount: typeof detail.totalAmount === 'number' ? detail.totalAmount : Number(detail.totalAmount) || 0,
    notes: undefined,
    items,
    createdAt: detail.confirmedAt || detail.updatedAt,
  };
}

export function mapDetailToDashboardPriorityOrder(
  detail: MerchantOrderDetailResponse,
): DashboardPriorityOrder {
  const items: OrderItemDetail[] = Array.isArray(detail.items)
    ? detail.items.map((i) => ({
        name: i.productName,
        variant: i.variantName ?? undefined,
        quantity: i.quantity,
        unitPrice: typeof i.unitPriceAmount === 'number' ? i.unitPriceAmount : Number(i.unitPriceAmount) || 0,
        totalPrice: typeof i.subtotalAmount === 'number' ? i.subtotalAmount : Number(i.subtotalAmount) || 0,
      }))
    : [];

  const itemSummary = items.length > 0
    ? items.map((i) => `${i.quantity}x ${i.name}`).join(', ')
    : 'Sin productos detallados';

  return {
    id: detail.id,
    orderNumber: detail.orderNumber,
    customerName: detail.customerName,
    customerPhone: detail.customerPhone ?? undefined,
    channel: undefined,
    channelLabel: undefined,
    channelIcon: undefined,
    icon: 'matRestaurantOutline',
    itemSummary,
    itemDetails: undefined,
    deliveryMode: detail.deliveryMode,
    deliveryAddress: detail.deliveryAddress ?? undefined,
    deliveryReference: detail.deliveryReference ?? undefined,
    scheduledTime: detail.serviceStartTime ? detail.serviceStartTime.slice(0, 5) : '00:00',
    deliveryTimeLabel: formatDeliveryTimeLabel(
      detail.serviceDate,
      detail.serviceStartTime,
      detail.serviceEndTime,
    ),
    isUrgent: false,
    status: detail.status,
    notes: undefined,
    totalAmount: typeof detail.totalAmount === 'number' ? detail.totalAmount : Number(detail.totalAmount) || 0,
    items,
  };
}
