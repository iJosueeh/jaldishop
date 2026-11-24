export type AdminUserRole = 'ADMIN' | 'MERCHANT' | 'CUSTOMER';
export type AdminUserStatus = 'ACTIVE' | 'SUSPENDED';
export type AdminStoreStatus = 'ACTIVE' | 'SUSPENDED';

export interface AdminUserSummary {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  status: AdminUserStatus;
  roles: AdminUserRole[];
  createdAt: string;
  updatedAt: string;
  storeId?: string;
  storeName?: string;
  primaryAdmin?: boolean;
}

export interface AdminStoreOwner {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  status: AdminUserStatus;
}

export interface PayoutDestinationInfo {
  id?: string;
  payoutChannel: 'BANK_ACCOUNT' | 'YAPE' | 'PLIN' | string;
  bankName?: string;
  accountNumber?: string;
  accountCci?: string;
  phoneNumber?: string;
  accountHolderName?: string;
  accountHolderDocType?: string;
  accountHolderDocNumber?: string;
  isPrimary?: boolean;
  isVerified?: boolean;
}

export interface AdminStoreSummary {
  id: string;
  merchantUserId: string;
  name: string;
  slug: string;
  contactPhone?: string;
  status: AdminStoreStatus;
  deliveryEnabled: boolean;
  pickupEnabled: boolean;
  createdAt: string;
  updatedAt: string;
  merchant: AdminStoreOwner;
  legalBusinessName?: string;
  taxIdType?: 'DNI' | 'RUC_10' | 'RUC_20';
  taxIdNumber?: string;
  taxRegime?: 'RUS' | 'ESPECIAL' | 'MYPE' | 'GENERAL' | 'NONE';
  canIssueInvoices?: boolean;
  payoutDestination?: PayoutDestinationInfo;
}

export interface AdminStoreDetail extends AdminStoreSummary {
  description?: string;
  address?: string;
  addressReference?: string;
  latitude?: number;
  longitude?: number;
  deliveryFeeAmount?: number;
  deliveryFeeCurrency?: string;
  taxRate?: number;
  logoUrl?: string;
  bannerUrl?: string;
  instagramUrl?: string;
  facebookUrl?: string;
  whatsappNumber?: string;
}

export interface AdminDashboardMetrics {
  totalUsers: number;
  totalMerchants: number;
  totalCustomers: number;
  totalAdmins: number;
  activeUsers: number;
  suspendedUsers: number;
  totalStores: number;
  activeStores: number;
  suspendedStores: number;
}

export interface AdminPlatformSettings {
  allowMerchantRegistration: boolean;
  requireStoreApproval: boolean;
  maintenanceMode: boolean;
  maintenanceNotice: string;
  marketplaceCommissionRate: number;
  version?: number;
  updatedAt?: string;
}

// Analíticas y Finanzas Globales
export interface CommercialAnalyticsSummary {
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

export interface FinancialAnalyticsSummary {
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

export interface StorePerformanceRank {
  storeId: string;
  storeName: string;
  confirmedOrdersCount?: number;
  commercialGmv?: number;
  grossInflowAmount?: number;
  settledRefundsAmount?: number;
  netFinancialInflow?: number;
  // Campos heredados o alternativos para compatibilidad
  ordersCount?: number;
  totalGmv?: number;
  averageTicket?: number;
}

export interface CapacityDemandSummary {
  totalActiveStores: number;
  storesAtPeakCapacity: number;
  avgOrdersPerActiveStore: number;
  demandSaturationRate: number;
}

export interface AdminAnalyticsSummary {
  period: { from: string; to: string; periodType?: string };
  filterStoreId?: string;
  commercial: CommercialAnalyticsSummary;
  financial: FinancialAnalyticsSummary;
  storeRankings: StorePerformanceRank[];
  capacityDemand?: CapacityDemandSummary;
}

// Pistas de Auditoría
export interface AdminAuditLog {
  id: string;
  actorId: string;
  actorEmail: string;
  action: string;
  targetType: string;
  targetId: string;
  status: 'SUCCESS' | 'FAILURE' | 'DENIED';
  ipAddress: string;
  details?: Record<string, any> | string;
  createdAt: string;
}

export interface AdminAuditLogPage {
  content: AdminAuditLog[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Gobierno de Administradores
export type AdminInvitationStatus = 'PENDING' | 'ACCEPTED' | 'REVOKED' | 'EXPIRED';

export interface AdminInvitation {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  status: AdminInvitationStatus;
  expiresAt: string;
  createdAt: string;
}

export interface InviteAdminRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

// Investigación y Conciliación de Pagos
export interface OrderInvestigationResult {
  attemptId: string;
  status?: string;
  providerStatus?: string;
  amount?: number;
  currency?: string;
  detail?: string;
  createdAt?: string;
  providerPaymentId?: string;
  outcome?: 'VERIFIED' | 'INCONCLUSIVE' | 'CONTRADICTION' | 'TECHNICAL_ERROR' | string;
  reason?: string;
}

export interface OrderReconciliationResult {
  attemptId: string;
  paymentId?: string;
  providerOrderId?: string;
  status: string;
  detail: string;
  financialStatus?: string;
  commercialStatus?: string;
  orderId?: string;
  orderNumber?: number;
}

export interface ReconciliationCandidateItem {
  attemptId: string;
  paymentId: string;
  providerOrderId?: string;
  providerPaymentId?: string;
  amount: number;
  currency: string;
  paymentMethod?: string;
  storeName: string;
  customerEmail: string;
  customerName?: string;
  approvedAt?: string;
  incidenceType: 'ORPHAN_APPROVED' | 'CREATION_TIMEOUT' | string;
}
