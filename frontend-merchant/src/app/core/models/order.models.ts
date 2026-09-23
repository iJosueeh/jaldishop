export type OrderStatus =
  | 'CONFIRMED'
  | 'IN_PREPARATION'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'COMPLETED'
  | 'CANCELLED';

export type DeliveryMode = 'PICKUP' | 'DELIVERY';

export type OrderChannel = 'WHATSAPP' | 'INSTAGRAM' | 'COUNTER' | 'WEB_STORE';

export type PaymentMethod = 'YAPE' | 'PLIN' | 'BCP' | 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA';

export interface OrderItem {
  name: string;
  variant?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface MerchantOrder {
  id: string;
  orderNumber: string;
  customerName: string;
  customerPhone?: string;
  channel: OrderChannel;
  channelLabel: string;
  deliveryMode: DeliveryMode;
  deliveryAddress?: string;
  deliveryReference?: string;
  scheduledTime?: string;
  deliveryTimeLabel: string;
  isUrgent: boolean;
  urgentLabel?: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentLabel?: string;
  totalAmount: number;
  notes?: string;
  items: OrderItem[];
  createdAt: string;
}

export type OrderFilterTab = 'ALL' | 'CONFIRMED' | 'IN_PREPARATION' | 'READY' | 'COMPLETED';
