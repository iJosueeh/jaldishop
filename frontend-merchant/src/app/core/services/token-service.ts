import { Service } from '@angular/core';

@Service()
export class TokenService {
    private readonly TOKEN_KEY = 'jaldi_merchant_token';


    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    setToken(token: string): void {
        localStorage.setItem(this.TOKEN_KEY, token);
    }

    removeToken(): void {
        localStorage.removeItem(this.TOKEN_KEY);
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

    getRoles(): string[] {
        const payload = this.getPayload();
        return payload?.roles || [];
    }

    getUserId(): string | null {
        const payload = this.getPayload();
        return payload?.sub || payload?.userId || null;
    }
    
}
