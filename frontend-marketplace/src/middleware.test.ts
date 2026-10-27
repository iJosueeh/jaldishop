import { describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import { middleware, CUSTOMER_COOKIE } from './middleware';

describe('Customer route protection middleware', () => {
  it('redirects unauthenticated requests from /mi-jaldishop to /login preserving destination', () => {
    const request = new NextRequest('http://localhost:3000/mi-jaldishop/perfil?tab=contact');
    const response = middleware(request);

    expect(response.status).toBe(307);
    const location = response.headers.get('location');
    expect(location).toContain('/login?redirect=%2Fmi-jaldishop%2Fperfil%3Ftab%3Dcontact');
  });

  it('allows authenticated requests with customer cookie to access /mi-jaldishop', () => {
    const request = new NextRequest('http://localhost:3000/mi-jaldishop', {
      headers: {
        cookie: `${CUSTOMER_COOKIE}=valid-jwt-token`,
      },
    });
    const response = middleware(request);

    expect(response.status).toBe(200);
    expect(response.headers.get('location')).toBeNull();
  });

  it('ignores public routes and allows them through', () => {
    const request = new NextRequest('http://localhost:3000/');
    const response = middleware(request);

    expect(response.status).toBe(200);
    expect(response.headers.get('location')).toBeNull();
  });
});
