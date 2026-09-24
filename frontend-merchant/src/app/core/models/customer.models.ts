export interface StoreCustomer {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  customerSince: string | null;
  ordersCount: number;
  totalSpentAmount: number;
  lastOrderAt: string | null;
}

export type CustomerFilterTab = 'all' | 'vip' | 'frequent' | 'new';

export interface CustomerKpis {
  totalCustomers: number;
  repeatCustomersCount: number;
  repeatPercentage: number;
  averageTicketAmount: number;
  totalSpentOverall: number;
}
