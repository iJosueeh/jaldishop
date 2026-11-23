import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductEditDrawer } from './product-edit-drawer';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MediaUploadService } from '../../../../core/services/media-upload.service';
import { InventoryService } from '../../../../core/services/inventory.service';
import { TokenService } from '../../../../core/services/token-service';
import { VariantInventoryCoordinatorService } from '../../../../core/services/variant-inventory-coordinator.service';
import { of, throwError } from 'rxjs';
import { Product, ProductCategory, ProductVariant } from '../../../../core/models/product.models';
import { InventoryResponse } from '../../../../core/models/inventory.models';
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
  let tokenServiceMock: {
    getUserId: ReturnType<typeof vi.fn>;
    getToken: ReturnType<typeof vi.fn>;
    hasValidToken: ReturnType<typeof vi.fn>;
  };
  let coordinatorMock: {
    createVariantWithInventory: ReturnType<typeof vi.fn>;
    initializePendingInventory: ReturnType<typeof vi.fn>;
    validateInventoryParameters: ReturnType<typeof vi.fn>;
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
    coordinatorMock = {
      createVariantWithInventory: vi.fn(),
      initializePendingInventory: vi.fn().mockReturnValue(of({ success: true })),
      validateInventoryParameters: vi.fn().mockReturnValue({ valid: true, error: null }),
    };
    tokenServiceMock = {
      getUserId: vi.fn().mockReturnValue('user-1'),
      getToken: vi.fn().mockReturnValue('valid-token'),
      hasValidToken: vi.fn().mockReturnValue(true),
    };

    vi.spyOn(InventoryService.prototype, 'getStoreInventory').mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ProductEditDrawer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: productServiceMock },
        { provide: StoreService, useValue: storeServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: MediaUploadService, useValue: mediaUploadServiceMock },
        { provide: VariantInventoryCoordinatorService, useValue: coordinatorMock },
        { provide: TokenService, useValue: tokenServiceMock },
        InventoryService,
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

  it('debe aplicar Cache-First y no llamar a getProductVariants si el producto ya tiene variantes precargadas', async () => {
    productServiceMock.getProductVariants.mockClear();
    const productWithVariants: Product = {
      ...mockProduct,
      id: 'prod-cached-99',
      variants: mockVariants,
    };
    fixture.componentRef.setInput('product', productWithVariants);
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(productServiceMock.getProductVariants).not.toHaveBeenCalled();
    expect(component.variants().length).toBe(1);
  });

  it('debe respetar el input initialTab cuando se define como variants', async () => {
    fixture.componentRef.setInput('initialTab', 'variants');
    fixture.componentRef.setInput('isOpen', true);
    await fixture.whenStable();
    expect(component.activeTab()).toBe('variants');
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
    component.singleVariantPrice.set(12.0); // mismo precio que var-1
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

  it('debe actualizar el precio de la variante única directamente desde la pestaña general si cambia', () => {
    const updatedProduct = { ...mockProduct };
    productServiceMock.updateProduct.mockReturnValue(of(updatedProduct));
    productServiceMock.updateProductVariant.mockReturnValue(of({ ...mockVariants[0], priceAmount: 18.0 }));

    component.singleVariantPrice.set(18.0);
    component.saveGeneralInfo();

    expect(productServiceMock.updateProduct).toHaveBeenCalled();
    expect(productServiceMock.updateProductVariant).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      'var-1',
      expect.objectContaining({ priceAmount: 18.0 }),
    );
  });

  it('debe agregar un nuevo formato/variante exitosamente usando el coordinador', () => {
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
    coordinatorMock.createVariantWithInventory.mockReturnValue(
      of({
        success: true,
        variant: newCreatedVariant,
        inventory: null,
        stepFailed: null,
        errorMessage: null,
      }),
    );

    component.submitNewVariant();

    expect(coordinatorMock.createVariantWithInventory).toHaveBeenCalledWith(
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

  it('debe conservar pendingCreatedVariant y reintentar inventario sin duplicar variante', () => {
    component.setActiveTab('variants');
    component.toggleAddVariant();
    component.onNewVariantNameChange('Porción Grande');
    component.newVariantPrice.set(20.0);
    component.newVariantTracksInventory.set(true);
    component.newVariantInitialStock.set(10);

    const createdVariant: ProductVariant = {
      id: 'var-partial-1',
      productId: 'prod-123',
      presentationName: 'Porción Grande',
      sku: 'TORT-LUC-GRAN',
      priceAmount: 20.0,
      priceCurrency: 'PEN',
      tracksInventory: true,
      status: 'ACTIVE',
    };

    // Simular fallo en paso de stock
    coordinatorMock.createVariantWithInventory.mockReturnValue(
      of({
        success: false,
        variant: createdVariant,
        inventory: null,
        stepFailed: 'STOCK',
        errorMessage: 'Variante creada en catálogo, pero falló stock físico',
      }),
    );

    component.submitNewVariant();

    expect(component.pendingCreatedVariant()).toEqual(createdVariant);
    expect(component.pendingInventoryStep()).toBe('STOCK');
    expect(toastServiceMock.error).toHaveBeenCalled();

    // Reintento: debe llamar a initializePendingInventory sin llamar de nuevo a createVariantWithInventory
    coordinatorMock.createVariantWithInventory.mockClear();
    coordinatorMock.initializePendingInventory.mockReturnValue(
      of({
        success: true,
        variant: createdVariant,
        inventory: { physicalQuantity: 10 } as any,
        stepFailed: null,
        errorMessage: null,
      }),
    );

    component.submitNewVariant();

    expect(coordinatorMock.createVariantWithInventory).not.toHaveBeenCalled();
    expect(coordinatorMock.initializePendingInventory).toHaveBeenCalledWith(
      createdVariant,
      10,
      null,
    );
    expect(component.pendingCreatedVariant()).toBeNull();
    expect(component.isAddingVariant()).toBe(false);
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

  it('debe inicializar la galería de fotos a partir de imageUrl si images no viene poblado', () => {
    const productWithImageOnly: Product = {
      ...mockProduct,
      imageUrl: 'https://res.cloudinary.com/demo/image/upload/torta.jpg',
      images: undefined,
    };
    fixture.componentRef.setInput('product', productWithImageOnly);
    fixture.detectChanges();

    expect(component.images().length).toBe(1);
    expect(component.images()[0].imageUrl).toBe('https://res.cloudinary.com/demo/image/upload/torta.jpg');
    expect(component.images()[0].isPrimary).toBe(true);
  });

  it('debe validar newVariantInitialStock al crear nueva variante con tracksInventory=true', () => {
    component.toggleAddVariant();
    component.newVariantName.set('Porción Familiar');
    component.newVariantPrice.set(30.0);
    component.newVariantTracksInventory.set(true);

    // Sin stock inicial -> inválido
    component.newVariantInitialStock.set(null);
    expect(component.isNewVariantValid()).toBe(false);

    // Stock negativo -> inválido
    component.newVariantInitialStock.set(-5);
    expect(component.isNewVariantValid()).toBe(false);

    // Stock decimal -> inválido
    component.newVariantInitialStock.set(4.2);
    expect(component.isNewVariantValid()).toBe(false);

    // Stock entero cero o positivo -> válido
    component.newVariantInitialStock.set(0);
    expect(component.isNewVariantValid()).toBe(true);

    component.newVariantInitialStock.set(12);
    expect(component.isNewVariantValid()).toBe(true);
  });

  it('debe mapear existencias de InventoryService distinguiendo CONFIGURED y NOT_CONFIGURED', () => {
    const invService = TestBed.inject(InventoryService);
    const mockInventoryList: InventoryResponse[] = [
      {
        variantId: 'var-1',
        productId: 'prod-123',
        productName: 'Torta de Lúcuma',
        presentationName: 'Porción',
        sku: 'TORT-LUC-PORC',
        tracksInventory: true,
        status: 'ACTIVE',
        inventoryStatus: 'CONFIGURED',
        physicalQuantity: 24,
        reservedQuantity: 4,
        availableQuantity: 20,
        lowStockThreshold: 5,
        isLowStock: false,
        updatedAt: '2026-11-16T10:00:00Z',
      },
    ];

    invService.inventory.set(mockInventoryList);
    fixture.detectChanges();

    const inv = component.inventoryMap().get('var-1');
    expect(inv).toBeDefined();
    expect(inv?.inventoryStatus).toBe('CONFIGURED');
    expect(inv?.physicalQuantity).toBe(24);
    expect(inv?.availableQuantity).toBe(20);

    // Variante sin inventario en lista -> NOT_CONFIGURED
    const missingInv = component.inventoryMap().get('var-missing');
    expect(missingInv).toBeUndefined();
  });

  it('debe editar nombre y precio de una variante sin alterar accidentalmente su stock físico', () => {
    const targetVariant = mockVariants[0];
    component.startEditVariant(targetVariant);
    component.editVariantName.set('Porción Grande');
    component.editVariantPrice.set(15.0);

    const updatedVariant: ProductVariant = {
      ...targetVariant,
      presentationName: 'Porción Grande',
      priceAmount: 15.0,
    };
    productServiceMock.updateProductVariant.mockReturnValue(of(updatedVariant));

    component.saveEditVariant(targetVariant);

    // Debe llamar a updateProductVariant con los datos de catálogo sin enviar ni tocar stock
    expect(productServiceMock.updateProductVariant).toHaveBeenCalledWith(
      'store-1',
      'prod-123',
      'var-1',
      expect.objectContaining({
        presentationName: 'Porción Grande',
        priceAmount: 15.0,
      }),
    );
    expect(coordinatorMock.initializePendingInventory).not.toHaveBeenCalled();
  });

  it('debe limpiar pendingCreatedVariant al cancelar y no bloquear el drawer', () => {
    component.pendingCreatedVariant.set(mockVariants[0]);
    component.pendingInventoryStep.set('STOCK');

    component.toggleAddVariant(); // Cierra formulario
    expect(component.pendingCreatedVariant()).toBeNull();
    expect(component.pendingInventoryStep()).toBeNull();
  });
});
