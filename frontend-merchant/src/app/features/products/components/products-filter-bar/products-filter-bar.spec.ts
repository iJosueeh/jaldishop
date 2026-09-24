import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsFilterBar } from './products-filter-bar';

describe('ProductsFilterBar', () => {
  let component: ProductsFilterBar;
  let fixture: ComponentFixture<ProductsFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductsFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsFilterBar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit categoryChange when pill clicked', () => {
    const spy = vi.fn();
    component.categoryChange.subscribe(spy);

    fixture.componentRef.setInput('categories', [{ id: 'cat-1', name: 'Tortas', productCount: 4 }]);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    // Button 0 is 'Todos', Button 1 is 'Tortas'
    buttons[1].click();

    expect(spy).toHaveBeenCalledWith('cat-1');
  });

  it('should emit viewModeChange when grid/list toggle clicked', () => {
    const spy = vi.fn();
    component.viewModeChange.subscribe(spy);

    const toggleButtons = fixture.nativeElement.querySelectorAll('div button');
    // The list view button
    const listBtn = fixture.nativeElement.querySelector('button[title="Vista lista"]');
    listBtn.click();

    expect(spy).toHaveBeenCalledWith('list');
  });
});
