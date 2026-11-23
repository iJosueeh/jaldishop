import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { InventoryHeader } from './inventory-header';

describe('InventoryHeader', () => {
  let component: InventoryHeader;
  let fixture: ComponentFixture<InventoryHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryHeader);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir refresh al hacer clic en el botón Actualizar', () => {
    const emitSpy = vi.spyOn(component.refresh, 'emit');

    const refreshButton = fixture.nativeElement.querySelector('button[aria-label="Actualizar inventario"]') as HTMLButtonElement;
    expect(refreshButton).toBeTruthy();

    refreshButton.click();
    expect(emitSpy).toHaveBeenCalled();
  });

  it('debe deshabilitar el botón si isLoading es true', () => {
    fixture.componentRef.setInput('isLoading', true);
    fixture.detectChanges();

    const refreshButton = fixture.nativeElement.querySelector('button[aria-label="Actualizar inventario"]') as HTMLButtonElement;
    expect(refreshButton.disabled).toBe(true);
  });
});
