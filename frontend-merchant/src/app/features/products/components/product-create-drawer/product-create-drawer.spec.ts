import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ProductCreateDrawer,
  generateProductSlug,
  generateProductSku,
} from './product-create-drawer';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { of, throwError } from 'rxjs';
import { Product, ProductCategory } from '../../../../core/models/product.models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ProductCreateDrawer', () => {
  let component: ProductCreateDrawer;
  let fixture: ComponentFixture<ProductCreateDrawer>;
  let productServiceMock: {
    createProductWithDefaultVariant: ReturnType<typeof vi.fn>;
  };
  let storeServiceMock: {
    currentStore: ReturnType<typeof vi.fn>;
  };
  let toastServiceMock: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
  };
  let mediaUploadServiceMock: {
    uploadPendingImages: ReturnType<typeof vi.fn>;
    validateImageFile: ReturnType<typeof vi.fn>;
  };

  const mockCategories: ProductCategory[] = [
    { id: 'cat-1', name: 'Tortas' },
    { id: 'cat-2', name: 'Bebidas' },
  ];

  beforeEach(async () => {
    productServiceMock = {
      createProductWithDefaultVariant: vi.fn(),
    };
    storeServiceMock = {
      currentStore: vi.fn().mockReturnValue({ id: 'store-uuid-1', name: 'Mi Tienda' }),
    };
    toastServiceMock = {
      success: vi.fn(),
      error: vi.fn(),
    };
    mediaUploadServiceMock = {
      uploadPendingImages: vi.fn().mockImplementation((imgs) => of(imgs)),
      validateImageFile: vi.fn().mockReturnValue({ valid: true }),
    };

    await TestBed.configureTestingModule({
      imports: [ProductCreateDrawer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: productServiceMock },
        { provide: StoreService, useValue: storeServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: MediaUploadService, useValue: mediaUploadServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCreateDrawer);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('categories', mockCategories);
    fixture.detectChanges();
  });

  it('debe crearse correctamente con estado inicial por defecto', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(false);
    expect(component.name()).toBe('');
    expect(component.canSubmit()).toBe(false);
    expect(component.presentationName()).toBe('Unidad Estándar');
    expect(component.showAdvancedOptions()).toBe(false);
  });

  it('generateProductSlug debe normalizar tildes, mayúsculas y caracteres especiales', () => {
    expect(generateProductSlug('Torta de Café & Chocolate!')).toBe('torta-de-cafe-chocolate');
    expect(generateProductSlug('  Panetón Artesanal 2026   ')).toBe('paneton-artesanal-2026');
  });

  it('generateProductSku debe crear un código corto y profesional omitiendo conectores', () => {
    expect(generateProductSku('Torta de Chocolate')).toBe('TORT-CHOC-01');
    expect(generateProductSku('Panetón Artesanal de Frutas')).toBe('PANE-ARTE-FRUT-01');
    expect(generateProductSku('Croissant')).toBe('CROISS-01');
    expect(generateProductSku('')).toBe('');
  });

  it('debe auto-generar slug y SKU al escribir el nombre sin tocar opciones avanzadas', () => {
    component.onNameChange('Alfajor de Maicena');
    expect(component.name()).toBe('Alfajor de Maicena');
    expect(component.slug()).toBe('alfajor-de-maicena');
    expect(component.sku()).toBe('ALFA-MAIC-01');

    // Al personalizar el slug, no debe sobreescribirse
    component.onSlugChange('alfajor-premium');
    expect(component.isCustomSlug()).toBe(true);

    // Al personalizar el SKU, no debe sobreescribirse
    component.onSkuChange('ALF-SPECIAL-01');
    expect(component.isCustomSku()).toBe(true);

    component.onNameChange('Alfajor Nuevo');
    expect(component.slug()).toBe('alfajor-premium');
    expect(component.sku()).toBe('ALF-SPECIAL-01');

    // Restablecer a default
    component.resetSlugToDefault();
    expect(component.isCustomSlug()).toBe(false);
    expect(component.slug()).toBe('alfajor-nuevo');

    component.resetSkuToDefault();
    expect(component.isCustomSku()).toBe(false);
    expect(component.sku()).toBe('ALFA-NUEV-01');
  });

  it('debe permitir alternar la visibilidad de opciones avanzadas y limpiar SKU', () => {
    expect(component.showAdvancedOptions()).toBe(false);
    component.toggleAdvancedOptions();
    expect(component.showAdvancedOptions()).toBe(true);

    component.onNameChange('Tarta de Limón');
    expect(component.sku()).toBe('TART-LIMO-01');

    component.clearSku();
    expect(component.sku()).toBe('');
    expect(component.isCustomSku()).toBe(true);
  });

  it('debe validar canSubmit correctamente según campos obligatorios', () => {
    expect(component.canSubmit()).toBe(false);

    // Solo nombre
    component.onNameChange('Café Americano');
    expect(component.canSubmit()).toBe(false);

    // Nombre + Categoría
    component.selectedCategoryId.set('cat-2');
    expect(component.canSubmit()).toBe(false);

    // Nombre + Categoría + Precio válido
    component.priceAmount.set(8.5);
    expect(component.canSubmit()).toBe(true);

    // Si el precio es <= 0, no es válido
    component.priceAmount.set(0);
    expect(component.canSubmit()).toBe(false);

    // En modo nueva categoría, requiere nombre de categoría
    component.priceAmount.set(8.5);
    component.toggleCategoryMode(true);
    expect(component.canSubmit()).toBe(false);

    component.newCategoryName.set('Cafetería de Especialidad');
    expect(component.canSubmit()).toBe(true);
  });

  it('debe enviar la petición de creación con categoría existente y emitir evento de éxito', () => {
    const createdProductMock: Product = {
      id: 'prod-99',
      storeId: 'store-uuid-1',
      categoryId: 'cat-1',
      name: 'Cheesecake de Frutos Rojos',
      slug: 'cheesecake-de-frutos-rojos',
      status: 'ACTIVE',
      minPrice: 18.0,
    };

    productServiceMock.createProductWithDefaultVariant.mockReturnValue(of(createdProductMock));
    const productCreatedSpy = vi.spyOn(component.productCreated, 'emit');

    component.onNameChange('Cheesecake de Frutos Rojos');
    component.selectedCategoryId.set('cat-1');
    component.priceAmount.set(18.0);
    component.description.set('Base crocante de galleta');
    component.tracksInventory.set(true);

    component.onSubmit();

    expect(productServiceMock.createProductWithDefaultVariant).toHaveBeenCalledWith(
      'store-uuid-1',
      expect.objectContaining({
        categoryId: 'cat-1',
        name: 'Cheesecake de Frutos Rojos',
        priceAmount: 18.0,
        tracksInventory: true,
        sku: 'CHEE-FRUT-ROJO-01',
      }),
    );
    expect(toastServiceMock.success).toHaveBeenCalled();
    expect(productCreatedSpy).toHaveBeenCalledWith(createdProductMock);
  });

  it('debe enviar la petición con newCategoryName si se activa creación de categoría inline', () => {
    const createdProductMock: Product = {
      id: 'prod-100',
      storeId: 'store-uuid-1',
      name: 'Cold Brew 250ml',
      slug: 'cold-brew-250ml',
      status: 'ACTIVE',
      minPrice: 12.0,
    };

    productServiceMock.createProductWithDefaultVariant.mockReturnValue(of(createdProductMock));

    component.onNameChange('Cold Brew 250ml');
    component.toggleCategoryMode(true);
    component.newCategoryName.set('Bebidas Frías');
    component.priceAmount.set(12.0);

    component.onSubmit();

    expect(productServiceMock.createProductWithDefaultVariant).toHaveBeenCalledWith(
      'store-uuid-1',
      expect.objectContaining({
        newCategoryName: 'Bebidas Frías',
        name: 'Cold Brew 250ml',
        priceAmount: 12.0,
      }),
    );
  });

  it('debe capturar errores de la API y mostrar mensaje de error', () => {
    const apiError = {
      error: {
        message: 'Ya existe un producto con este slug en tu tienda.',
      },
    };

    productServiceMock.createProductWithDefaultVariant.mockReturnValue(throwError(() => apiError));

    component.onNameChange('Producto Repetido');
    component.selectedCategoryId.set('cat-1');
    component.priceAmount.set(10.0);

    component.onSubmit();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Ya existe un producto con este slug en tu tienda.');
  });

  it('debe permitir crear producto con múltiples opciones/variantes', () => {
    const createdProductMock: Product = {
      id: 'prod-multi',
      storeId: 'store-uuid-1',
      name: 'Café de Especialidad',
      slug: 'cafe-de-especialidad',
      status: 'ACTIVE',
      minPrice: 20.0,
      maxPrice: 38.0,
    };

    productServiceMock.createProductWithDefaultVariant.mockReturnValue(of(createdProductMock));

    component.onNameChange('Café de Especialidad');
    component.selectedCategoryId.set('cat-1');
    component.setPricingMode('multiple');

    expect(component.formVariants().length).toBe(2);

    // Configurar variante 1
    component.updateVariantName(0, 'Bolsa 250g');
    component.updateVariantPrice(0, 20.0);

    // Configurar variante 2
    component.updateVariantName(1, 'Bolsa 500g');
    component.updateVariantPrice(1, 38.0);

    expect(component.canSubmit()).toBe(true);

    component.onSubmit();

    expect(productServiceMock.createProductWithDefaultVariant).toHaveBeenCalledWith(
      'store-uuid-1',
      expect.objectContaining({
        name: 'Café de Especialidad',
        variants: expect.arrayContaining([
          expect.objectContaining({ presentationName: 'Bolsa 250g', priceAmount: 20.0 }),
          expect.objectContaining({ presentationName: 'Bolsa 500g', priceAmount: 38.0 }),
        ]),
      }),
    );
  });

  it('debe permitir agregar y eliminar opciones en modalidad múltiple', () => {
    component.setPricingMode('multiple');
    expect(component.formVariants().length).toBe(2);

    component.addVariantOption();
    expect(component.formVariants().length).toBe(3);

    component.removeVariantOption(2);
    expect(component.formVariants().length).toBe(2);

    // No debe eliminar si solo quedan 1 o menos
    component.removeVariantOption(0);
    expect(component.formVariants().length).toBe(1);
    component.removeVariantOption(0);
    expect(component.formVariants().length).toBe(1);
  });
});
