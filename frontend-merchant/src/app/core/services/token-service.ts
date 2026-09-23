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

      let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      while (base64.length % 4 !== 0) {
        base64 += '=';
      }

      const decoded = atob(base64);
      const bytes = new Uint8Array(decoded.length);
      for (let i = 0; i < decoded.length; i++) {
        bytes[i] = decoded.charCodeAt(i);
      }

      const text = new TextDecoder('utf-8').decode(bytes);
      return JSON.parse(text);
    } catch (error) {
      return null;
    }
  }

  isTokenExpired(): boolean {
    const payload = this.getPayload();
    if (!payload || !payload.exp) return false;

    return Date.now() >= payload.exp * 1000;
  }

  hasValidToken(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired();
  }

  getRoles(): string[] {
    const payload = this.getPayload();
    if (!payload) return [];

    const rawRoles = payload.roles || payload.authorities || payload.role || [];
    if (Array.isArray(rawRoles)) {
      return rawRoles.map((r: any) => {
        const name = typeof r === 'string' ? r : r?.authority || r?.name || '';
        return name.replace(/^ROLE_/, '').trim().toUpperCase();
      });
    }

    if (typeof rawRoles === 'string') {
      return [rawRoles.replace(/^ROLE_/, '').trim().toUpperCase()];
    }

    return [];
  }

  getUserId(): string | null {
    const payload = this.getPayload();
    return payload?.sub || payload?.userId || null;
  }
}
