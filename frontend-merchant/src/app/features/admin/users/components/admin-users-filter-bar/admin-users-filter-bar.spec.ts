import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUsersFilterBar } from './admin-users-filter-bar';

describe('AdminUsersFilterBar', () => {
  let component: AdminUsersFilterBar;
  let fixture: ComponentFixture<AdminUsersFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsersFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersFilterBar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir roleChange al presionar una pill de rol', () => {
    const spy = vi.spyOn(component.roleChange, 'emit');
    component.roleChange.emit('MERCHANT');
    expect(spy).toHaveBeenCalledWith('MERCHANT');
  });

  it('debe emitir searchChange con string vacío en clearSearch()', () => {
    const spy = vi.spyOn(component.searchChange, 'emit');
    component.clearSearch();
    expect(spy).toHaveBeenCalledWith('');
  });
});
