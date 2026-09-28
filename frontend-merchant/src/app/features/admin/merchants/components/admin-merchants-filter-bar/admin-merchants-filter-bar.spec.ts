import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminMerchantsFilterBar } from './admin-merchants-filter-bar';

describe('AdminMerchantsFilterBar', () => {
  let component: AdminMerchantsFilterBar;
  let fixture: ComponentFixture<AdminMerchantsFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMerchantsFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMerchantsFilterBar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir searchChange al ingresar texto en la búsqueda', () => {
    const spy = vi.spyOn(component.searchChange, 'emit');
    const inputElement = fixture.nativeElement.querySelector('input');
    inputElement.value = 'Carlos';
    inputElement.dispatchEvent(new Event('input'));
    expect(spy).toHaveBeenCalledWith('Carlos');
  });

  it('debe limpiar la búsqueda al llamar a clearSearch', () => {
    const spy = vi.spyOn(component.searchChange, 'emit');
    component.clearSearch();
    expect(spy).toHaveBeenCalledWith('');
  });

  it('debe emitir statusChange al cambiar el selector de estado', () => {
    const spy = vi.spyOn(component.statusChange, 'emit');
    const selectElement = fixture.nativeElement.querySelector('select');
    selectElement.value = 'ACTIVE';
    selectElement.dispatchEvent(new Event('change'));
    expect(spy).toHaveBeenCalledWith('ACTIVE');
  });
});
