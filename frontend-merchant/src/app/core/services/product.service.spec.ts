import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Product, ProductCategory } from '../models/product.models';
import { environment } from '../../../environments/environment';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  const mockStoreId = 'store-123';
  const mockCategories: ProductCategory[] = [
    { id: 'cat-1', name: 'Tortas & Pasteles' },
    { id: 'cat-2', name: 'Galletas & Brownies' },
  ];

  const mockProducts: Product[] = [
    {
      id: 'prod-1',
      storeId: mockStoreId,
      categoryId: 'cat-1',
      name: 'Torta de Chocolate',
      slug: 'torta-chocolate',
      description: 'Deliciosa torta',
      status: 'ACTIVE',
      minPrice: 84,
    },
    {
      id: 'prod-2',
      storeId: mockStoreId,
      categoryId: 'cat-2',
      name: 'Brownies Melcochosos',
      slug: 'brownies-melcochosos',
      description: 'Caja x 8',
      status: 'ACTIVE',
      minPrice: 58,
    },
    {
      id: 'prod-3',
      storeId: mockStoreId,
      categoryId: 'cat-1',
      name: 'Cheesecake de Maracuyá',
      slug: 'cheesecake-maracuya',
      description: 'Sin stock de pulpa',
      status: 'INACTIVE',
      minPrice: 72,
      hasStockIssue: true,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProductService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load products from API on first call (Cache First)', () => {
    service.loadProducts(mockStoreId).subscribe((products) => {
      expect(products.length).toBe(3);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/products`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProducts);

    expect(service.products().length).toBe(3);
    expect(service.totalActiveCount()).toBe(2);
    expect(service.totalCount()).toBe(3);
  });

  it('should return cached products without extra HTTP call if loaded', () => {
    service.loadProducts(mockStoreId).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/products`);
    req.flush(mockProducts);

    // Second call without forceRefresh
    service.loadProducts(mockStoreId).subscribe((products) => {
      expect(products.length).toBe(3);
    });
    httpMock.expectNone(`${environment.apiUrl}/merchants/stores/${mockStoreId}/products`);
  });

  it('should filter products by category', () => {
    service.products.set(mockProducts);
    expect(service.filteredProducts().length).toBe(3);

    service.setCategoryFilter('cat-1');
    expect(service.filteredProducts().length).toBe(2);
    expect(service.filteredProducts().map((p) => p.id)).toEqual(['prod-1', 'prod-3']);

    service.setCategoryFilter(null);
    expect(service.filteredProducts().length).toBe(3);
  });

  it('should filter products by text query', () => {
    service.products.set(mockProducts);

    service.setSearchQuery('brownie');
    expect(service.filteredProducts().length).toBe(1);
    expect(service.filteredProducts()[0].id).toBe('prod-2');
  });

  it('should compute category counts properly', () => {
    service.categories.set(mockCategories);
    service.products.set(mockProducts);

    const counts = service.categoriesWithCounts();
    expect(counts.length).toBe(2);
    expect(counts.find((c) => c.id === 'cat-1')?.productCount).toBe(2);
    expect(counts.find((c) => c.id === 'cat-2')?.productCount).toBe(1);
  });

  it('should toggle product status', () => {
    service.products.set(mockProducts);
    const prod = mockProducts[0];

    service.toggleProductStatus(mockStoreId, prod).subscribe((updated) => {
      expect(updated.status).toBe('INACTIVE');
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/${prod.id}`,
    );
    expect(req.request.method).toBe('PUT');
    req.flush({ ...prod, status: 'INACTIVE' });

    expect(service.products().find((p) => p.id === 'prod-1')?.status).toBe('INACTIVE');
  });

  it('should compute isEmptyCatalog and isFilterEmpty accurately', () => {
    service.loadProducts(mockStoreId).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/products`);
    req.flush([]);

    expect(service.isEmptyCatalog()).toBe(true);
    expect(service.isFilterEmpty()).toBe(false);

    service.products.set(mockProducts);
    expect(service.isEmptyCatalog()).toBe(false);

    service.setSearchQuery('non-existent-product');
    expect(service.isFilterEmpty()).toBe(true);
  });

  it('should clear cache on clearCache()', () => {
    service.products.set(mockProducts);
    service.categories.set(mockCategories);
    service.setSearchQuery('test');

    service.clearCache();

    expect(service.products().length).toBe(0);
    expect(service.categories().length).toBe(0);
    expect(service.searchQuery()).toBe('');
  });
});
