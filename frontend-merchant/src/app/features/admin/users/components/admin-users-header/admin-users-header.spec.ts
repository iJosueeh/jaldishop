import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUsersHeader } from './admin-users-header';

describe('AdminUsersHeader', () => {
  let component: AdminUsersHeader;
  let fixture: ComponentFixture<AdminUsersHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsersHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersHeader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('totalCount', 24);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y mostrar el conteo', () => {
    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(24);
  });

  it('debe emitir refresh al accionar el botón', () => {
    const spy = vi.spyOn(component.refresh, 'emit');
    component.refresh.emit();
    expect(spy).toHaveBeenCalled();
  });
});
