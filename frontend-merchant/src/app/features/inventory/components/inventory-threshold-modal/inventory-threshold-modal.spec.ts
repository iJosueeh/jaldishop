import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryThresholdModal } from './inventory-threshold-modal';
import { InventoryService } from '../../../../core/services/inventory.service';
import { InventoryResponse } from '../../../../core/models/inventory.models';

describe('InventoryThresholdModal', () => {
  let component: InventoryThresholdModal;
  let fixture: ComponentFixture<InventoryThresholdModal>;
  let inventoryService: InventoryService;

  const mockVariantWithThreshold: InventoryResponse = {
    variantId: 'var-123',
    productId: 'prod-1',
    productName: 'Torta Tres Leches',
    presentationName: 'Entera',
    sku: 'TTL-01',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 10,
    reservedQuantity: 0,
    availableQuantity: 10,
    lowStockThreshold: 4,
    isLowStock: false,
    updatedAt: '2026-11-16T10:00:00Z',
  };

  const mockVariantWithoutThreshold: InventoryResponse = {
    ...mockVariantWithThreshold,
    lowStockThreshold: null,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryThresholdModal],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        InventoryService,
      ],
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    fixture = TestBed.createComponent(InventoryThresholdModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe inicializarse habilitado con el valor de lowStockThreshold existente', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithThreshold);
    fixture.detectChanges();

    expect(component.isEnabled()).toBe(true);
    expect(component.thresholdValue()).toBe(4);
  });

  it('debe inicializarse deshabilitado si lowStockThreshold es null', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithoutThreshold);
    fixture.detectChanges();

    expect(component.isEnabled()).toBe(false);
  });

  it('debe enviar null si isEnabled es false', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithThreshold);
    fixture.detectChanges();

    const updateSpy = vi.spyOn(inventoryService, 'updateThreshold').mockReturnValue(of({ ...mockVariantWithThreshold, lowStockThreshold: null }));
    const savedSpy = vi.spyOn(component.thresholdSaved, 'emit');
    const closeSpy = vi.spyOn(component.close, 'emit');

    component.isEnabled.set(false);
    component.onSubmit();

    expect(updateSpy).toHaveBeenCalledWith('var-123', null);
    expect(savedSpy).toHaveBeenCalled();
    expect(closeSpy).toHaveBeenCalled();
  });

  it('debe enviar el valor numérico entero si isEnabled es true', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithoutThreshold);
    fixture.detectChanges();

    const updateSpy = vi.spyOn(inventoryService, 'updateThreshold').mockReturnValue(of({ ...mockVariantWithThreshold, lowStockThreshold: 8 }));

    component.isEnabled.set(true);
    component.thresholdValue.set(8);
    component.onSubmit();

    expect(updateSpy).toHaveBeenCalledWith('var-123', 8);
  });

  it('debe permitir valor 0 como umbral válido (distinto de null)', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithoutThreshold);
    fixture.detectChanges();

    const updateSpy = vi.spyOn(inventoryService, 'updateThreshold').mockReturnValue(of({ ...mockVariantWithThreshold, lowStockThreshold: 0 }));

    component.isEnabled.set(true);
    component.thresholdValue.set(0);
    component.onSubmit();

    expect(updateSpy).toHaveBeenCalledWith('var-123', 0);
  });

  it('debe validar que el umbral no supere el límite máximo de 9,999 unidades', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithThreshold);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input[type="number"]') as HTMLInputElement;
    input.value = '15000';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.validationError()).toContain('máximo permitido es 9,999');
  });

  it('debe asignar valores sugeridos correctamente con setPreset', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithoutThreshold);
    fixture.detectChanges();

    component.setPreset(10);
    expect(component.isEnabled()).toBe(true);
    expect(component.thresholdValue()).toBe(10);
    expect(component.validationError()).toBeNull();
  });

  it('debe calcular isLowStockPreview comparando con physicalQuantity', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithThreshold); // physicalQuantity = 10
    fixture.detectChanges();

    component.isEnabled.set(true);
    component.thresholdValue.set(5);
    expect(component.isLowStockPreview()).toBe(false); // 10 > 5

    component.thresholdValue.set(10);
    expect(component.isLowStockPreview()).toBe(true); // 10 <= 10

    component.thresholdValue.set(15);
    expect(component.isLowStockPreview()).toBe(true); // 10 <= 15
  });

  it('debe manejar error INVENTORY_NOT_CONFIGURED', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('variant', mockVariantWithoutThreshold);
    fixture.detectChanges();

    vi.spyOn(inventoryService, 'updateThreshold').mockReturnValue(
      throwError(() => ({
        status: 409,
        error: { code: 'INVENTORY_NOT_CONFIGURED', message: 'No configurado' },
      }))
    );

    component.isEnabled.set(true);
    component.thresholdValue.set(5);
    component.onSubmit();

    expect(component.serverError()).toContain('no tiene stock inicializado');
    expect(component.isSaving()).toBe(false);
  });
});
