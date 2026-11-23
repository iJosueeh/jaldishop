import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { SettlementCard } from './settlement-card';
import { BillingService } from '../../../../core/services/billing.service';

describe('SettlementCard', () => {
  let component: SettlementCard;
  let fixture: ComponentFixture<SettlementCard>;
  let billingServiceMock: any;

  beforeEach(async () => {
    billingServiceMock = {
      availableBalance: signal(120.0),
      lockedBalance: signal(0.0),
      totalPaidOut: signal(500.0),
      minThreshold: signal(50.0),
      payoutDestination: signal({
        id: 'dest-1',
        payoutChannel: 'BANK_TRANSFER_CCI',
        bankName: 'BCP',
        accountCci: '00219400123456789012',
        accountHolderName: 'Restaurante Prueba',
      }),
      canRequestPayout: signal(true),
      isRequestingPayout: signal(false),
    };

    await TestBed.configureTestingModule({
      imports: [SettlementCard],
      providers: [
        { provide: BillingService, useValue: billingServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SettlementCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe mostrar el saldo disponible y habilitar el botón cuando el saldo supera el umbral', () => {
    expect(component.availableBalance()).toBe(120.0);
    expect(component.canRequest()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('S/ 120.00');

    const button = compiled.querySelector('button[type="button"]') as HTMLButtonElement;
    expect(button).toBeTruthy();
  });

  it('debe emitir openRequestModal al hacer clic en Solicitar Liquidación', () => {
    let emitted = false;
    component.openRequestModal.subscribe(() => {
      emitted = true;
    });

    component.onRequestPayout();
    expect(emitted).toBe(true);
  });
});
