export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  roles: string[];
  createdAt: string;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
  phone?: string | null;
}

export interface ProfileFormState {
  firstName: string;
  lastName: string;
  phone: string;
}

export type OrderStatus =
  | 'CONFIRMED'
  | 'IN_PREPARATION'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'COMPLETED'
  | 'CANCELLED';

export type DeliveryMode = 'DELIVERY' | 'PICKUP';

export interface CustomerOrderStoreInfo {
  id: string;
  name: string;
  slug: string;
  contactPhone?: string | null;
  whatsappNumber?: string | null;
  address?: string | null;
  addressReference?: string | null;
  logoUrl?: string | null;
}

export interface CustomerOrderItem {
  id: string;
  variantId?: string | null;
  productName: string;
  variantName?: string | null;
  quantity: number;
  unitPriceAmount: number;
  subtotalAmount: number;
}

export interface CustomerOrderStatusHistory {
  status: OrderStatus;
  reason?: string | null;
  changedAt: string;
}

export interface CustomerOrderSummary {
  id: string;
  orderNumber: string;
  storeId: string;
  storeName: string;
  storeSlug: string;
  storeLogoUrl?: string | null;
  status: OrderStatus;
  deliveryMode: DeliveryMode;
  serviceDate: string;
  serviceStartTime: string;
  serviceEndTime: string;
  totalAmount: number;
  currency: string;
  totalItems: number;
  confirmedAt: string;
}

export interface CustomerOrderDetail {
  id: string;
  orderNumber: string;
  store: CustomerOrderStoreInfo;
  status: OrderStatus;
  deliveryMode: DeliveryMode;
  serviceDate: string;
  serviceStartTime: string;
  serviceEndTime: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  deliveryAddress?: string | null;
  deliveryReference?: string | null;
  deliveryLatitude?: number | null;
  deliveryLongitude?: number | null;
  currency: string;
  productsSubtotalAmount: number;
  discountAmount: number;
  discountCode?: string | null;
  deliveryFeeAmount: number;
  includedTaxAmount: number;
  totalAmount: number;
  confirmedAt: string;
  updatedAt: string;
  items: CustomerOrderItem[];
  statusHistory: CustomerOrderStatusHistory[];
}

export interface FavoriteProductSummary {
  productId: string;
  productName: string;
  productSlug: string;
  productImageUrl?: string | null;
  storeId: string;
  storeName: string;
  storeSlug: string;
  minPrice: number;
  currency: string;
  isAvailable: boolean;
  addedAt: string;
}

export interface FavoriteStatusResponse {
  productId: string;
  isFavorite: boolean;
}

export interface CustomerReview {
  id: string;
  productId: string;
  productName: string;
  productSlug: string;
  storeName: string;
  storeSlug: string;
  imageUrl?: string | null;
  rating: number;
  comment?: string | null;
  status: 'PUBLISHED' | 'HIDDEN';
  createdAt: string;
  updatedAt: string;
}

export interface ReviewEligibility {
  productId: string;
  eligible: boolean;
  alreadyReviewed: boolean;
  currentReview?: CustomerReview | null;
}

export interface CreateReviewPayload {
  rating: number;
  comment?: string | null;
}

export interface UpdateReviewPayload {
  rating: number;
  comment?: string | null;
}

export interface CustomerAddress {
  id: string;
  alias: string;
  recipientName: string;
  recipientPhone: string;
  department: string;
  province: string;
  district: string;
  ubigeoCode: string;
  addressLine: string;
  reference?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerAddressListResponse {
  addresses: CustomerAddress[];
  total: number;
}

export interface CreateCustomerAddressPayload {
  alias: string;
  recipientName: string;
  recipientPhone: string;
  department: string;
  province: string;
  district: string;
  ubigeoCode: string;
  addressLine: string;
  reference?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  isDefault?: boolean;
}

export interface UpdateCustomerAddressPayload {
  alias: string;
  recipientName: string;
  recipientPhone: string;
  department: string;
  province: string;
  district: string;
  ubigeoCode: string;
  addressLine: string;
  reference?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  isDefault?: boolean;
}


