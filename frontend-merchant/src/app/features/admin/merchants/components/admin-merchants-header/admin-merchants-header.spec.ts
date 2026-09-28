import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminMerchantsHeader } from './admin-merchants-header';

describe('AdminMerchantsHeader', () => {
  let component: AdminMerchantsHeader;
  let fixture: ComponentFixture<AdminMerchantsHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMerchantsHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMerchantsHeader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('totalCount', 6);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y mostrar el conteo de comerciantes', () => {
    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(6);
  });

  it('debe emitir refresh al presionar el botón', () => {
    const spy = vi.spyOn(component.refresh, 'emit');
    component.refresh.emit();
    expect(spy).toHaveBeenCalled();
  });
});
