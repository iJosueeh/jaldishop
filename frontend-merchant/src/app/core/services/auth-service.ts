import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { TokenService } from './token-service';
import { Router } from '@angular/router';
import { AuthResult, LoginRequest, RegisterMerchantRequest } from '../models/auth.models';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CapacityService } from './capacity.service';
import { StoreService } from './store.service';
import { ProfileService } from './profile.service';
import { ProductService } from './product.service';
import { CustomerService } from './customer.service';
import { OrderService } from './order.service';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly router = inject(Router);
  private readonly capacityService = inject(CapacityService);
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);
  private readonly productService = inject(ProductService);
  private readonly customerService = inject(CustomerService);
  private readonly orderService = inject(OrderService);

  readonly token = signal<string | null>(this.tokenService.getToken());
  readonly currentUser = signal<AuthResult | null>(null);

  readonly isAuthenticated = computed(() => !!this.token() && this.tokenService.hasValidToken());
  readonly userRoles = computed(() => (this.isAuthenticated() ? this.tokenService.getRoles() : []));
  readonly isMerchant = computed(() => this.userRoles().includes('MERCHANT'));
  readonly isAdmin = computed(() => this.userRoles().includes('ADMIN'));

  login(credentials: LoginRequest): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap((result) => {
        this.tokenService.setToken(result.token);
        const tokenRoles = this.tokenService.getRoles();

        if (!tokenRoles.includes('MERCHANT') && !tokenRoles.includes('ADMIN')) {
          this.logout();
          throw new Error(
            'Acceso denegado: Este panel es exclusivo para comerciantes y administradores.',
          );
        }

        this.token.set(result.token);
        this.currentUser.set(result);
      }),
    );
  }

  registerMerchant(request: RegisterMerchantRequest): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${environment.apiUrl}/auth/register/merchant`, request).pipe(
      tap((result) => {
        this.tokenService.setToken(result.token);
        this.token.set(result.token);
        this.currentUser.set(result);
      }),
    );
  }

  logout(): void {
    this.tokenService.removeToken();
    this.token.set(null);
    this.currentUser.set(null);

    this.capacityService.clearCache();
    this.storeService.clearStore();
    this.profileService.clearProfile();
    this.productService.clearCache();
    this.customerService.clearCache();
    this.orderService.clearOrders();

    this.router.navigate(['/login']);
  }
}
