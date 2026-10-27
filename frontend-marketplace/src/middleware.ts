import { NextRequest, NextResponse } from 'next/server';

export const CUSTOMER_COOKIE = 'jaldishop_customer_token';

/**
 * Edge middleware that protects customer-only personal routes (/mi-jaldishop/*).
 * If the customer is not authenticated via HttpOnly cookie, redirects to /login
 * preserving the target URL in the redirect parameter.
 */
export function middleware(request: NextRequest) {
  const token = request.cookies.get(CUSTOMER_COOKIE)?.value;
  const { pathname, search } = request.nextUrl;

  if (pathname.startsWith('/mi-jaldishop')) {
    if (!token) {
      const loginUrl = new URL('/login', request.url);
      loginUrl.searchParams.set('redirect', `${pathname}${search}`);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/mi-jaldishop/:path*'],
};
