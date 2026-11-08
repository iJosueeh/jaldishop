import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdersFilterBar } from './orders-filter-bar';

describe('OrdersFilterBar', () => {
  let component: OrdersFilterBar;
  let fixture: ComponentFixture<OrdersFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdersFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersFilterBar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir tabChange al seleccionar una pestaña', () => {
    let selected: any = null;
    component.tabChange.subscribe((tab) => {
      selected = tab;
    });

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[1].click(); // Tab "Nuevos" -> CONFIRMED

    expect(selected).toBe('CONFIRMED');
  });

  it('debe emitir searchChange al ingresar texto en el buscador', () => {
    let query = '';
    component.searchChange.subscribe((val) => {
      query = val;
    });

    const input = fixture.nativeElement.querySelector('input');
    input.value = 'Brownie';
    input.dispatchEvent(new Event('input'));

    expect(query).toBe('Brownie');
  });

  it('debe admitir conteos en formato string como "—" para pestañas inactivas', () => {
    fixture.componentRef.setInput('activeTab', 'ALL');
    fixture.componentRef.setInput('totalCount', 15);
    fixture.componentRef.setInput('confirmedCount', '—');
    fixture.componentRef.setInput('inPrepCount', '—');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('15');
    expect(compiled.textContent).toContain('—');
  });
});
