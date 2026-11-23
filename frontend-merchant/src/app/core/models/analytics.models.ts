/**
 * Contratos de datos analíticos y financieros para el panel de comerciantes.
 * Equivalente estricto a los DTOs en {@code com.jaldishop.backend.analytics.web.dto.*}.
 */

export interface PeriodResponse {
  from: string; // ISO YYYY-MM-DD
  to: string; // ISO YYYY-MM-DD
  timeZone: string;
}

export interface CommercialSummary {
  historicalConfirmedOrdersCount: number;
  historicalConfirmedGmv: number;
  activeOrdersCount: number;
  activeCommercialVolume: number;
  completedOrdersCount: number;
  productsGrossSales: number;
  totalDiscounts: number;
  totalDeliveryFees: number;
  cancelledOrdersCount: number;
  cancelledOrdersVolume: number;
  averageTicket: number;
}

export interface FinancialSummary {
  grossInflowAmount: number;
  approvedPaymentsCount: number;
  settledRefundsAmount: number;
  settledRefundsCount: number;
  netFinancialInflow: number;
  orderLinkedPaymentsAmount: number;
  orderLinkedPaymentsCount: number;
  orphanPaymentsAmount: number;
  orphanPaymentsCount: number;
  disclosure: string;
}

export interface ProductSalesRank {
  variantId: string;
  productId: string;
  productName: string;
  variantName: string;
  unitsSold: number;
  totalRevenue: number;
}

export interface CapacityDemandSummary {
  committedReservationsCount: number;
  releasedReservationsCount: number;
  notes: string | null;
}

export interface MerchantAnalyticsSummaryResponse {
  storeId: string;
  period: PeriodResponse;
  commercial: CommercialSummary;
  financial: FinancialSummary;
  topProducts: ProductSalesRank[];
  capacityDemand: CapacityDemandSummary;
}

export type AnalyticsDateRangePreset = '7D' | '30D' | '90D' | 'CUSTOM';
