import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminStoresTable } from './admin-stores-table';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';

describe('AdminStoresTable', () => {
  let component: AdminStoresTable;
  let fixture: ComponentFixture<AdminStoresTable>;

  const mockStores: AdminStoreSummary[] = [
    {
      id: 'str-1',
      merchantUserId: 'usr-1',
      name: 'Dulce Capri',
      slug: 'dulce-capri',
      status: 'ACTIVE',
      deliveryEnabled: true,
      pickupEnabled: true,
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
      merchant: {
        id: 'usr-1',
        email: 'v@s.com',
        fullName: 'Valeria Ramos',
        status: 'ACTIVE',
      },
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminStoresTable],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminStoresTable);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('stores', mockStores);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar tiendas', () => {
    expect(component).toBeTruthy();
    expect(component.stores().length).toBe(1);
  });

  it('debe emitir toggleStatus al hacer clic en suspender o reactivar', () => {
    const spy = vi.spyOn(component.toggleStatus, 'emit');
    component.toggleStatus.emit(mockStores[0]);
    expect(spy).toHaveBeenCalledWith(mockStores[0]);
  });
});
