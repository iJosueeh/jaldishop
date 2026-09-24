import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsList } from './products-list';
import { Product } from '../../../../core/models/product.models';

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
});
