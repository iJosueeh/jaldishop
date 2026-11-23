import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryStockDrawer } from './inventory-stock-drawer';
import { InventoryService } from '../../../../core/services/inventory.service';
import { InventoryResponse } from '../../../../core/models/inventory.models';

describe('InventoryStockDrawer', () => {
  let component: InventoryStockDrawer;
  let fixture: ComponentFixture<InventoryStockDrawer>;
  let inventoryService: InventoryService;

  const mockVariant: InventoryResponse = {
    variantId: 'var-123',
    productId: 'prod-1',
    productName: 'Torta Tres Leches',
    presentationName: 'Entera',
    sku: 'TTL-01',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 10,
    reservedQuantity: 3,
    availableQuantity: 7,
    lowStockThreshold: 4,
    isLowStock: false,
    updatedAt: '2026-11-16T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryStockDrawer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InventoryService,
      ],
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    fixture = TestBed.createComponent(InventoryStockDrawer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe inicializar la cantidad con el valor del variant input', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    expect(component.quantity()).toBe(10);
    expect(component.validationError()).toBeNull();
  });

  it('debe validar que la cantidad no sea menor que las reservas activas', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '2'; // menor a reservedQuantity = 3
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.validationError()).toContain('mínimo 3');
  });

  it('debe validar que la cantidad sea un entero no negativo', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '-5';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.validationError()).toContain('número entero mayor o igual a 0');
  });

  it('debe llamar a inventoryService.updateStock y emitir stockSaved y close al completar', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    const updatedVariant = { ...mockVariant, physicalQuantity: 15, availableQuantity: 12 };
    const updateSpy = vi.spyOn(inventoryService, 'updateStock').mockReturnValue(of(updatedVariant));
    const savedSpy = vi.spyOn(component.stockSaved, 'emit');
    const closeSpy = vi.spyOn(component.close, 'emit');

    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '15';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    component.onSubmit();

    expect(updateSpy).toHaveBeenCalledWith('var-123', 15);
    expect(savedSpy).toHaveBeenCalledWith(updatedVariant);
    expect(closeSpy).toHaveBeenCalled();
  });

  it('debe manejar error de negocio y mostrar mensaje al fallar el guardado', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    vi.spyOn(inventoryService, 'updateStock').mockReturnValue(
      throwError(() => ({
        status: 409,
        error: { code: 'CANNOT_REDUCE_BELOW_RESERVED', message: 'Conflicto de reservas' },
      }))
    );

    component.quantity.set(3);
    component.onSubmit();

    expect(component.serverError()).toContain('No se puede reducir la cantidad física por debajo de las órdenes activas');
    expect(component.isSaving()).toBe(false);
  });

  it('debe validar que la cantidad no supere el límite máximo permitido', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '100000';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.validationError()).toContain('máxima permitida es 99,999');
  });

  it('debe aplicar deltas positivos y negativos respetando límites', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant); // physical: 10, reserved: 3
    fixture.detectChanges();

    component.applyDelta(5);
    expect(component.quantity()).toBe(15);
    expect(component.availablePreview()).toBe(12);

    // No debe bajar de reservedQuantity (3)
    component.applyDelta(-20);
    expect(component.quantity()).toBe(3);
    expect(component.availablePreview()).toBe(0);
  });

  it('debe prevenir doble envío si ya está guardando', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariant);
    fixture.detectChanges();

    const updateSpy = vi.spyOn(inventoryService, 'updateStock').mockReturnValue(of(mockVariant));
    component.isSaving.set(true);

    component.onSubmit();

    expect(updateSpy).not.toHaveBeenCalled();
  });
});
