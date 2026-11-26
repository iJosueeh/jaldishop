import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { StoreTaxCard } from './store-tax-card';
import { StoreService } from '../../../../core/services/store.service';
import { ToastService } from '../../../../core/services/toast.service';
import { StoreResponse } from '../../../../core/models/store.models';

describe('StoreTaxCard', () => {
  let component: StoreTaxCard;
  let fixture: ComponentFixture<StoreTaxCard>;
  let storeServiceMock: any;
  let toastMock: any;

  const mockStore: StoreResponse = {
    id: 'store-1',
    merchantUserId: 'user-1',
    name: 'Sabor Criollo',
    slug: 'sabor-criollo',
    pickupEnabled: true,
    deliveryEnabled: true,
    status: 'ACTIVE',
    legalBusinessName: 'Sabor Criollo E.I.R.L.',
    taxIdType: 'RUC_10',
    taxIdNumber: '10456789019',
    taxRegime: 'RUS',
    canIssueInvoices: false,
    createdAt: '2026-11-21T00:00:00Z',
    updatedAt: '2026-11-21T00:00:00Z',
  };

  beforeEach(async () => {
    storeServiceMock = {
      currentStore: vi.fn().mockReturnValue(mockStore),
      updateTaxProfile: vi.fn().mockReturnValue(of({ ...mockStore })),
    };

    toastMock = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [StoreTaxCard],
      providers: [
        { provide: StoreService, useValue: storeServiceMock },
        { provide: ToastService, useValue: toastMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreTaxCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializar el formulario con los datos de la tienda y bloquear facturas bajo Nuevo RUS', () => {
    expect(component.taxForm.get('taxRegime')?.value).toBe('RUS');
    expect(component.taxForm.get('canIssueInvoices')?.value).toBe(false);
    expect(component.taxForm.get('canIssueInvoices')?.disabled).toBe(true);
    expect(component.isRusActive()).toBe(true);
  });

  it('debe habilitar el selector de facturas al cambiar a Régimen MYPE Tributario', () => {
    component.taxForm.get('taxRegime')?.setValue('MYPE');
    fixture.detectChanges();

    expect(component.isRusActive()).toBe(false);
    expect(component.taxForm.get('canIssueInvoices')?.enabled).toBe(true);

    // Puede activar facturas
    component.taxForm.get('canIssueInvoices')?.setValue(true);
    expect(component.taxForm.get('canIssueInvoices')?.value).toBe(true);
  });

  it('debe forzar canIssueInvoices a false si el comerciante vuelve a seleccionar Nuevo RUS', () => {
    // Primero en MYPE
    component.taxForm.get('taxRegime')?.setValue('MYPE');
    component.taxForm.get('canIssueInvoices')?.setValue(true);
    fixture.detectChanges();

    // Vuelve a RUS
    component.taxForm.get('taxRegime')?.setValue('RUS');
    fixture.detectChanges();

    expect(component.isRusActive()).toBe(true);
    expect(component.taxForm.get('canIssueInvoices')?.value).toBe(false);
    expect(component.taxForm.get('canIssueInvoices')?.disabled).toBe(true);
  });
});
