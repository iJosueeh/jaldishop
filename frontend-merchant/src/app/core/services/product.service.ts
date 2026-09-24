import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Product,
  ProductCategory,
  ProductViewMode,
  UpdateProductRequest,
} from '../models/product.models';
import { catchError, Observable, of, tap, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/merchants/stores`;

  private readonly isLoaded = signal<boolean>(false);
  private readonly isCategoriesLoaded = signal<boolean>(false);

  readonly products = signal<Product[]>([]);
  readonly categories = signal<ProductCategory[]>([]);
  readonly isLoading = signal<boolean>(false);

  // Filtros y Vista
  readonly selectedCategoryId = signal<string | null>(null);
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<string>('all');
  readonly viewMode = signal<ProductViewMode>('grid');
  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(6);

  // Computados de Filtrado
  readonly filteredProducts = computed(() => {
    const list = this.products();
    const catId = this.selectedCategoryId();
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((p) => {
      // Filtro por categoría
      if (catId !== null && p.categoryId !== catId) {
        return false;
      }

      // Filtro por estado
      if (status !== 'all' && p.status !== status) {
        return false;
      }

      // Filtro por texto de búsqueda
      if (query.length > 0) {
        const nameMatch = p.name.toLowerCase().includes(query);
        const descMatch = p.description ? p.description.toLowerCase().includes(query) : false;
        return nameMatch || descMatch;
      }

      return true;
    });
  });

  // Métricas y Computados Auxiliares
  readonly totalActiveCount = computed(() => {
    return this.products().filter((p) => p.status === 'ACTIVE').length;
  });

  readonly categoriesWithCounts = computed(() => {
    const cats = this.categories();
    const prods = this.products();

    return cats.map((cat) => ({
      ...cat,
      productCount: prods.filter((p) => p.categoryId === cat.id).length,
    }));
  });

  readonly totalCount = computed(() => this.products().length);

  readonly pagedProducts = computed(() => {
    const filtered = this.filteredProducts();
    const page = this.currentPage();
    const size = this.pageSize();
    const start = (page - 1) * size;
    return filtered.slice(start, start + size);
  });

  readonly attentionRequiredProducts = computed(() => {
    return this.products().filter((p) => p.status === 'INACTIVE' || p.hasStockIssue);
  });

  readonly hasActiveFilters = computed(() => {
    return (
      this.selectedCategoryId() !== null ||
      this.searchQuery().trim().length > 0 ||
      this.statusFilter() !== 'all'
    );
  });

  readonly isEmptyCatalog = computed(() => {
    return !this.isLoading() && this.isLoaded() && this.products().length === 0;
  });

  readonly isFilterEmpty = computed(() => {
    return (
      !this.isLoading() &&
      this.isLoaded() &&
      this.products().length > 0 &&
      this.filteredProducts().length === 0
    );
  });

  /**
   * Carga los productos de la tienda aplicando estrategia Cache First.
   * Si ya fueron cargados previamente y forceRefresh es false, retorna inmediatamente el estado en memoria.
   */
  loadProducts(storeId: string, forceRefresh = false): Observable<Product[]> {
    if (this.isLoaded() && !forceRefresh) {
      return of(this.products());
    }

    this.isLoading.set(true);

    return this.http.get<Product[]>(`${this.baseUrl}/${storeId}/products`).pipe(
      tap((data) => {
        this.products.set(data || []);
        this.isLoaded.set(true);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Carga las categorías de la tienda aplicando estrategia Cache First.
   */
  loadCategories(storeId: string, forceRefresh = false): Observable<ProductCategory[]> {
    if (this.isCategoriesLoaded() && !forceRefresh) {
      return of(this.categories());
    }

    return this.http.get<ProductCategory[]>(`${this.baseUrl}/${storeId}/categories`).pipe(
      tap((data) => {
        this.categories.set(data || []);
        this.isCategoriesLoaded.set(true);
      }),
      catchError((error) => {
        return throwError(() => error);
      }),
    );
  }

  updateProduct(
    storeId: string,
    productId: string,
    request: UpdateProductRequest,
  ): Observable<Product> {
    this.isLoading.set(true);
    return this.http.put<Product>(`${this.baseUrl}/${storeId}/products/${productId}`, request).pipe(
      tap((updated) => {
        const currentList = this.products();
        this.products.set(currentList.map((p) => (p.id === productId ? { ...p, ...updated } : p)));
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  toggleProductStatus(storeId: string, product: Product): Observable<Product> {
    const newStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    return this.updateProduct(storeId, product.id, {
      name: product.name,
      slug: product.slug,
      description: product.description,
      imageUrl: product.imageUrl,
      categoryId: product.categoryId,
      status: newStatus,
    });
  }

  setCategoryFilter(categoryId: string | null): void {
    this.selectedCategoryId.set(categoryId);
    this.currentPage.set(1);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(1);
  }

  setStatusFilter(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(1);
  }

  setViewMode(mode: ProductViewMode): void {
    this.viewMode.set(mode);
  }

  setPage(page: number): void {
    this.currentPage.set(page);
  }

  clearFilters(): void {
    this.selectedCategoryId.set(null);
    this.searchQuery.set('');
    this.statusFilter.set('all');
    this.currentPage.set(1);
  }

  clearCache(): void {
    this.products.set([]);
    this.categories.set([]);
    this.isLoaded.set(false);
    this.isCategoriesLoaded.set(false);
    this.clearFilters();
  }
}
