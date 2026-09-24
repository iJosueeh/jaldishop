import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomersHeader } from './customers-header';
import { vi } from 'vitest';

describe('CustomersHeader', () => {
  let component: CustomersHeader;
  let fixture: ComponentFixture<CustomersHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomersHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersHeader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('totalCustomers', 12);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render title and customer count badge', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Gestión de Clientes');
    expect(compiled.textContent).toContain('12 Registrados');
  });

  it('should emit exportCsv when export button is clicked', () => {
    const emitSpy = vi.spyOn(component.exportCsv, 'emit');
    const button = fixture.nativeElement.querySelector('button');
    button.click();
    expect(emitSpy).toHaveBeenCalled();
  });
});
