import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CustomerFilterTab, CustomerKpis, StoreCustomer } from '../models/customer.models';
import { catchError, Observable, of, tap, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/merchant/customers`;

  private readonly isLoaded = signal<boolean>(false);
  readonly customers = signal<StoreCustomer[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly searchQuery = signal<string>('');
  readonly activeFilter = signal<CustomerFilterTab>('all');

  readonly filteredCustomers = computed(() => {
    const list = this.customers();
    const filter = this.activeFilter();
    const query = this.searchQuery().trim().toLowerCase();

    return list.filter((c) => {
      // 1. Filtro por pestaña de segmento
      let matchesTab = true;
      if (filter === 'vip') {
        matchesTab = c.ordersCount >= 3;
      } else if (filter === 'frequent') {
        matchesTab = c.ordersCount === 2;
      } else if (filter === 'new') {
        matchesTab = c.ordersCount <= 1;
      }

      if (!matchesTab) {
        return false;
      }

      // 2. Filtro por búsqueda textual
      if (query.length > 0) {
        const fullName = `${c.firstName} ${c.lastName}`.toLowerCase();
        const email = c.email.toLowerCase();
        const phone = c.phone ? c.phone.toLowerCase() : '';
        return fullName.includes(query) || email.includes(query) || phone.includes(query);
      }

      return true;
    });
  });

  readonly kpis = computed<CustomerKpis>(() => {
    const list = this.customers();
    const totalCustomers = list.length;
    if (totalCustomers === 0) {
      return {
        totalCustomers: 0,
        repeatCustomersCount: 0,
        repeatPercentage: 0,
        averageTicketAmount: 0,
        totalSpentOverall: 0,
      };
    }

    const repeatCustomersCount = list.filter((c) => c.ordersCount >= 2).length;
    const repeatPercentage = Math.round((repeatCustomersCount / totalCustomers) * 100);

    const totalSpentOverall = list.reduce((acc, c) => acc + (c.totalSpentAmount || 0), 0);
    const totalOrdersOverall = list.reduce((acc, c) => acc + (c.ordersCount || 0), 0);
    const averageTicketAmount =
      totalOrdersOverall > 0 ? Number((totalSpentOverall / totalOrdersOverall).toFixed(2)) : 0;

    return {
      totalCustomers,
      repeatCustomersCount,
      repeatPercentage,
      averageTicketAmount,
      totalSpentOverall: Number(totalSpentOverall.toFixed(2)),
    };
  });

  loadCustomers(forceRefresh = false, query?: string): Observable<StoreCustomer[]> {
    if (this.isLoaded() && !forceRefresh && (!query || query.trim().length === 0)) {
      return of(this.customers());
    }

    this.isLoading.set(true);

    let params = new HttpParams();
    if (query && query.trim().length > 0) {
      params = params.set('query', query.trim());
    }

    return this.http.get<StoreCustomer[]>(this.baseUrl, { params }).pipe(
      tap((data) => {
        this.customers.set(data || []);
        if (!query || query.trim().length === 0) {
          this.isLoaded.set(true);
        }
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  setFilter(filter: CustomerFilterTab): void {
    this.activeFilter.set(filter);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  clearCache(): void {
    this.customers.set([]);
    this.isLoaded.set(false);
    this.searchQuery.set('');
    this.activeFilter.set('all');
  }
}
