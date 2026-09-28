import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminMerchants } from './admin-merchants';
import { AdminService } from '../../../core/services/admin.service';
import { AdminUserSummary } from '../../../core/models/admin.models';
import { of } from 'rxjs';

describe('AdminMerchants', () => {
  let component: AdminMerchants;
  let fixture: ComponentFixture<AdminMerchants>;
  let adminServiceMock: any;

  const mockMerchants: AdminUserSummary[] = [
    {
      id: 'usr-1',
      email: 'valeria@store.com',
      firstName: 'Valeria',
      lastName: 'Ramos',
      fullName: 'Valeria Ramos',
      status: 'ACTIVE',
      roles: ['MERCHANT'],
      createdAt: '2026-09-01T10:00:00Z',
      updatedAt: '2026-09-01T10:00:00Z',
      storeName: 'Dulce Capri',
    },
  ];

  beforeEach(async () => {
    adminServiceMock = {
      users: vi.fn().mockReturnValue(mockMerchants),
      merchants: vi.fn().mockReturnValue(mockMerchants),
      metrics: vi.fn().mockReturnValue({ totalMerchants: 1 }),
      isLoadingUsers: vi.fn().mockReturnValue(false),
      loadUsers: vi.fn().mockReturnValue(of(mockMerchants)),
      suspendUser: vi.fn().mockReturnValue(of({ ...mockMerchants[0], status: 'SUSPENDED' })),
      activateUser: vi.fn().mockReturnValue(of({ ...mockMerchants[0], status: 'ACTIVE' })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminMerchants],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminMerchants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe abrir y cerrar el modal de estado para un comerciante', () => {
    component.onOpenStatusModal(mockMerchants[0]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.selectedMerchant()).toEqual(mockMerchants[0]);

    component.onCloseModal();
    expect(component.isModalOpen()).toBe(false);
    expect(component.selectedMerchant()).toBeNull();
  });
});
