import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsGrid } from './products-grid';
import { vi } from 'vitest';

describe('ProductsGrid', () => {
  let component: ProductsGrid;
  let fixture: ComponentFixture<ProductsGrid>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductsGrid],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsGrid);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display empty catalog state when isEmptyCatalog is true', () => {
    fixture.componentRef.setInput('isEmptyCatalog', true);
    fixture.detectChanges();

    const emptyText = fixture.nativeElement.querySelector('h3');
    expect(emptyText.textContent).toContain('Tu catálogo está vacío');
  });

  it('should display filter empty state when isFilterEmpty is true', () => {
    fixture.componentRef.setInput('isFilterEmpty', true);
    fixture.detectChanges();

    const emptyText = fixture.nativeElement.querySelector('h3');
    expect(emptyText.textContent).toContain('No se encontraron productos');
  });

  it('should forward viewInventory and openPreview outputs', () => {
    const invSpy = vi.fn();
    const prevSpy = vi.fn();
    component.viewInventory.subscribe(invSpy);
    component.openPreview.subscribe(prevSpy);

    const testProd = { id: 'p1' } as any;
    component.viewInventory.emit(testProd);
    component.openPreview.emit(testProd);

    expect(invSpy).toHaveBeenCalledWith(testProd);
    expect(prevSpy).toHaveBeenCalledWith(testProd);
  });
});
