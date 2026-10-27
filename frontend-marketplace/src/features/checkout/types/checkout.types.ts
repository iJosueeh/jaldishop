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
  id: string;
  productId: string;
  variantId?: string | null;
  productName: string;
  variantName?: string | null;
  quantity: number;
  unitPriceAmount: number;
  subtotalAmount: number;
  currency: string;
}

export interface CheckoutPricing {
  currency: string;
  productsSubtotalAmount: number;
  discountAmount: number;
  discountCode?: string | null;
  deliveryFeeAmount: number;
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
