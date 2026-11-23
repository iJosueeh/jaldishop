export type PayoutChannel = 'BANK_TRANSFER_CCI' | 'YAPE_PHONE' | 'PLIN_PHONE';

export type PayoutBatchStatus = 'PENDING' | 'PROCESSING' | 'PAID' | 'FAILED' | 'CANCELLED';

export interface MerchantPayoutDestinationResponse {
  id: string;
  storeId: string;
  payoutChannel: PayoutChannel;
  bankName?: string;
  accountNumber?: string;
  accountCci?: string;
  phoneNumber?: string;
  accountHolderName: string;
  accountHolderDocType: 'DNI' | 'RUC' | 'CE';
  accountHolderDocNumber: string;
  isPrimary: boolean;
  isVerified: boolean;
  updatedAt: string;
}

export interface MerchantBillingSummaryResponse {
  storeId: string;
  availableBalance: number;
  lockedBalance: number;
  totalPaidOut: number;
  minPayoutThreshold: number;
  currency: string;
  payoutDestination: MerchantPayoutDestinationResponse | null;
  asOf: string;
}

export interface MerchantPayoutBatchResponse {
  id: string;
  storeId: string;
  status: PayoutBatchStatus;
  grossAmount: number;
  bankingFee: number;
  netDisbursedAmount: number;
  currency: string;
  bankOperationNumber?: string;
  scheduledFor: string;
  processedAt?: string;
  createdAt: string;
}

export interface UpdatePayoutDestinationRequest {
  payoutChannel: PayoutChannel;
  bankName?: string;
  accountNumber?: string;
  accountCci?: string;
  phoneNumber?: string;
  accountHolderName: string;
  accountHolderDocType: 'DNI' | 'RUC' | 'CE';
  accountHolderDocNumber: string;
}

export interface OrderPaymentBreakdownResponse {
  subtotalAmount: number;
  deliveryFee: number;
  platformFee: number;
  merchantCommissionAmount: number;
  merchantNetAmount: number;
  totalPaidByCustomer: number;
}

export interface RequestPayoutResponse {
  batchId: string;
  status: string;
  amount: number;
  currency: string;
  message: string;
}
