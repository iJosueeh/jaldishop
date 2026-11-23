import { TestBed } from '@angular/core/testing';
import { of, throwError, firstValueFrom } from 'rxjs';
import { vi } from 'vitest';
import {
  CreateVariantWithInventoryRequest,
  VariantInventoryCoordinatorService,
} from './variant-inventory-coordinator.service';
import { ProductService } from './product.service';
import { InventoryService } from './inventory.service';
import { TokenService } from './token-service';
import { ProductVariant } from '../models/product.models';
import { InventoryResponse } from '../models/inventory.models';

describe('VariantInventoryCoordinatorService', () => {
  let coordinator: VariantInventoryCoordinatorService;
  let productServiceMock: {
    createProductVariant: ReturnType<typeof vi.fn>;
  };
  let inventoryServiceMock: {
    updateStock: ReturnType<typeof vi.fn>;
    updateThreshold: ReturnType<typeof vi.fn>;
  };
  let tokenServiceMock: {
    hasValidToken: ReturnType<typeof vi.fn>;
  };

  const mockStoreId = 'store-uuid-1';
  const mockProductId = 'product-uuid-1';

  const mockCreatedVariant: ProductVariant = {
    id: 'variant-uuid-1',
    productId: mockProductId,
    presentationName: 'Bolsa x 1kg',
    sku: 'PAN-01',
    priceAmount: 18.5,
    priceCurrency: 'PEN',
    tracksInventory: true,
    status: 'ACTIVE',
  };

  const mockStockResponse: InventoryResponse = {
    variantId: 'variant-uuid-1',
    productId: mockProductId,
    productName: 'Pan Artesanal',
    presentationName: 'Bolsa x 1kg',
    sku: 'PAN-01',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 15,
    reservedQuantity: 0,
    availableQuantity: 15,
    lowStockThreshold: null,
    isLowStock: false,
    updatedAt: '2026-11-16T12:00:00Z',
  };

  const mockThresholdResponse: InventoryResponse = {
    ...mockStockResponse,
    lowStockThreshold: 5,
  };

  beforeEach(() => {
    productServiceMock = {
      createProductVariant: vi.fn(),
    };
    inventoryServiceMock = {
      updateStock: vi.fn(),
      updateThreshold: vi.fn(),
    };
    tokenServiceMock = {
      hasValidToken: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        VariantInventoryCoordinatorService,
        { provide: ProductService, useValue: productServiceMock },
        { provide: InventoryService, useValue: inventoryServiceMock },
        { provide: TokenService, useValue: tokenServiceMock },
      ],
    });

    coordinator = TestBed.inject(VariantInventoryCoordinatorService);
  });

  // Caso 1: Variante sin seguimiento de stock
  it('1. Debe crear variante sin seguimiento de stock sin exigir ni llamar al servicio de inventario', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Porción Personal',
      priceAmount: 10,
      tracksInventory: false,
    };

    const variantWithoutTrack: ProductVariant = {
      ...mockCreatedVariant,
      tracksInventory: false,
    };

    productServiceMock.createProductVariant.mockReturnValue(of(variantWithoutTrack));

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(true);
    expect(result.variant).toEqual(variantWithoutTrack);
    expect(result.inventory).toBeNull();
    expect(productServiceMock.createProductVariant).toHaveBeenCalled();
    expect(inventoryServiceMock.updateStock).not.toHaveBeenCalled();
    expect(inventoryServiceMock.updateThreshold).not.toHaveBeenCalled();
  });

  // Caso 2: Variante con seguimiento y stock inicial positivo
  it('2. Debe crear variante con seguimiento, inicializar stock positivo y configurar umbral', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Bolsa x 1kg',
      priceAmount: 18.5,
      tracksInventory: true,
      initialStock: 15,
      lowStockThreshold: 5,
    };

    productServiceMock.createProductVariant.mockReturnValue(of(mockCreatedVariant));
    inventoryServiceMock.updateStock.mockReturnValue(of(mockStockResponse));
    inventoryServiceMock.updateThreshold.mockReturnValue(of(mockThresholdResponse));

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(true);
    expect(result.variant).toEqual(mockCreatedVariant);
    expect(result.inventory).toEqual(mockThresholdResponse);
    expect(productServiceMock.createProductVariant).toHaveBeenCalled();
    expect(inventoryServiceMock.updateStock).toHaveBeenCalledWith('variant-uuid-1', 15);
    expect(inventoryServiceMock.updateThreshold).toHaveBeenCalledWith('variant-uuid-1', 5);
  });

  // Caso 3: Variante con seguimiento y stock inicial cero
  it('3. Debe permitir inicializar stock cero cuando tracksInventory=true', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Unidad Agotada Inicial',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: 0,
      lowStockThreshold: null,
    };

    const zeroStockResponse: InventoryResponse = {
      ...mockStockResponse,
      physicalQuantity: 0,
      availableQuantity: 0,
    };

    productServiceMock.createProductVariant.mockReturnValue(of(mockCreatedVariant));
    inventoryServiceMock.updateStock.mockReturnValue(of(zeroStockResponse));

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(true);
    expect(result.inventory?.physicalQuantity).toBe(0);
    expect(inventoryServiceMock.updateStock).toHaveBeenCalledWith('variant-uuid-1', 0);
    expect(inventoryServiceMock.updateThreshold).not.toHaveBeenCalled();
  });

  // Caso 4: Stock inicial inválido (negativo, decimal o ausente)
  it('4. Debe rechazar validación en memoria ante stock inicial negativo o decimal sin emitir HTTP', async () => {
    const negativeReq: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: -1,
    };

    await expect(
      firstValueFrom(
        coordinator.createVariantWithInventory(mockStoreId, mockProductId, negativeReq),
      ),
    ).rejects.toThrow('mayor o igual a cero');
    expect(productServiceMock.createProductVariant).not.toHaveBeenCalled();

    const decimalReq: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: 3.5,
    };

    await expect(
      firstValueFrom(
        coordinator.createVariantWithInventory(mockStoreId, mockProductId, decimalReq),
      ),
    ).rejects.toThrow('número entero');
    expect(productServiceMock.createProductVariant).not.toHaveBeenCalled();
  });

  // Caso 5: Error al crear variante
  it('5. Debe reportar fallo en paso VARIANT y no llamar a stock ni umbral si falla creación de variante', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: 10,
    };

    productServiceMock.createProductVariant.mockReturnValue(
      throwError(() => ({ error: { message: 'El SKU ya está en uso' } })),
    );

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(false);
    expect(result.stepFailed).toBe('VARIANT');
    expect(result.variant).toBeNull();
    expect(result.errorMessage).toContain('El SKU ya está en uso');
    expect(inventoryServiceMock.updateStock).not.toHaveBeenCalled();
  });

  // Caso 6: Error al inicializar inventario (fallo parcial)
  it('6. Debe conservar la variante creada y marcar stepFailed STOCK si falla la llamada de stock', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: 10,
    };

    productServiceMock.createProductVariant.mockReturnValue(of(mockCreatedVariant));
    inventoryServiceMock.updateStock.mockReturnValue(
      throwError(() => new Error('Error al conectar con el servicio de inventario')),
    );

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(false);
    expect(result.stepFailed).toBe('STOCK');
    expect(result.variant).toEqual(mockCreatedVariant);
    expect(result.variant?.id).toBe('variant-uuid-1');
    expect(result.inventory).toBeNull();
    expect(result.errorMessage).toContain('falló la inicialización de stock físico');
  });

  // Caso 7: Reintento sin duplicar variante
  it('7. initializePendingInventory debe reintentar configuración de inventario sin duplicar la variante', async () => {
    inventoryServiceMock.updateStock.mockReturnValue(of(mockStockResponse));
    inventoryServiceMock.updateThreshold.mockReturnValue(of(mockThresholdResponse));

    const result = await firstValueFrom(
      coordinator.initializePendingInventory(mockCreatedVariant, 15, 5),
    );

    expect(result.success).toBe(true);
    expect(result.variant).toEqual(mockCreatedVariant);
    expect(result.inventory).toEqual(mockThresholdResponse);
    expect(productServiceMock.createProductVariant).not.toHaveBeenCalled();
    expect(inventoryServiceMock.updateStock).toHaveBeenCalledWith('variant-uuid-1', 15);
    expect(inventoryServiceMock.updateThreshold).toHaveBeenCalledWith('variant-uuid-1', 5);
  });

  // Caso 8: Error al configurar umbral
  it('8. Debe conservar stock confirmado y marcar stepFailed THRESHOLD si falla el umbral', async () => {
    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: true,
      initialStock: 15,
      lowStockThreshold: 5,
    };

    productServiceMock.createProductVariant.mockReturnValue(of(mockCreatedVariant));
    inventoryServiceMock.updateStock.mockReturnValue(of(mockStockResponse));
    inventoryServiceMock.updateThreshold.mockReturnValue(
      throwError(() => ({ error: { message: 'Umbral no válido' } })),
    );

    const result = await firstValueFrom(
      coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
    );

    expect(result.success).toBe(false);
    expect(result.stepFailed).toBe('THRESHOLD');
    expect(result.variant).toEqual(mockCreatedVariant);
    expect(result.inventory).toEqual(mockStockResponse);
    expect(result.errorMessage).toContain('falló la configuración del umbral');
  });

  // Caso 9: Actualización de parámetros y validación de umbral
  it('9. validateInventoryParameters debe validar correctamente todas las combinaciones', () => {
    expect(coordinator.validateInventoryParameters(false).valid).toBe(true);
    expect(coordinator.validateInventoryParameters(true, 10, null).valid).toBe(true);
    expect(coordinator.validateInventoryParameters(true, 10, 2).valid).toBe(true);
    expect(coordinator.validateInventoryParameters(true, 10, -1).valid).toBe(false);
    expect(coordinator.validateInventoryParameters(true, 10, 2.5).valid).toBe(false);
    expect(coordinator.validateInventoryParameters(true, null, 2).valid).toBe(false);
  });

  // Caso 10: Aislamiento entre sesiones
  it('10. Debe rechazar la operación si el token no es válido o la sesión expiró', async () => {
    tokenServiceMock.hasValidToken.mockReturnValue(false);

    const request: CreateVariantWithInventoryRequest = {
      presentationName: 'Pan',
      priceAmount: 5,
      tracksInventory: false,
    };

    await expect(
      firstValueFrom(
        coordinator.createVariantWithInventory(mockStoreId, mockProductId, request),
      ),
    ).rejects.toThrow('Sesión no válida o expirada');

    expect(productServiceMock.createProductVariant).not.toHaveBeenCalled();
  });
});
