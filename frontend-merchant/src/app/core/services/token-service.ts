import { Service } from '@angular/core';

@Service()
export class TokenService {
  private readonly TOKEN_KEY = 'jaldi_merchant_token';

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  removeToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  getPayload(): any | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      return JSON.parse(atob(parts[1]));
    } catch (error) {
      return null;
    }
  }

  isTokenExpired(): boolean {
    const payload = this.getPayload();
    if (!payload || !payload.exp) return true;

    return Date.now() >= payload.exp * 1000;
  }

  hasValidToken(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired();
  }

  getRoles(): string[] {
    const payload = this.getPayload();
    return payload?.roles || [];
  }

  getUserId(): string | null {
    const payload = this.getPayload();
    return payload?.sub || payload?.userId || null;
  }
}
