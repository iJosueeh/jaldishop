import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryTable } from './inventory-table';
import { InventoryResponse } from '../../../../core/models/inventory.models';

describe('InventoryTable', () => {
  let component: InventoryTable;
  let fixture: ComponentFixture<InventoryTable>;

  const mockItemConfigured: InventoryResponse = {
    variantId: 'var-1',
    productId: 'prod-1',
    productName: 'Torta de Fresa',
    presentationName: '1 Kg',
    sku: 'FRE-01',
    tracksInventory: true,
    status: 'ACTIVE',
    inventoryStatus: 'CONFIGURED',
    physicalQuantity: 10,
    reservedQuantity: 2,
    availableQuantity: 8,
    lowStockThreshold: 4,
    isLowStock: false,
    updatedAt: '2026-11-16T12:00:00Z',
  };

  const mockItemNotConfigured: InventoryResponse = {
    variantId: 'var-2',
    productId: 'prod-2',
    productName: 'Brownie',
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
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryTable],
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryTable);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe mostrar filas de la tabla cuando hay items', () => {
    fixture.componentRef.setInput('items', [mockItemConfigured, mockItemNotConfigured]);
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);

    expect(fixture.nativeElement.textContent).toContain('Torta de Fresa');
    expect(fixture.nativeElement.textContent).toContain('Brownie');
    expect(fixture.nativeElement.textContent).toContain('Sin inicializar');
  });

  it('debe emitir editStock al hacer clic en el botón de stock', () => {
    fixture.componentRef.setInput('items', [mockItemConfigured]);
    fixture.detectChanges();

    const editSpy = vi.spyOn(component.editStock, 'emit');
    const button = fixture.nativeElement.querySelector('button[title="Ajustar cantidad física"]') as HTMLButtonElement;
    expect(button).toBeTruthy();

    button.click();
    expect(editSpy).toHaveBeenCalledWith(mockItemConfigured);
  });

  it('debe emitir editThreshold al hacer clic en el botón de umbral', () => {
    fixture.componentRef.setInput('items', [mockItemConfigured]);
    fixture.detectChanges();

    const editSpy = vi.spyOn(component.editThreshold, 'emit');
    const button = fixture.nativeElement.querySelector('button[title="Configurar umbral de stock bajo"]') as HTMLButtonElement;
    expect(button).toBeTruthy();

    button.click();
    expect(editSpy).toHaveBeenCalledWith(mockItemConfigured);
  });

  it('debe deshabilitar el botón de umbral si la variante no está configurada', () => {
    fixture.componentRef.setInput('items', [mockItemNotConfigured]);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button[title="Configurar umbral de stock bajo"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  it('debe mostrar estado de error y emitir retry al hacer clic en reintentar', () => {
    fixture.componentRef.setInput('items', []);
    fixture.componentRef.setInput('hasError', true);
    fixture.componentRef.setInput('errorMessage', 'Error 500');
    fixture.detectChanges();

    const retrySpy = vi.spyOn(component.retry, 'emit');
    const retryBtn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(retryBtn.textContent).toContain('Reintentar');

    retryBtn.click();
    expect(retrySpy).toHaveBeenCalled();
  });

  it('debe emitir editStock al hacer clic en cualquier parte de la fila para variantes con control de inventario', () => {
    fixture.componentRef.setInput('items', [mockItemConfigured]);
    fixture.detectChanges();

    const editSpy = vi.spyOn(component.editStock, 'emit');
    const row = fixture.nativeElement.querySelector('tbody tr') as HTMLTableRowElement;
    expect(row).toBeTruthy();

    row.click();
    expect(editSpy).toHaveBeenCalledWith(mockItemConfigured);
  });
});
