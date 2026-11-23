import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductCard } from './product-card';
import { Product } from '../../../../../core/models/product.models';
import { vi } from 'vitest';

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
    quotaToday: { current: 3, total: 10 },
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

  it('should calculate dynamic capacity bars based on quota', () => {
    const bars = component.capacityBars();
    expect(bars.length).toBe(4);
    // 3 de 10 es ratio 0.3 -> filled = round(0.3 * 4) = 1
    expect(bars[0].active).toBe(true);
    expect(bars[1].active).toBe(false);
  });

  it('should emit toggleStatus when visibility button is clicked', () => {
    const spy = vi.fn();
    component.toggleStatus.subscribe(spy);

    const toggleBtn = fixture.nativeElement.querySelector('button[title="Pausar producto"]');
    expect(toggleBtn).toBeTruthy();
    toggleBtn.click();

    expect(spy).toHaveBeenCalledWith(mockProduct);
  });

  it('should emit editProduct when card or edit button is clicked', () => {
    const spy = vi.fn();
    component.editProduct.subscribe(spy);

    const editBtn = fixture.nativeElement.querySelector('button[title="Editar producto, precio y detalles"]');
    expect(editBtn).toBeTruthy();
    editBtn.click();

    expect(spy).toHaveBeenCalledWith(mockProduct);
  });

  it('should emit openPreview when catalog preview button is clicked', () => {
    const prevSpy = vi.fn();
    component.openPreview.subscribe(prevSpy);

    const previewBtn = fixture.nativeElement.querySelector('button[title="Ver en Catálogo Web"]');
    expect(previewBtn).toBeTruthy();
    previewBtn.click();

    expect(prevSpy).toHaveBeenCalledWith(mockProduct);
  });
});
