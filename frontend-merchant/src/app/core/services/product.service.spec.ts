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

  describe('createCategory()', () => {
    it('debe registrar una nueva categoría y actualizar la señal reactiva categories', () => {
      service.categories.set(mockCategories);

      const newCategoryMock: ProductCategory = {
        id: 'cat-3',
        name: 'Bebidas Calientes',
        description: 'Cafés y tés',
      };

      service
        .createCategory(mockStoreId, { name: 'Bebidas Calientes', description: 'Cafés y tés' })
        .subscribe((res) => {
          expect(res).toEqual(newCategoryMock);
        });

      const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/categories`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name: 'Bebidas Calientes', description: 'Cafés y tés' });
      req.flush(newCategoryMock);

      expect(service.categories().length).toBe(3);
      expect(service.categories().find((c) => c.id === 'cat-3')).toBeTruthy();
    });
  });

  describe('createProduct() y createProductVariant()', () => {
    it('debe emitir petición POST para crear producto base', () => {
      const newProductMock: Product = {
        id: 'prod-new',
        storeId: mockStoreId,
        categoryId: 'cat-1',
        name: 'Pie de Manzana',
        slug: 'pie-de-manzana',
        status: 'ACTIVE',
      };

      service
        .createProduct(mockStoreId, {
          categoryId: 'cat-1',
          name: 'Pie de Manzana',
          slug: 'pie-de-manzana',
        })
        .subscribe((res) => {
          expect(res.id).toBe('prod-new');
        });

      const req = httpMock.expectOne(`${environment.apiUrl}/merchants/stores/${mockStoreId}/products`);
      expect(req.request.method).toBe('POST');
      req.flush(newProductMock);
    });

    it('debe emitir petición POST para registrar la variante del producto', () => {
      const variantMock = {
        id: 'var-1',
        productId: 'prod-new',
        presentationName: 'Unidad Estándar',
        priceAmount: 25.5,
        priceCurrency: 'PEN',
        tracksInventory: false,
        status: 'ACTIVE' as const,
      };

      service
        .createProductVariant(mockStoreId, 'prod-new', {
          presentationName: 'Unidad Estándar',
          priceAmount: 25.5,
          priceCurrency: 'PEN',
          tracksInventory: false,
        })
        .subscribe((res) => {
          expect(res).toEqual(variantMock);
        });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-new/variants`,
      );
      expect(req.request.method).toBe('POST');
      req.flush(variantMock);
    });
  });

  describe('createProductWithDefaultVariant()', () => {
    it('debe orquestar creación de producto y variante estándar con categoría existente', () => {
      service.categories.set(mockCategories);
      service.products.set(mockProducts);

      const createdProductBase = {
        id: 'prod-10',
        storeId: mockStoreId,
        categoryId: 'cat-1',
        name: 'Tarta de Frutos Rojos',
        slug: 'tarta-de-frutos-rojos',
        description: 'Fresas y arándanos frescos',
        status: 'ACTIVE',
      };

      const createdVariant = {
        id: 'var-10',
        productId: 'prod-10',
        presentationName: 'Porción',
        priceAmount: 18.0,
        priceCurrency: 'PEN',
        tracksInventory: true,
        status: 'ACTIVE' as const,
      };

      service
        .createProductWithDefaultVariant(mockStoreId, {
          categoryId: 'cat-1',
          name: 'Tarta de Frutos Rojos',
          description: 'Fresas y arándanos frescos',
          presentationName: 'Porción',
          priceAmount: 18.0,
          tracksInventory: true,
        })
        .subscribe((fullProduct) => {
          expect(fullProduct.id).toBe('prod-10');
          expect(fullProduct.minPrice).toBe(18.0);
          expect(fullProduct.categoryName).toBe('Tortas & Pasteles');
          expect(fullProduct.variants?.length).toBe(1);
        });

      // 1. POST producto
      const reqProduct = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products`,
      );
      expect(reqProduct.request.method).toBe('POST');
      reqProduct.flush(createdProductBase);

      // 2. POST variante
      const reqVariant = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-10/variants`,
      );
      expect(reqVariant.request.method).toBe('POST');
      reqVariant.flush(createdVariant);

      // 3. Verifica reactividad
      expect(service.products().length).toBe(4);
      expect(service.products()[0].id).toBe('prod-10');
      expect(service.isLoading()).toBe(false);
    });

    it('debe orquestar creación de producto con múltiples variantes', () => {
      service.categories.set(mockCategories);
      service.products.set([]);

      const createdProductBase = {
        id: 'prod-var',
        storeId: mockStoreId,
        categoryId: 'cat-1',
        name: 'Torta Selva Negra',
        status: 'ACTIVE',
      };

      const variant1 = {
        id: 'v-1',
        productId: 'prod-var',
        presentationName: 'Porción',
        priceAmount: 15.0,
        priceCurrency: 'PEN',
        tracksInventory: false,
        status: 'ACTIVE' as const,
      };

      const variant2 = {
        id: 'v-2',
        productId: 'prod-var',
        presentationName: 'Entera Familiar',
        priceAmount: 85.0,
        priceCurrency: 'PEN',
        tracksInventory: true,
        status: 'ACTIVE' as const,
      };

      service
        .createProductWithDefaultVariant(mockStoreId, {
          categoryId: 'cat-1',
          name: 'Torta Selva Negra',
          variants: [
            { presentationName: 'Porción', priceAmount: 15.0, tracksInventory: false },
            { presentationName: 'Entera Familiar', priceAmount: 85.0, tracksInventory: true },
          ],
        })
        .subscribe((fullProduct) => {
          expect(fullProduct.id).toBe('prod-var');
          expect(fullProduct.minPrice).toBe(15.0);
          expect(fullProduct.maxPrice).toBe(85.0);
          expect(fullProduct.variants?.length).toBe(2);
        });

      const reqProduct = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products`,
      );
      reqProduct.flush(createdProductBase);

      const reqVariants = httpMock.match(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-var/variants`,
      );
      expect(reqVariants.length).toBe(2);
      reqVariants[0].flush(variant1);
      reqVariants[1].flush(variant2);

      expect(service.products().length).toBe(1);
      expect(service.products()[0].minPrice).toBe(15.0);
      expect(service.products()[0].maxPrice).toBe(85.0);
    });

    it('debe crear primero la categoría si se especifica newCategoryName', () => {
      service.categories.set([]);
      service.products.set([]);

      const createdCategory = {
        id: 'cat-new-99',
        name: 'Alfajores & Dulces',
      };

      const createdProductBase = {
        id: 'prod-20',
        storeId: mockStoreId,
        categoryId: 'cat-new-99',
        name: 'Caja Alfajores Maicena',
        slug: 'caja-alfajores-maicena',
        status: 'ACTIVE',
      };

      const createdVariant = {
        id: 'var-20',
        productId: 'prod-20',
        presentationName: 'Caja x 12',
        priceAmount: 35.0,
        priceCurrency: 'PEN',
        tracksInventory: false,
        status: 'ACTIVE' as const,
      };

      service
        .createProductWithDefaultVariant(mockStoreId, {
          newCategoryName: 'Alfajores & Dulces',
          name: 'Caja Alfajores Maicena',
          presentationName: 'Caja x 12',
          priceAmount: 35.0,
          tracksInventory: false,
        })
        .subscribe((fullProduct) => {
          expect(fullProduct.id).toBe('prod-20');
          expect(fullProduct.categoryId).toBe('cat-new-99');
          expect(fullProduct.categoryName).toBe('Alfajores & Dulces');
        });

      // 1. POST categoría
      const reqCat = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/categories`,
      );
      expect(reqCat.request.method).toBe('POST');
      expect(reqCat.request.body).toEqual({ name: 'Alfajores & Dulces' });
      reqCat.flush(createdCategory);

      // 2. POST producto
      const reqProduct = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products`,
      );
      expect(reqProduct.request.method).toBe('POST');
      expect(reqProduct.request.body.categoryId).toBe('cat-new-99');
      reqProduct.flush(createdProductBase);

      // 3. POST variante
      const reqVariant = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-20/variants`,
      );
      expect(reqVariant.request.method).toBe('POST');
      reqVariant.flush(createdVariant);

      expect(service.categories().length).toBe(1);
      expect(service.products().length).toBe(1);
      expect(service.isLoading()).toBe(false);
    });

    it('debe fallar si no se provee categoryId ni newCategoryName', () => {
      service
        .createProductWithDefaultVariant(mockStoreId, {
          name: 'Producto Sin Categoría',
          priceAmount: 10,
          tracksInventory: false,
        })
        .subscribe({
          next: () => {
            throw new Error('No debió tener éxito sin categoría');
          },
          error: (err) => {
            expect(err.message).toContain('Debes seleccionar o crear una categoría');
            expect(service.isLoading()).toBe(false);
          },
        });
    });
  });

  describe('Gestión de Variantes (Formatos y Precios)', () => {
    it('getProductVariants debe obtener las variantes del backend', () => {
      const mockVariants = [
        {
          id: 'v1',
          productId: 'prod-1',
          presentationName: 'Porción',
          sku: 'P-01',
          priceAmount: 12.0,
          priceCurrency: 'PEN',
          tracksInventory: false,
          status: 'ACTIVE' as const,
        },
      ];

      service.getProductVariants(mockStoreId, 'prod-1').subscribe((res) => {
        expect(res).toEqual(mockVariants);
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-1/variants`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockVariants);
    });

    it('updateProductVariant debe enviar petición PUT para actualizar variante', () => {
      const updatePayload = {
        presentationName: 'Molde Grande 26cm',
        priceAmount: 85.0,
        priceCurrency: 'PEN',
        tracksInventory: true,
        status: 'ACTIVE' as const,
      };

      const updatedVariant = {
        id: 'v2',
        productId: 'prod-1',
        ...updatePayload,
        sku: 'P-02',
      };

      service.updateProductVariant(mockStoreId, 'prod-1', 'v2', updatePayload).subscribe((res) => {
        expect(res).toEqual(updatedVariant);
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/merchants/stores/${mockStoreId}/products/prod-1/variants/v2`,
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatePayload);
      req.flush(updatedVariant);
    });

    it('syncProductVariants debe recalcular minPrice y maxPrice en la lista de productos', () => {
      service.products.set([
        {
          id: 'prod-1',
          storeId: mockStoreId,
          name: 'Torta de Fresa',
          slug: 'torta-de-fresa',
          status: 'ACTIVE',
        },
      ]);

      const variants = [
        {
          id: 'v1',
          productId: 'prod-1',
          presentationName: 'Porción',
          sku: 'P-1',
          priceAmount: 15.0,
          priceCurrency: 'PEN',
          tracksInventory: false,
          status: 'ACTIVE' as const,
        },
        {
          id: 'v2',
          productId: 'prod-1',
          presentationName: 'Entera',
          sku: 'P-2',
          priceAmount: 70.0,
          priceCurrency: 'PEN',
          tracksInventory: false,
          status: 'ACTIVE' as const,
        },
      ];

      service.syncProductVariants('prod-1', variants);

      const updated = service.products().find((p) => p.id === 'prod-1');
      expect(updated).toBeDefined();
      expect(updated?.minPrice).toBe(15.0);
      expect(updated?.maxPrice).toBe(70.0);
      expect(updated?.variants?.length).toBe(2);
    });
  });
});
