import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStoresFilterBar } from './admin-stores-filter-bar';

describe('AdminStoresFilterBar', () => {
  let component: AdminStoresFilterBar;
  let fixture: ComponentFixture<AdminStoresFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminStoresFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminStoresFilterBar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir searchChange al limpiar la búsqueda', () => {
    const spy = vi.spyOn(component.searchChange, 'emit');
    component.clearSearch();
    expect(spy).toHaveBeenCalledWith('');
  });
});
