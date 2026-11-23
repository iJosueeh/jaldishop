import type { CustomerPaymentHistory } from '../types/customer.types';

export async function getCustomerPaymentHistory(): Promise<CustomerPaymentHistory[]> {
  const response = await fetch('/api/customer/payments', { cache: 'no-store' });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || 'No pudimos consultar tus pagos.');
  }
  return response.json();
}
