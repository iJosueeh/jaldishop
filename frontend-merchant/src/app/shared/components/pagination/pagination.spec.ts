import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Pagination } from './pagination';
import { ComponentRef } from '@angular/core';

describe('Pagination', () => {
  let component: Pagination;
  let fixture: ComponentFixture<Pagination>;
  let componentRef: ComponentRef<Pagination>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Pagination],
    }).compileComponents();

    fixture = TestBed.createComponent(Pagination);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe calcular totalPages y rangos correctamente', () => {
    componentRef.setInput('totalItems', 25);
    componentRef.setInput('pageSize', 10);
    componentRef.setInput('currentPage', 2);
    fixture.detectChanges();

    expect(component.totalPages()).toBe(3);
    expect(component.startItem()).toBe(11);
    expect(component.endItem()).toBe(20);
    expect(component.hasPrevious()).toBe(true);
    expect(component.hasNext()).toBe(true);
  });

  it('debe emitir pageChange al seleccionar una página', () => {
    componentRef.setInput('totalItems', 30);
    componentRef.setInput('pageSize', 10);
    componentRef.setInput('currentPage', 1);
    fixture.detectChanges();

    const spy = vi.fn();
    component.pageChange.subscribe(spy);

    component.onPageSelect(2);
    expect(spy).toHaveBeenCalledWith(2);
  });

  it('debe emitir página anterior y siguiente adecuadamente', () => {
    componentRef.setInput('totalItems', 30);
    componentRef.setInput('pageSize', 10);
    componentRef.setInput('currentPage', 2);
    fixture.detectChanges();

    const spy = vi.fn();
    component.pageChange.subscribe(spy);

    component.onPrevious();
    expect(spy).toHaveBeenCalledWith(1);

    component.onNext();
    expect(spy).toHaveBeenCalledWith(3);
  });

  it('no debe emitir si intenta avanzar más allá del límite o retroceder de la primera página', () => {
    componentRef.setInput('totalItems', 10);
    componentRef.setInput('pageSize', 10);
    componentRef.setInput('currentPage', 1);
    fixture.detectChanges();

    const spy = vi.fn();
    component.pageChange.subscribe(spy);

    component.onPrevious();
    expect(spy).not.toHaveBeenCalled();

    component.onNext();
    expect(spy).not.toHaveBeenCalled();
  });
});
