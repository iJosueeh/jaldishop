import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsList } from './products-list';
import { Product } from '../../../../core/models/product.models';
import { vi } from 'vitest';

describe('ProductsList', () => {
  let component: ProductsList;
  let fixture: ComponentFixture<ProductsList>;

  const mockProduct: Product = {
    id: 'prod-1',
    storeId: 'store-1',
    name: 'Brownies Melcochosos',
    slug: 'brownies-melcochosos',
    description: 'Caja x 8',
    minPrice: 58.0,
    status: 'ACTIVE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductsList],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render product name and price in table row', () => {
    fixture.componentRef.setInput('products', [mockProduct]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Brownies Melcochosos');
    expect(text).toContain('S/ 58.00');
  });

  it('should show empty catalog state when isEmptyCatalog is true', () => {
    fixture.componentRef.setInput('isEmptyCatalog', true);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Tu catálogo está vacío');
  });

  it('should emit editProduct, openPreview and toggleStatus from row actions', () => {
    const editSpy = vi.fn();
    const prevSpy = vi.fn();
    const toggleSpy = vi.fn();
    component.editProduct.subscribe(editSpy);
    component.openPreview.subscribe(prevSpy);
    component.toggleStatus.subscribe(toggleSpy);

    component.onEdit(mockProduct);
    expect(editSpy).toHaveBeenCalledWith(mockProduct);

    component.onOpenPreview(mockProduct);
    expect(prevSpy).toHaveBeenCalledWith(mockProduct);

    component.onToggleStatus(mockProduct);
    expect(toggleSpy).toHaveBeenCalledWith(mockProduct);

    component.onRowClick(mockProduct);
    expect(editSpy).toHaveBeenCalledTimes(2);
  });
});
