import { NextRequest } from 'next/server';
import { customerProxy } from '@/core/api/customerProxy';

export async function GET(request: NextRequest) {
  return customerProxy(request, '/customer/payments');
}
