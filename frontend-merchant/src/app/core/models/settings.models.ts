export type WhatsAppTemplateId = 'in_preparation' | 'ready' | 'completed';

export interface WhatsAppTemplate {
  id: WhatsAppTemplateId;
  title: string;
  description: string;
  message: string;
  availableTags: string[];
}

export interface NotificationPreferences {
  orderSoundAlert: boolean;
  lowStockVisualAlert: boolean;
  dailyEmailSummary: boolean;
  soundVolume: number;
}

export interface OrderPreferences {
  minDeliveryAmount: number | null;
  postCheckoutMessage: string;
  estimatedPreparationTimeMinutes: number;
}

export interface PaymentMethodsSettings {
  mercadoPago: {
    enabled: boolean;
    sandboxMode: boolean;
    publicKey?: string;
  };
  yapePlin: {
    enabled: boolean;
    phoneNumber: string;
    accountHolder: string;
    qrImageUrl?: string;
    instructions: string;
  };
  bankTransfer: {
    enabled: boolean;
    bankName: string;
    accountNumber: string;
    cci: string;
    accountHolder: string;
  };
  cashOnDelivery: {
    enabled: boolean;
    acceptsCardsOnDelivery: boolean;
  };
}

export interface StorePoliciesSettings {
  cancellationPolicy: string;
  refundPolicy: string;
  requireCustomerDni: boolean;
  allowOrderNotes: boolean;
}

export interface GeneralPreferences {
  currency: string;
  currencySymbol: string;
  timezone: string;
  locale: string;
}

export interface MerchantSettings {
  whatsappTemplates: WhatsAppTemplate[];
  notifications: NotificationPreferences;
  orderPreferences: OrderPreferences;
  paymentMethods: PaymentMethodsSettings;
  policies: StorePoliciesSettings;
  general: GeneralPreferences;
}

