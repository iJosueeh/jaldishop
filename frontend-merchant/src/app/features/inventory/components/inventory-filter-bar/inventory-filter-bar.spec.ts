import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryFilterBar } from './inventory-filter-bar';

describe('InventoryFilterBar', () => {
  let component: InventoryFilterBar;
  let fixture: ComponentFixture<InventoryFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryFilterBar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir searchChange al tipear en el buscador', () => {
    const emitSpy = vi.spyOn(component.searchChange, 'emit');

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'Chocolate';
    input.dispatchEvent(new Event('input'));

    expect(emitSpy).toHaveBeenCalledWith('Chocolate');
  });

  it('debe emitir filterChange al hacer clic en un chip de filtro', () => {
    const emitSpy = vi.spyOn(component.filterChange, 'emit');

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const lowStockBtn = Array.from(buttons).find((b) => b.textContent?.includes('Stock bajo'));
    expect(lowStockBtn).toBeTruthy();

    lowStockBtn?.click();
    expect(emitSpy).toHaveBeenCalledWith('LOW_STOCK');
  });
});
