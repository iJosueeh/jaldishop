import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductCard } from './product-card';
import { Product } from '../../../../../core/models/product.models';

describe('ProductCard', () => {
  let component: ProductCard;
  let fixture: ComponentFixture<ProductCard>;

  const mockProduct: Product = {
    id: 'prod-1',
    storeId: 'store-1',
    name: 'Brownies Melcochosos',
    slug: 'brownies-melcochosos',
    description: 'Caja x 8 unidades',
    minPrice: 58.0,
    status: 'ACTIVE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('product', mockProduct);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display formatted price', () => {
    expect(component.displayPrice()).toBe('S/ 58.00');
  });

  it('should emit toggleStatus when visibility button is clicked', () => {
    const spy = vi.fn();
    component.toggleStatus.subscribe(spy);

    const toggleBtn = fixture.nativeElement.querySelector('button[title="Pausar producto"]');
    expect(toggleBtn).toBeTruthy();
    toggleBtn.click();

    expect(spy).toHaveBeenCalledWith(mockProduct);
  });
});
