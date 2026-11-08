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
  channel?: OrderChannel;
  channelLabel?: string;
  deliveryMode: DeliveryMode;
  deliveryAddress?: string;
  deliveryReference?: string;
  scheduledTime?: string;
  deliveryTimeLabel: string;
  isUrgent: boolean;
  urgentLabel?: string;
  status: OrderStatus;
  paymentMethod?: PaymentMethod;
  paymentLabel?: string;
  totalAmount: number;
  notes?: string;
  items: OrderItem[];
  createdAt: string;
}

export type OrderFilterTab = 'ALL' | 'CONFIRMED' | 'IN_PREPARATION' | 'READY' | 'COMPLETED';

// =============================================================================
// CONTRATOS REST BACKEND (Equivalentes estrictos a los records Java)
// =============================================================================

/**
 * Representa una línea de producto del pedido tal como la entrega el backend.
 * Equivalente a {@code MerchantOrderItemResponse.java}.
 */
export interface MerchantOrderItemResponse {
  id: string;
  variantId: string;
  productName: string;
  variantName: string;
  quantity: number;
  unitPriceAmount: number;
  subtotalAmount: number;
}

/**
 * Representa una transición en el historial de estados del pedido.
 * Equivalente a {@code MerchantOrderStatusHistoryResponse.java}.
 */
export interface MerchantOrderStatusHistoryResponse {
  id: string;
  changedByUserId: string | null;
  status: OrderStatus;
  reason: string | null;
  changedAt: string;
}

/**
 * Resumen de pedido para listados y comandas del comerciante.
 * Equivalente a {@code MerchantOrderSummaryResponse.java}.
 */
export interface MerchantOrderSummaryResponse {
  id: string;
  orderNumber: string;
  storeId: string;
  customerUserId: string;
  status: OrderStatus;
  deliveryMode: DeliveryMode;
  serviceDate: string;
  serviceStartTime: string;
  serviceEndTime: string;
  customerName: string;
  customerPhone: string | null;
  deliveryAddress: string | null;
  deliveryReference: string | null;
  totalAmount: number;
  currency: string;
  totalItems: number;
  confirmedAt: string;
  updatedAt: string;
  items: MerchantOrderItemResponse[];
}

/**
 * Detalle exhaustivo de un pedido para el comerciante.
 * Equivalente a {@code MerchantOrderDetailResponse.java}.
 */
export interface MerchantOrderDetailResponse {
  id: string;
  orderNumber: string;
  storeId: string;
  customerUserId: string;
  paymentId: string;
  capacityReservationId: string;
  status: OrderStatus;
  deliveryMode: DeliveryMode;
  serviceDate: string;
  serviceStartTime: string;
  serviceEndTime: string;
  customerName: string;
  customerPhone: string | null;
  customerEmail: string | null;
  deliveryAddress: string | null;
  deliveryReference: string | null;
  deliveryLatitude: number | null;
  deliveryLongitude: number | null;
  currency: string;
  productsSubtotalAmount: number;
  discountAmount: number;
  discountCode: string | null;
  deliveryFeeAmount: number;
  includedTaxAmount: number;
  totalAmount: number;
  confirmedAt: string;
  updatedAt: string;
  items: MerchantOrderItemResponse[];
  statusHistory: MerchantOrderStatusHistoryResponse[];
}

/**
 * Contenedor paginado con metadatos estándar de Spring Data Page.
 * Equivalente a {@code MerchantOrderPageResponse.java}.
 */
export interface MerchantOrderPageResponse {
  content: MerchantOrderSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/**
 * Payload para mutación de estado por parte del comerciante (PATCH).
 * Equivalente a {@code ChangeOrderStatusRequest.java}.
 */
export interface ChangeOrderStatusRequest {
  status: OrderStatus;
}

// =============================================================================
// CONTRATOS WEBSOCKET / STOMP
// =============================================================================

/**
 * Estados del ciclo de vida de la conexión STOMP.
 */
export type StompConnectionState =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'DISCONNECTED'
  | 'RECONNECTING'
  | 'AUTH_ERROR';

/**
 * Evento ligero de dominio emitido por el backend al confirmar un pedido.
 * Equivalente a {@code OrderConfirmedDomainEvent.java} en /topic/stores/{storeId}/orders.
 */
export interface OrderConfirmedDomainEvent {
  eventId: string;
  eventType: 'ORDER_CONFIRMED';
  orderId: string;
  orderNumber: string;
  storeId: string;
  totalAmount: number;
  currency: string;
  occurredAt: string;
}

