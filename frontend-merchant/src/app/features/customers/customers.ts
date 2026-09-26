import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { CustomerFilterTab, StoreCustomer } from '../../core/models/customer.models';
import { CustomersHeader } from './components/customers-header/customers-header';
import { CustomersKpis } from './components/customers-kpis/customers-kpis';
import { CustomersFilterBar } from './components/customers-filter-bar/customers-filter-bar';
import { CustomersTable } from './components/customers-table/customers-table';
import { Pagination } from '../../shared/components/pagination/pagination';

@Component({
  selector: 'app-customers',
  imports: [
    CommonModule,
    CustomersHeader,
    CustomersKpis,
    CustomersFilterBar,
    CustomersTable,
    Pagination,
  ],
  templateUrl: './customers.html',
  styleUrl: './customers.css',
})
export class Customers implements OnInit {
  readonly customerService = inject(CustomerService);
  private readonly toastService = inject(ToastService);

  readonly activeTab = computed(() => this.customerService.activeFilter());
  readonly searchQuery = computed(() => this.customerService.searchQuery());
  readonly isLoading = computed(() => this.customerService.isLoading());
  readonly kpis = computed(() => this.customerService.kpis());

  readonly rawCustomers = computed(() => this.customerService.customers());
  readonly filteredCustomers = computed(() => this.customerService.filteredCustomers());

  readonly totalCount = computed(() => this.rawCustomers().length);
  readonly vipCount = computed(
    () => this.rawCustomers().filter((c) => c.ordersCount >= 3).length,
  );
  readonly frequentCount = computed(
    () => this.rawCustomers().filter((c) => c.ordersCount === 2).length,
  );
  readonly newCount = computed(
    () => this.rawCustomers().filter((c) => c.ordersCount <= 1).length,
  );

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  readonly paginatedCustomers = computed(() => {
    const list = this.filteredCustomers();
    const start = (this.currentPage() - 1) * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.customerService.loadCustomers().subscribe({
      error: () => {
        this.toastService.error('Error al cargar la lista de clientes.');
      },
    });
  }

  onTabChange(tab: CustomerFilterTab): void {
    this.customerService.setFilter(tab);
    this.currentPage.set(1);
  }

  onSearchChange(query: string): void {
    this.customerService.setSearchQuery(query);
    this.currentPage.set(1);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onOpenWhatsApp(customer: StoreCustomer): void {
    this.toastService.info(`Iniciando chat con ${customer.firstName}...`);
  }
}
