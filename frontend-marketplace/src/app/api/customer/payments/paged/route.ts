import { NextRequest } from 'next/server';
import { customerProxy } from '@/core/api/customerProxy';

export async function GET(request: NextRequest) {
  const qs = request.nextUrl.searchParams.toString();
  const path = qs ? `/customer/payments/paged?${qs}` : '/customer/payments/paged';
  return customerProxy(request, path);
}
