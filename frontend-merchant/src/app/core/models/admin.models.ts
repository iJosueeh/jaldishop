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
}

export interface AdminStoreOwner {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  status: AdminUserStatus;
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
