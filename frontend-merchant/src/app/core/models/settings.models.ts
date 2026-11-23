export interface StorePoliciesSettings {
  cancellationPolicy: string;
  refundPolicy: string;
  requireCustomerDni: boolean;
  allowOrderNotes: boolean;
}

export interface StorePaymentsAndDeliverySettings {
  onlinePaymentsEnabled: boolean;
  cashOnDeliveryEnabled: boolean;
  acceptsPosOnDelivery: boolean;
  minDeliveryAmount: number | null;
  estimatedPrepTimeMinutes: number;
}

export interface StoreAlertsSettings {
  orderSoundAlert: boolean;
  soundVolume: number;
  lowStockVisualAlert: boolean;
}

export interface MerchantSettings {
  cancellationPolicy: string;
  refundPolicy: string;
  requireCustomerDni: boolean;
  allowOrderNotes: boolean;
  onlinePaymentsEnabled: boolean;
  cashOnDeliveryEnabled: boolean;
  acceptsPosOnDelivery: boolean;
  minDeliveryAmount: number | null;
  estimatedPrepTimeMinutes: number;
  orderSoundAlert: boolean;
  soundVolume: number;
  lowStockVisualAlert: boolean;
}
