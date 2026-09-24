import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Products } from './products';
import { ProductService } from '../../core/services/product.service';
import { StoreService } from '../../core/services/store.service';
import { ToastService } from '../../core/services/toast.service';
import { environment } from '../../../environments/environment';
import { Product, ProductCategory } from '../../core/models/product.models';
import { vi } from 'vitest';

describe('Products', () => {
  let component: Products;
  let fixture: ComponentFixture<Products>;
  let productService: ProductService;
  let storeService: StoreService;
  let toastService: ToastService;
  let httpMock: HttpTestingController;

  const mockCategories: ProductCategory[] = [
    { id: 'cat-1', name: 'Tortas & Pasteles' },
    { id: 'cat-2', name: 'Galletas & Brownies' },
  ];

  const mockProducts: Product[] = [
    {
      id: 'prod-1',
      storeId: 'demo-store',
      categoryId: 'cat-1',
      name: 'Torta Chocolate Tradicional',
      slug: 'torta-chocolate',
      description: 'Deliciosa',
      status: 'ACTIVE',
      minPrice: 84,
    },
    {
      id: 'prod-2',
      storeId: 'demo-store',
      categoryId: 'cat-2',
      name: 'Brownies Melcochosos',
      slug: 'brownies-melcochosos',
      description: 'Caja x 8',
      status: 'ACTIVE',
      minPrice: 58,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Products],
      providers: [
        ProductService,
        StoreService,
        ToastService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    productService = TestBed.inject(ProductService);
    storeService = TestBed.inject(StoreService);
    toastService = TestBed.inject(ToastService);
    httpMock = TestBed.inject(HttpTestingController);

    // Pre-establecer tienda actual para evitar llamada asíncrona a stores/me
    storeService.currentStore.set({
      id: 'demo-store',
      name: 'Dulces Clara',
      slug: 'dulces-clara',
      status: 'ACTIVE',
    } as any);

    fixture = TestBed.createComponent(Products);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initComponentWithData() {
    fixture.detectChanges();

    const reqCat = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/demo-store/categories`,
    );
    reqCat.flush(mockCategories);

    const reqProd = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/demo-store/products`,
    );
    reqProd.flush(mockProducts);

    fixture.detectChanges();
  }

  it('should create and load catalog products and categories', () => {
    initComponentWithData();
    expect(component).toBeTruthy();
    expect(productService.products().length).toBe(2);
    expect(productService.categories().length).toBe(2);
  });

  it('should toggle product status and show toast', () => {
    initComponentWithData();
    const toastSpy = vi.spyOn(toastService, 'success');
    const product = productService.products()[0];

    component.onToggleStatus(product);

    const toggleReq = httpMock.expectOne(
      `${environment.apiUrl}/merchants/stores/demo-store/products/${product.id}`,
    );
    expect(toggleReq.request.method).toBe('PUT');
    toggleReq.flush({ ...product, status: 'INACTIVE' });

    expect(toastSpy).toHaveBeenCalled();
  });

  it('should update filter when onCategoryChange is called', () => {
    initComponentWithData();
    component.onCategoryChange('cat-1');
    expect(productService.selectedCategoryId()).toBe('cat-1');
  });

  it('should clear filters when onClearFilters is called', () => {
    initComponentWithData();
    component.onCategoryChange('cat-1');
    component.onSearchChange('brownie');

    component.onClearFilters();

    expect(productService.selectedCategoryId()).toBeNull();
    expect(productService.searchQuery()).toBe('');
  });
});
