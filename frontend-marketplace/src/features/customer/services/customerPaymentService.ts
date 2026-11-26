import type { CustomerPageResponse, CustomerPaymentHistory } from '../types/customer.types';

export interface CustomerPaymentPageParams {
  page?: number;
  size?: number;
  filter?: string;
  q?: string;
}

export async function getCustomerPaymentHistory(): Promise<CustomerPaymentHistory[]> {
  const response = await fetch('/api/customer/payments', { cache: 'no-store' });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'No pudimos consultar tus pagos.');
  }
  return response.json();
}

export async function getCustomerPaymentHistoryPaged(
  params: CustomerPaymentPageParams = {}
): Promise<CustomerPageResponse<CustomerPaymentHistory>> {
  const sp = new URLSearchParams();
  if (typeof params.page === 'number') sp.set('page', String(params.page));
  if (typeof params.size === 'number') sp.set('size', String(params.size));
  if (params.filter) sp.set('filter', params.filter);
  if (params.q && params.q.trim()) sp.set('q', params.q.trim());

  const qs = sp.toString();
  const url = qs ? `/api/customer/payments/paged?${qs}` : '/api/customer/payments/paged';
  const response = await fetch(url, { cache: 'no-store' });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'No pudimos consultar tus pagos.');
  }
  return response.json();
}
