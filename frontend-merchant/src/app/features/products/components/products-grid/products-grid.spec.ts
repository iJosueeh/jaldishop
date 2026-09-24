import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsGrid } from './products-grid';

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
});
