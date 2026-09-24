import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomersTable } from './customers-table';
import { StoreCustomer } from '../../../../core/models/customer.models';

describe('CustomersTable', () => {
  let component: CustomersTable;
  let fixture: ComponentFixture<CustomersTable>;

  const mockCustomers: StoreCustomer[] = [
    {
      userId: 'usr-1',
      firstName: 'María',
      lastName: 'Gómez',
      email: 'maria@example.com',
      phone: '987654321',
      customerSince: '2026-01-01T00:00:00Z',
      ordersCount: 6,
      totalSpentAmount: 240.0,
      lastOrderAt: '2026-03-20T14:30:00Z',
    },
    {
      userId: 'usr-2',
      firstName: 'Carlos',
      lastName: 'Pérez',
      email: 'carlos@example.com',
      phone: null,
      customerSince: '2026-02-01T00:00:00Z',
      ordersCount: 1,
      totalSpentAmount: 25.0,
      lastOrderAt: null,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomersTable],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersTable);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('customers', mockCustomers);
    fixture.componentRef.setInput('isLoading', false);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render customer rows', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('María Gómez');
    expect(compiled.textContent).toContain('maria@example.com');
    expect(compiled.textContent).toContain('987654321');
    expect(compiled.textContent).toContain('VIP');
    expect(compiled.textContent).toContain('6 pedidos');
    expect(compiled.textContent).toContain('Carlos Pérez');
    expect(compiled.textContent).toContain('Nuevo');
    expect(compiled.textContent).toContain('Sin teléfono');
  });

  it('should compute initials accurately', () => {
    expect(component.getInitials('María', 'Gómez')).toBe('MG');
    expect(component.getInitials('', '')).toBe('CL');
  });

  it('should format WhatsApp link properly', () => {
    const link = component.getWhatsAppLink('987654321', 'María');
    expect(link).toContain('wa.me/51987654321');
    expect(link).toContain('Mar%C3%ADa');
  });

  it('should render empty state when no customers are provided', () => {
    fixture.componentRef.setInput('customers', []);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No se encontraron clientes');
  });
});
