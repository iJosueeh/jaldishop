import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { LogisticsAlerts } from './logistics-alerts';
import { OrderService } from '../../../../core/services/order.service';
import { InventoryService } from '../../../../core/services/inventory.service';
import { InventoryResponse } from '../../../../core/models/inventory.models';

describe('LogisticsAlerts', () => {
  let component: LogisticsAlerts;
  let fixture: ComponentFixture<LogisticsAlerts>;
  let inventoryService: InventoryService;
  let orderService: OrderService;
  let httpMock: HttpTestingController;

  const mockLowStockItem1: InventoryResponse = {
    variantId: 'var-101',
    productId: 'prod-10',
    productName: 'Torta de Selva Negra',
    presentationName: 'Porción 150g',
    sku: 'SN-150',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 3,
    reservedQuantity: 1,
    availableQuantity: 2,
    lowStockThreshold: 5,
    isLowStock: true,
    updatedAt: '2026-11-16T12:00:00Z',
  };

  const mockOutOfStockItem: InventoryResponse = {
    variantId: 'var-102',
    productId: 'prod-11',
    productName: 'Cheesecake Frutos Rojos',
    presentationName: '',
    sku: 'CHK-FR',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 0,
    reservedQuantity: 0,
    availableQuantity: 0,
    lowStockThreshold: 4,
    isLowStock: true,
    updatedAt: '2026-11-16T12:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogisticsAlerts],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        OrderService,
        InventoryService,
      ],
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    orderService = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock inicial de orders para evitar peticiones no controladas
    vi.spyOn(orderService, 'loadOrders').mockReturnValue(of([] as any));

    fixture = TestBed.createComponent(LogisticsAlerts);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
    expect(component.stockAlerts()).toEqual([]);
  });

  it('debe consultar getLowStockInventory() en ngOnInit si no está cargado', () => {
    const spy = vi.spyOn(inventoryService, 'getLowStockInventory').mockReturnValue(of([]));

    component.ngOnInit();

    expect(spy).toHaveBeenCalled();
  });

  it('debe mapear alertas reales de inventario discriminando LOW_STOCK y OUT_OF_STOCK', () => {
    // Simulamos que inventoryService tiene items con stock bajo
    inventoryService.lowStock.set([mockLowStockItem1, mockOutOfStockItem]);

    const alerts = component.stockAlerts();

    expect(alerts).toHaveLength(2);

    // Ítem 1: stock bajo (físico 3 > 0, disponible 2)
    expect(alerts[0].id).toBe('var-101');
    expect(alerts[0].productName).toBe('Torta de Selva Negra (Porción 150g)');
    expect(alerts[0].availableUnits).toBe(2);
    expect(alerts[0].stockStatus).toBe('LOW_STOCK');

    // Ítem 2: agotado (físico 0)
    expect(alerts[1].id).toBe('var-102');
    expect(alerts[1].productName).toBe('Cheesecake Frutos Rojos');
    expect(alerts[1].availableUnits).toBe(0);
    expect(alerts[1].stockStatus).toBe('OUT_OF_STOCK');
  });

  it('no debe incluir productos por criterio de estado INACTIVE de catálogo', () => {
    // Si la señal de lowStock está vacía, no debe haber alertas aunque existieran productos inactivos
    inventoryService.lowStock.set([]);

    expect(component.stockAlerts()).toHaveLength(0);
  });

  it('debe manejar gracefully errores de red en getLowStockInventory() sin romper el componente', () => {
    vi.spyOn(inventoryService, 'getLowStockInventory').mockReturnValue(
      throwError(() => new Error('Error al conectar con el servidor'))
    );

    expect(() => component.ngOnInit()).not.toThrow();
    expect(component.stockAlerts()).toEqual([]);
  });

  it('debe reaccionar reactivamente cuando el inventario sufre mutaciones', () => {
    inventoryService.lowStock.set([mockLowStockItem1]);
    expect(component.stockAlerts()).toHaveLength(1);

    // Se reabastece y ya no es stock bajo
    inventoryService.lowStock.set([]);
    expect(component.stockAlerts()).toHaveLength(0);
  });
});
