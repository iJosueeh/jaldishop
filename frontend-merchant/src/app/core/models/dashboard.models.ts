export type OrderStatus =
  'CONFIRMED' | 'IN_PREPARATION' | 'READY' | 'OUT_FOR_DELIVERY' | 'COMPLETED' | 'CANCELLED';

export type DeliveryMode = 'PICKUP' | 'DELIVERY';

export type OrderChannel = 'WHATSAPP' | 'INSTAGRAM' | 'COUNTER' | 'WEB_STORE';

export interface OrderItemDetail {
  name: string;
  variant?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface DashboardPriorityOrder {
  id: string;
  orderNumber: string;
  customerName: string;
  channel: OrderChannel;
  channelLabel: string;
  channelIcon: string;
  icon: string;
  itemSummary: string;
  itemDetails?: string;
  deliveryMode: DeliveryMode;
  scheduledTime: string;
  deliveryTimeLabel: string;
  isUrgent: boolean;
  status: OrderStatus;
  customerPhone?: string;
  deliveryAddress?: string;
  deliveryReference?: string;
  notes?: string;
  totalAmount?: number;
  items?: OrderItemDetail[];
}

export interface UpcomingDeliveryItem {
  id: string;
  customerName: string;
  deliveryMode: DeliveryMode;
  scheduledTime: string;
  timeLabel: string;
  urgency: 'NORMAL' | 'WARNING' | 'CRITICAL';
}

export interface StockAlertItem {
  id: string;
  productName: string;
  availableUnits: number;
  stockStatus: 'LOW_STOCK' | 'OUT_OF_STOCK';
}

export interface DashboardMetrics {
  capacityOccupied: number;
  capacityTotal: number;
  shiftSchedule: string;
  totalOrdersToday: number;
  confirmedOrders: number;
  inPrepOrders: number;
  readyOrders: number;
  completedOrders: number;
  operatingRhythmMin: number;
  targetRhythmMin: number;
}
