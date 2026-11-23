import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Inventory } from './inventory';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';
import { InventoryResponse } from '../../core/models/inventory.models';

describe('Inventory Component', () => {
  let component: Inventory;
  let fixture: ComponentFixture<Inventory>;
  let inventoryService: InventoryService;
  let toastService: ToastService;

  const mockItems: InventoryResponse[] = [
    {
      variantId: 'var-1',
      productId: 'prod-1',
      productName: 'Pan Ciabatta Artesanal',
      presentationName: 'Bolsa x 10',
      sku: 'CIA-010',
      tracksInventory: true,
      status: 'ACTIVE',
      inventoryStatus: 'CONFIGURED',
      physicalQuantity: 50,
      reservedQuantity: 5,
      availableQuantity: 45,
      lowStockThreshold: 10,
      isLowStock: false,
      updatedAt: '2026-03-30T10:00:00Z',
    },
    {
      variantId: 'var-2',
      productId: 'prod-1',
      productName: 'Pan Ciabatta Artesanal',
      presentationName: 'Bolsa x 20',
      sku: 'CIA-020',
      tracksInventory: true,
      status: 'ACTIVE',
      inventoryStatus: 'CONFIGURED',
      physicalQuantity: 8,
      reservedQuantity: 2,
      availableQuantity: 6,
      lowStockThreshold: 10,
      isLowStock: true,
      updatedAt: '2026-03-30T10:00:00Z',
    },
    {
      variantId: 'var-3',
      productId: 'prod-2',
      productName: 'Croissant Mantequilla',
      presentationName: 'Unidad',
      sku: null,
      tracksInventory: true,
      status: 'ACTIVE',
      inventoryStatus: 'NOT_CONFIGURED',
      physicalQuantity: null,
      reservedQuantity: null,
      availableQuantity: null,
      lowStockThreshold: null,
      isLowStock: false,
      updatedAt: null,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inventory],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        InventoryService,
        ToastService,
      ],
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    toastService = TestBed.inject(ToastService);

    vi.spyOn(inventoryService, 'getStoreInventory').mockReturnValue(of(mockItems));
    vi.spyOn(toastService, 'success').mockReturnValue('toast-1');
    vi.spyOn(toastService, 'error').mockReturnValue('toast-2');

    // Simular que el servicio ya tiene los items en memoria
    inventoryService.inventory.set(mockItems);

    fixture = TestBed.createComponent(Inventory);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e inicializar la carga de inventario y activar visibilidad', () => {
    expect(component).toBeTruthy();
    expect(inventoryService.getStoreInventory).toHaveBeenCalled();
    expect(inventoryService.isViewActive()).toBe(true);

    fixture.destroy();
    expect(inventoryService.isViewActive()).toBe(false);
  });

  it('debe calcular pagedInventory cortando los elementos según la página actual', () => {
    component.pageSize.set(2);
    fixture.detectChanges();

    const paged = component.pagedInventory();
    expect(paged.length).toBe(2);
    expect(paged[0].variantId).toBe('var-1');
    expect(paged[1].variantId).toBe('var-2');

    component.onPageChange(2);
    const pagedPage2 = component.pagedInventory();
    expect(pagedPage2.length).toBe(1);
    expect(pagedPage2[0].variantId).toBe('var-3');
  });

  it('debe actualizar búsqueda y resetear la página a 1', () => {
    component.currentPage.set(2);
    const searchSpy = vi.spyOn(inventoryService, 'setSearchQuery');

    component.onSearchChange('Ciabatta');

    expect(searchSpy).toHaveBeenCalledWith('Ciabatta');
    expect(component.currentPage()).toBe(1);
  });

  it('debe actualizar filtro de estado y resetear la página a 1', () => {
    component.currentPage.set(2);
    const filterSpy = vi.spyOn(inventoryService, 'setStatusFilter');

    component.onFilterChange('LOW_STOCK');

    expect(filterSpy).toHaveBeenCalledWith('LOW_STOCK');
    expect(component.currentPage()).toBe(1);
  });

  it('debe limpiar filtros y resetear la página a 1', () => {
    component.currentPage.set(2);
    const clearSpy = vi.spyOn(inventoryService, 'clearFilters');

    component.onClearFilters();

    expect(clearSpy).toHaveBeenCalled();
    expect(component.currentPage()).toBe(1);
  });

  it('debe abrir y cerrar el drawer de ajuste de stock', () => {
    const item = mockItems[0];
    component.openStockDrawer(item);

    expect(component.isStockDrawerOpen()).toBe(true);
    expect(component.selectedVariantForStock()).toEqual(item);

    component.closeStockDrawer();

    expect(component.isStockDrawerOpen()).toBe(false);
    expect(component.selectedVariantForStock()).toBeNull();
  });

  it('debe abrir y cerrar el modal de umbral de stock', () => {
    const item = mockItems[0];
    component.openThresholdModal(item);

    expect(component.isThresholdModalOpen()).toBe(true);
    expect(component.selectedVariantForThreshold()).toEqual(item);

    component.closeThresholdModal();

    expect(component.isThresholdModalOpen()).toBe(false);
    expect(component.selectedVariantForThreshold()).toBeNull();
  });

  it('debe mostrar toast de éxito al guardar stock', () => {
    component.onStockSaved(mockItems[0]);
    expect(toastService.success).toHaveBeenCalledWith('Stock físico actualizado a 50 unidades.');
  });

  it('debe mostrar toast de éxito al guardar umbral numérico y null', () => {
    component.onThresholdSaved(mockItems[0]);
    expect(toastService.success).toHaveBeenCalledWith(
      'Umbral de stock bajo actualizado a 10 unidades.',
    );

    component.onThresholdSaved(mockItems[2]);
    expect(toastService.success).toHaveBeenCalledWith('Alerta de stock bajo desactivada.');
  });

  it('debe refrescar forzando llamada y mostrar feedback', () => {
    vi.mocked(inventoryService.getStoreInventory).mockClear();
    vi.mocked(inventoryService.getStoreInventory).mockReturnValue(of(mockItems));

    component.onRefresh();

    expect(inventoryService.getStoreInventory).toHaveBeenCalledWith(true);
    expect(toastService.success).toHaveBeenCalledWith('Inventario sincronizado.');
  });

  it('debe mostrar toast de error si el refresco falla', () => {
    vi.mocked(inventoryService.getStoreInventory).mockClear();
    vi.mocked(inventoryService.getStoreInventory).mockReturnValue(
      throwError(() => new Error('Error al refrescar')),
    );

    component.onRefresh();

    expect(toastService.error).toHaveBeenCalledWith('Error al sincronizar el inventario.');
  });
});
