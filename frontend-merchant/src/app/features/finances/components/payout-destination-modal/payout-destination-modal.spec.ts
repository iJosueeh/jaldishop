import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PayoutDestinationModal } from './payout-destination-modal';
import { BillingService } from '../../../../core/services/billing.service';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { of } from 'rxjs';

describe('PayoutDestinationModal', () => {
  let component: PayoutDestinationModal;
  let fixture: ComponentFixture<PayoutDestinationModal>;
  let mockBillingService: any;
  let mockStoreService: any;
  let mockProfileService: any;

  beforeEach(async () => {
    mockBillingService = {
      payoutDestination: vi.fn().mockReturnValue({
        payoutChannel: 'BANK_TRANSFER_CCI',
        bankName: 'BCP',
        accountCci: '00219400123456789012',
        accountNumber: '194-12345678-0-12',
        accountHolderName: 'Inversiones Jaldi SAC',
        accountHolderDocType: 'RUC',
        accountHolderDocNumber: '20601234567',
      }),
      updatePayoutDestination: vi.fn().mockReturnValue(of({})),
    };

    mockStoreService = {
      currentStore: vi.fn().mockReturnValue({
        id: 'store-1',
        name: 'Bodega Don Pepe',
        legalBusinessName: 'Inversiones Jaldi SAC',
        taxIdType: 'RUC_20',
        taxIdNumber: '20601234567',
        contactPhone: '987654321',
      }),
    };

    mockProfileService = {
      currentProfile: vi.fn().mockReturnValue({
        id: 'usr-1',
        firstName: 'José',
        lastName: 'Pérez Rodríguez',
        phone: '987654321',
      }),
      getMyProfile: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [PayoutDestinationModal],
      providers: [
        { provide: BillingService, useValue: mockBillingService },
        { provide: StoreService, useValue: mockStoreService },
        { provide: ProfileService, useValue: mockProfileService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PayoutDestinationModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe validar que el CCI contenga 20 dígitos', () => {
    component.channel.set('BANK_TRANSFER_CCI');
    component.accountHolderName.set('Juan Perez');
    component.docNumber.set('12345678');
    component.cci.set('12345'); // Inválido

    component.onSave();

    expect(component.fieldErrors()['accountCci']).toContain('20 dígitos');
    expect(component.generalError()).not.toBeNull();
    expect(mockBillingService.updatePayoutDestination).not.toHaveBeenCalled();
  });

  it('debe autocompletar con Razón Social si la tienda es RUC 20', () => {
    component.autofillFromProfile();

    expect(component.accountHolderName()).toBe('Inversiones Jaldi SAC');
    expect(component.docType()).toBe('RUC');
    expect(component.docNumber()).toBe('20601234567');
  });

  it('debe autocompletar con Nombre Completo del comerciante (NO nombre comercial) para Persona Natural', () => {
    mockStoreService.currentStore.mockReturnValue({
      id: 'store-2',
      name: 'Bodega Don Pepe',
      legalBusinessName: undefined,
      taxIdType: 'RUC_10',
      taxIdNumber: '10456789012',
    });

    component.autofillFromProfile();

    expect(component.accountHolderName()).toBe('José Pérez Rodríguez');
    expect(component.accountHolderName()).not.toBe('Bodega Don Pepe');
    expect(component.docType()).toBe('RUC');
    expect(component.docNumber()).toBe('10456789012');
  });

  it('debe autocompletar teléfono cuando el canal es YAPE_PHONE', () => {
    component.channel.set('YAPE_PHONE');
    component.phoneNumber.set('');

    component.autofillFromProfile();

    expect(component.phoneNumber()).toBe('987654321');
  });

  it('debe enviar payload correcto cuando los datos son válidos', () => {
    component.channel.set('BANK_TRANSFER_CCI');
    component.accountHolderName.set('José Pérez Rodríguez');
    component.docType.set('DNI');
    component.docNumber.set('12345678');
    component.cci.set('00219400123456789012');

    component.onSave();

    expect(component.generalError()).toBeNull();
    expect(mockBillingService.updatePayoutDestination).toHaveBeenCalledWith(
      expect.objectContaining({
        payoutChannel: 'BANK_TRANSFER_CCI',
        accountCci: '00219400123456789012',
        accountHolderName: 'José Pérez Rodríguez',
        accountHolderDocType: 'DNI',
        accountHolderDocNumber: '12345678',
      })
    );
  });
});


