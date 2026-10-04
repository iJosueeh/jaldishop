import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  CreateCategoryRequest,
  CreateProductRequest,
  CreateProductVariantRequest,
  CreateProductWithVariantPayload,
  Product,
  ProductCategory,
  ProductVariant,
  ProductViewMode,
  UpdateProductRequest,
  UpdateProductVariantRequest,
} from '../models/product.models';
import { catchError, forkJoin, map, Observable, of, switchMap, tap, throwError } from 'rxjs';

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

  /**
   * Crea una nueva categoría para la tienda y actualiza la señal reactiva categories.
   */
  createCategory(storeId: string, request: CreateCategoryRequest): Observable<ProductCategory> {
    return this.http
      .post<ProductCategory>(`${this.baseUrl}/${storeId}/categories`, request)
      .pipe(
        tap((newCategory) => {
          this.categories.update((cats) => [...cats, newCategory]);
        }),
      );
  }

  /**
   * Crea un producto base en la tienda.
   */
  createProduct(storeId: string, request: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/${storeId}/products`, request);
  }

  /**
   * Obtiene la lista de variantes (formatos y precios) de un producto.
   */
  getProductVariants(storeId: string, productId: string): Observable<ProductVariant[]> {
    return this.http.get<ProductVariant[]>(
      `${this.baseUrl}/${storeId}/products/${productId}/variants`,
    );
  }

  /**
   * Crea una variante para un producto existente.
   */
  createProductVariant(
    storeId: string,
    productId: string,
    request: CreateProductVariantRequest,
  ): Observable<ProductVariant> {
    return this.http.post<ProductVariant>(
      `${this.baseUrl}/${storeId}/products/${productId}/variants`,
      request,
    );
  }

  /**
   * Actualiza una variante existente de un producto.
   */
  updateProductVariant(
    storeId: string,
    productId: string,
    variantId: string,
    request: UpdateProductVariantRequest,
  ): Observable<ProductVariant> {
    return this.http.put<ProductVariant>(
      `${this.baseUrl}/${storeId}/products/${productId}/variants/${variantId}`,
      request,
    );
  }

  /**
   * Recalcula y actualiza los rangos de precio (minPrice, maxPrice) y variantes de un producto en el estado reactivo.
   */
  syncProductVariants(productId: string, variants: ProductVariant[]): void {
    const activeVariants = variants.filter((v) => v.status === 'ACTIVE');
    const prices = (activeVariants.length > 0 ? activeVariants : variants).map((v) => v.priceAmount);
    const minPrice = prices.length > 0 ? Math.min(...prices) : undefined;
    const maxPrice = prices.length > 0 ? Math.max(...prices) : undefined;

    this.products.update((current) =>
      current.map((p) => (p.id === productId ? { ...p, variants, minPrice, maxPrice } : p)),
    );
  }

  /**
   * Orquesta la creación atómica de un producto y su variante estándar:
   * 1. Resuelve la categoría (si se ingresó una nueva, la crea; de lo contrario usa la existente).
   * 2. Registra el producto base.
   * 3. Registra la variante estándar con su precio y control operativo.
   * 4. Ensambla y actualiza la lista reactiva de productos sin requerir recargar la página.
   */
  createProductWithDefaultVariant(
    storeId: string,
    payload: CreateProductWithVariantPayload,
  ): Observable<Product> {
    this.isLoading.set(true);

    const resolveCategory$: Observable<string> =
      payload.newCategoryName && !payload.categoryId
        ? this.createCategory(storeId, { name: payload.newCategoryName.trim() }).pipe(
            map((cat) => cat.id),
          )
        : payload.categoryId
          ? of(payload.categoryId)
          : throwError(() => new Error('Debes seleccionar o crear una categoría para el producto.'));

    return resolveCategory$.pipe(
      switchMap((categoryId) => {
        const createProductPayload: CreateProductRequest = {
          categoryId,
          name: payload.name.trim(),
          slug: payload.slug?.trim() || undefined,
          description: payload.description?.trim() || undefined,
          imageUrl: payload.imageUrl || undefined,
        };
        return this.createProduct(storeId, createProductPayload);
      }),
      switchMap((createdProduct) => {
        const variantRequests: CreateProductVariantRequest[] =
          payload.variants && payload.variants.length > 0
            ? payload.variants.map((v) => ({
                presentationName: v.presentationName.trim(),
                sku: v.sku?.trim() || undefined,
                priceAmount: Number(v.priceAmount),
                priceCurrency: v.priceCurrency?.trim() || 'PEN',
                tracksInventory: !!v.tracksInventory,
                attributes: v.attributes || [],
              }))
            : [
                {
                  presentationName: payload.presentationName?.trim() || 'Unidad Estándar',
                  sku: payload.sku?.trim() || undefined,
                  priceAmount: Number(payload.priceAmount || 0),
                  priceCurrency: payload.priceCurrency?.trim() || 'PEN',
                  tracksInventory: !!payload.tracksInventory,
                  attributes: payload.attributes || [],
                },
              ];

        const variantObservables = variantRequests.map((req) =>
          this.createProductVariant(storeId, createdProduct.id, req),
        );

        return forkJoin(variantObservables).pipe(
          map((createdVariants) => {
            const categoryObj = this.categories().find((c) => c.id === createdProduct.categoryId);
            const prices = createdVariants.map((v) => v.priceAmount);
            const minPrice = prices.length > 0 ? Math.min(...prices) : undefined;
            const maxPrice = prices.length > 0 ? Math.max(...prices) : undefined;

            const fullProduct: Product = {
              ...createdProduct,
              categoryName: categoryObj?.name || payload.newCategoryName || 'General',
              minPrice,
              maxPrice,
              variants: createdVariants,
              status: (createdProduct.status as any) || 'ACTIVE',
            };

            this.products.update((current) => [fullProduct, ...current]);
            this.isLoaded.set(true);
            this.isLoading.set(false);
            return fullProduct;
          }),
        );
      }),
      catchError((error) => {
        this.isLoading.set(false);
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
