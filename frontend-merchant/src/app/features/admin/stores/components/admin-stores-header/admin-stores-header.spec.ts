import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStoresHeader } from './admin-stores-header';

describe('AdminStoresHeader', () => {
  let component: AdminStoresHeader;
  let fixture: ComponentFixture<AdminStoresHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminStoresHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminStoresHeader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('totalCount', 8);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y mostrar el total de tiendas', () => {
    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(8);
  });

  it('debe emitir refresh al presionar el botón', () => {
    const spy = vi.spyOn(component.refresh, 'emit');
    component.refresh.emit();
    expect(spy).toHaveBeenCalled();
  });
});
