import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductsAlertBanner } from './products-alert-banner';

describe('ProductsAlertBanner', () => {
  let component: ProductsAlertBanner;
  let fixture: ComponentFixture<ProductsAlertBanner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductsAlertBanner],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsAlertBanner);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit viewDetails when clicked', () => {
    fixture.componentRef.setInput('attentionCount', 2);
    fixture.detectChanges();

    const spy = vi.fn();
    component.viewDetails.subscribe(spy);

    const button = fixture.nativeElement.querySelector('button');
    expect(button).toBeTruthy();
    button.click();

    expect(spy).toHaveBeenCalled();
  });
});
