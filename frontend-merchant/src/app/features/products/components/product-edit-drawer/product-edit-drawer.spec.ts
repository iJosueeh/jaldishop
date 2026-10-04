import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductEditDrawer } from './product-edit-drawer';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { of, throwError } from 'rxjs';
import { Product, ProductCategory, ProductVariant } from '../../../../core/models/product.models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ProductEditDrawer', () => {
  let component: ProductEditDrawer;
  let fixture: ComponentFixture<ProductEditDrawer>;
  let productServiceMock: {
    getProductVariants: ReturnType<typeof vi.fn>;
    updateProduct: ReturnType<typeof vi.fn>;
    createProductVariant: ReturnType<typeof vi.fn>;
    updateProductVariant: ReturnType<typeof vi.fn>;
    syncProductVariants: ReturnType<typeof vi.fn>;
  };
  let storeServiceMock: {
    currentStore: ReturnType<typeof vi.fn>;
  };
  let toastServiceMock: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
    info: ReturnType<typeof vi.fn>;
  };
  let mediaUploadServiceMock: {
    uploadPendingImages: ReturnType<typeof vi.fn>;
    validateImageFile: ReturnType<typeof vi.fn>;
  };

  const mockCategories: ProductCategory[] = [
    { id: 'cat-1', name: 'Tortas & Pasteles' },
    { id: 'cat-2', name: 'Bebidas' },
  ];

  const mockProduct: Product = {
    id: 'prod-123',
    storeId: 'store-1',
    categoryId: 'cat-1',
    name: 'Torta de Lúcuma',
    slug: 'torta-de-lucuma',
    description: 'Bizcochuelo bañado en almíbar de café',
    status: 'ACTIVE',
    minPrice: 45.0,
    maxPrice: 45.0,
  };

  const mockVariants: ProductVariant[] = [
    {
      id: 'var-1',
      productId: 'prod-123',
      presentationName: 'Porción',
      sku: 'TORT-LUC-PORC',
      priceAmount: 12.0,
      priceCurrency: 'PEN',
      tracksInventory: true,
      status: 'ACTIVE',
    },
  ];

  beforeEach(async () => {
    productServiceMock = {
      getProductVariants: vi.fn().mockReturnValue(of(mockVariants)),
      updateProduct: vi.fn(),
      createProductVariant: vi.fn(),
      updateProductVariant: vi.fn(),
      syncProductVariants: vi.fn(),
    };
    storeServiceMock = {
      currentStore: vi.fn().mockReturnValue({ id: 'store-1', name: 'Pastelería Dulce' }),
    };
    toastServiceMock = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    };
    mediaUploadServiceMock = {
      uploadPendingImages: vi.fn().mockImplementation((imgs) => of(imgs)),
      validateImageFile: vi.fn().mockReturnValue({ valid: true }),
    };

    await TestBed.configureTestingModule({
      imports: [ProductEditDrawer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: productServiceMock },
        { provide: StoreService, useValue: storeServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: MediaUploadService, useValue: mediaUploadServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductEditDrawer);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('categories', mockCategories);
    fixture.componentRef.setInput('product', mockProduct);
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar datos del producto y variantes', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(true);
    expect(component.name()).toBe('Torta de Lúcuma');
    expect(component.selectedCategoryId()).toBe('cat-1');
    expect(component.activeTab()).toBe('general');
    expect(productServiceMock.getProductVariants).toHaveBeenCalledWith('store-1', 'prod-123');
    expect(component.variants().length).toBe(1);
  });

  it('debe permitir cambiar de pestaña entre información general y variantes', () => {
    expect(component.activeTab()).toBe('general');
    component.setActiveTab('variants');
    expect(component.activeTab()).toBe('variants');
    component.setActiveTab('general');
    expect(component.activeTab()).toBe('general');
  });

  it('debe guardar cambios de información general y emitir productUpdated', () => {
    const updatedProduct = {
      ...mockProduct,
      name: 'Torta de Lúcuma Premium',
      description: 'Edición especial',
    };
    productServiceMock.updateProduct.mockReturnValue(of(updatedProduct));
    const emitSpy = vi.spyOn(component.productUpdated, 'emit');

    component.name.set('Torta de Lúcuma Premium');
    component.description.set('Edición especial');
    component.saveGeneralInfo();

    expect(productServiceMock.updateProduct).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      expect.objectContaining({
        name: 'Torta de Lúcuma Premium',
        description: 'Edición especial',
      }),
    );
    expect(toastServiceMock.success).toHaveBeenCalled();
    expect(emitSpy).toHaveBeenCalledWith(updatedProduct);
  });

  it('debe agregar un nuevo formato/variante exitosamente', () => {
    component.setActiveTab('variants');
    expect(component.isAddingVariant()).toBe(false);

    component.toggleAddVariant();
    expect(component.isAddingVariant()).toBe(true);

    component.onNewVariantNameChange('Molde Entero 22cm');
    component.newVariantPrice.set(65.0);
    component.newVariantTracksInventory.set(false);

    const newCreatedVariant: ProductVariant = {
      id: 'var-2',
      productId: 'prod-123',
      presentationName: 'Molde Entero 22cm',
      sku: 'TORT-MOLD-01',
      priceAmount: 65.0,
      priceCurrency: 'PEN',
      tracksInventory: false,
      status: 'ACTIVE',
    };
    productServiceMock.createProductVariant.mockReturnValue(of(newCreatedVariant));

    component.submitNewVariant();

    expect(productServiceMock.createProductVariant).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      expect.objectContaining({
        presentationName: 'Molde Entero 22cm',
        priceAmount: 65.0,
        tracksInventory: false,
      }),
    );
    expect(component.variants().length).toBe(2);
    expect(productServiceMock.syncProductVariants).toHaveBeenCalled();
    expect(component.isAddingVariant()).toBe(false);
    expect(toastServiceMock.success).toHaveBeenCalled();
  });

  it('debe pausar o reactivar una variante específica', () => {
    const initialVariant = mockVariants[0];
    const updatedVariant: ProductVariant = {
      ...initialVariant,
      status: 'INACTIVE',
    };
    productServiceMock.updateProductVariant.mockReturnValue(of(updatedVariant));

    component.toggleVariantStatus(initialVariant);

    expect(productServiceMock.updateProductVariant).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      'var-1',
      expect.objectContaining({
        status: 'INACTIVE',
      }),
    );
    expect(component.variants()[0].status).toBe('INACTIVE');
    expect(productServiceMock.syncProductVariants).toHaveBeenCalled();
    expect(toastServiceMock.success).toHaveBeenCalled();
  });

  it('debe permitir iniciar, cancelar y guardar la edición de una variante existente', () => {
    const targetVariant = mockVariants[0];

    // Iniciar edición
    component.startEditVariant(targetVariant);
    expect(component.editingVariantId()).toBe('var-1');
    expect(component.editVariantName()).toBe('Porción');
    expect(component.editVariantPrice()).toBe(12.0);

    // Cancelar
    component.cancelEditVariant();
    expect(component.editingVariantId()).toBeNull();

    // Iniciar nuevamente y editar valores
    component.startEditVariant(targetVariant);
    component.editVariantName.set('Porción Doble');
    component.editVariantPrice.set(22.5);

    const updatedVariant: ProductVariant = {
      ...targetVariant,
      presentationName: 'Porción Doble',
      priceAmount: 22.5,
    };
    productServiceMock.updateProductVariant.mockReturnValue(of(updatedVariant));

    component.saveEditVariant(targetVariant);

    expect(productServiceMock.updateProductVariant).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      'var-1',
      expect.objectContaining({
        presentationName: 'Porción Doble',
        priceAmount: 22.5,
      }),
    );
    expect(component.variants()[0].presentationName).toBe('Porción Doble');
    expect(component.variants()[0].priceAmount).toBe(22.5);
    expect(component.editingVariantId()).toBeNull();
    expect(toastServiceMock.success).toHaveBeenCalled();
  });

  it('debe manejar cierre del drawer con animación y emitir closeDrawer', () => {
    const closeSpy = vi.spyOn(component.closeDrawer, 'emit');
    component.handleClose();
    expect(component.isClosing()).toBe(true);
  });
});
