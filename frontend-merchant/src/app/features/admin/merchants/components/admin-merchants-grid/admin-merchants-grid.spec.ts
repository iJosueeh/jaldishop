import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminMerchantsGrid } from './admin-merchants-grid';
import { AdminUserSummary } from '../../../../../core/models/admin.models';

describe('AdminMerchantsGrid', () => {
  let component: AdminMerchantsGrid;
  let fixture: ComponentFixture<AdminMerchantsGrid>;

  const mockMerchants: AdminUserSummary[] = [
    {
      id: 'usr-1',
      email: 'valeria@store.com',
      firstName: 'Valeria',
      lastName: 'Ramos',
      fullName: 'Valeria Ramos',
      phone: '984552109',
      status: 'ACTIVE',
      roles: ['MERCHANT'],
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
      storeName: 'Dulce Capri',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMerchantsGrid],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMerchantsGrid);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('merchants', mockMerchants);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar tarjetas de comerciantes', () => {
    expect(component).toBeTruthy();
    expect(component.merchants().length).toBe(1);
  });

  it('debe emitir toggleStatus al hacer clic en suspender o reactivar', () => {
    const spy = vi.spyOn(component.toggleStatus, 'emit');
    component.toggleStatus.emit(mockMerchants[0]);
    expect(spy).toHaveBeenCalledWith(mockMerchants[0]);
  });
});
