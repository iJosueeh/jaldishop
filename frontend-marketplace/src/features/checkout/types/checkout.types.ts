export type CheckoutFulfillmentType = 'DELIVERY' | 'PICKUP';

export interface InitiateCheckoutPayload {
  storeId: string;
  fulfillmentType: CheckoutFulfillmentType;
  serviceDate: string;
  startTime?: string | null;
  endTime?: string | null;
  customerName?: string | null;
  customerPhone?: string | null;
  customerEmail?: string | null;
  deliveryAddress?: string | null;
  deliveryReference?: string | null;
  deliveryLatitude?: number | null;
  deliveryLongitude?: number | null;
  customerDocumentNumber?: string | null;
  orderNotes?: string | null;
  discountCode?: string | null;
}

export interface CheckoutCustomerInfo {
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  deliveryAddress?: string | null;
  deliveryReference?: string | null;
}

export interface CheckoutItem {
  id?: string;
  productId: string;
  variantId?: string | null;
  productName: string;
  presentationName?: string | null;
  variantName?: string | null;
  sku?: string | null;
  imageUrl?: string | null;
  quantity: number;
  unitPriceAmount: number;
  subtotalAmount: number;
  currency: string;
  tracksInventory?: boolean;
}

export interface CheckoutPricing {
  currency: string;
  productsSubtotalAmount: number;
  discountAmount: number;
  discountCode?: string | null;
  deliveryFeeAmount: number;
  serviceFeeRate?: number | null;
  serviceFeeAmount?: number | null;
  taxRate?: number | null;
  includedTaxAmount: number;
  totalAmount: number;
}

export interface CheckoutResponse {
  reservationId: string;
  reservationExpiresAt: string;
  storeId: string;
  storeName: string;
  userId: string;
  fulfillmentType: CheckoutFulfillmentType;
  serviceDate: string;
  startTime?: string | null;
  endTime?: string | null;
  customerInfo: CheckoutCustomerInfo;
  items?: CheckoutItem[];
  pricing: CheckoutPricing;
  createdAt: string;
}

export interface DeliveryFormData {
  mode: 'saved' | 'manual';
  selectedAddressId?: string | null;
  recipientName: string;
  recipientPhone: string;
  department: string;
  province: string;
  district: string;
  ubigeoCode: string;
  addressLine: string;
  reference?: string | null;
  saveToAddresses?: boolean;
  alias?: string;
  latitude?: number | null;
  longitude?: number | null;
}

export interface CreatePaymentPreferencePayload {
  capacityReservationId: string;
  fulfillmentType: CheckoutFulfillmentType;
  customerName?: string | null;
  customerPhone?: string | null;
  customerEmail?: string | null;
  deliveryAddress?: string | null;
  deliveryReference?: string | null;
  deliveryLatitude?: number | null;
  deliveryLongitude?: number | null;
  storeId?: string;
  storeName?: string;
  returnBaseUrl?: string;
  discountCode?: string | null;
  requestedReceiptType?: 'BOLETA' | 'FACTURA' | null;
  customerDocType?: 'DNI' | 'RUC' | null;
  customerDocNumber?: string | null;
  customerLegalName?: string | null;
}

export interface ValidatePromoPayload {
  storeId: string;
  code: string;
  subtotal: number;
}

export interface ValidatePromoResponse {
  valid: boolean;
  code?: string | null;
  discountType?: 'PERCENTAGE' | 'FIXED_AMOUNT' | null;
  discountValue?: number | null;
  discountAmount: number;
  newSubtotal: number;
  errorCode?: string | null;
  message?: string | null;
}

export interface CreatePaymentPreferenceResponse {
  paymentId: string;
  preferenceId: string;
  initPoint: string;
  status: string;
}

export interface PendingPaymentCorrelation {
  paymentId: string;
  preferenceId: string;
  capacityReservationId: string;
  storeId?: string;
  storeName?: string;
  createdAt: number;
  status: string;
}

export interface CustomerPaymentStatusResponse {
  paymentId: string;
  paymentStatus:
    | 'PENDING'
    | 'APPROVED'
    | 'FAILED'
    | 'PROCESSING'
    | 'IN_PROCESS'
    | 'REJECTED'
    | 'CANCELLED'
    | 'REFUNDED'
    | string;
  orderCreated: boolean;
  orderNumber: string | null;
  orderStatus: string | null;
  hasIncidence: boolean;
  incidenceCode: string | null;
  incidenceMessage: string | null;
  refundStatus: string;
  amount: number;
  currency: string;
  createdAt: string;
  approvedAt: string | null;
}


