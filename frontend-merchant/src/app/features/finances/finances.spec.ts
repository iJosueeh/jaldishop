import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Finances } from './finances';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ProfileService } from '../../core/services/profile.service';
import { MerchantAnalyticsSummaryResponse } from '../../core/models/analytics.models';

describe('Finances Component', () => {
  let component: Finances;
  let fixture: ComponentFixture<Finances>;
  let analyticsService: AnalyticsService;

  const mockReport: MerchantAnalyticsSummaryResponse = {
    storeId: 'store-1',
    period: {
      from: '2026-10-21',
      to: '2026-11-20',
      timeZone: 'America/Lima',
    },
    commercial: {
      historicalConfirmedOrdersCount: 50,
      historicalConfirmedGmv: 2500,
      activeOrdersCount: 2,
      activeCommercialVolume: 100,
      completedOrdersCount: 45,
      productsGrossSales: 2200,
      totalDiscounts: 20,
      totalDeliveryFees: 150,
      cancelledOrdersCount: 3,
      cancelledOrdersVolume: 150,
      averageTicket: 48.88,
    },
    financial: {
      grossInflowAmount: 2500,
      approvedPaymentsCount: 50,
      settledRefundsAmount: 150,
      settledRefundsCount: 3,
      netFinancialInflow: 2350,
      orderLinkedPaymentsAmount: 2500,
      orderLinkedPaymentsCount: 50,
      orphanPaymentsAmount: 0,
      orphanPaymentsCount: 0,
      disclosure: 'Descargo legal de Mercado Pago',
    },
    topProducts: [
      {
        variantId: 'v-1',
        productId: 'p-1',
        productName: 'Alfajores Artesanales',
        variantName: 'Caja x 12',
        unitsSold: 20,
        totalRevenue: 600,
      },
    ],
    capacityDemand: {
      committedReservationsCount: 48,
      releasedReservationsCount: 3,
      notes: null,
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Finances],
      providers: [
        AnalyticsService,
        ProfileService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Finances);
    component = fixture.componentInstance;
    analyticsService = TestBed.inject(AnalyticsService);
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe invocar loadSummary en ngOnInit cuando summary() es nulo', () => {
    const loadSpy = vi.spyOn(analyticsService, 'loadSummary').mockReturnValue(of(mockReport));

    component.ngOnInit();

    expect(loadSpy).toHaveBeenCalled();
  });

  it('debe renderizar KPIs y desglose cuando summary() tiene datos', () => {
    analyticsService.summary.set(mockReport);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Finanzas & Métricas');
    expect(compiled.textContent).toContain('Ingresos Netos (Pasarela)');
    expect(compiled.textContent).toContain('S/ 2,350.00');
    expect(compiled.textContent).toContain('Alfajores Artesanales');
  });

  it('debe mostrar estado de error y botón de reintento ante fallo inicial', () => {
    const loadSpy = vi.spyOn(analyticsService, 'loadSummary').mockReturnValue(of(mockReport));
    fixture.detectChanges(); // Ejecuta ngOnInit inicial

    analyticsService.summary.set(null);
    analyticsService.error.set('No se pudo conectar con el servidor');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Error al cargar datos financieros');
    expect(compiled.textContent).toContain('No se pudo conectar con el servidor');

    component.retry();
    expect(loadSpy).toHaveBeenCalled();
  });

  it('debe reinvocar loadSummary en ngOnInit cuando analyticsService.isOutdated() es true aunque summary() tenga datos', () => {
    analyticsService.summary.set(mockReport);
    analyticsService.isOutdated.set(true);

    const loadSpy = vi.spyOn(analyticsService, 'loadSummary').mockReturnValue(of(mockReport));

    component.ngOnInit();

    expect(loadSpy).toHaveBeenCalled();
  });

  it('debe cerrar el modal de destino y recargar el balance de billing al cerrarse', () => {
    const billingSpy = vi.spyOn(component.billingService, 'loadSummarySilently');
    component.isDestinationModalOpen.set(true);

    component.onDestinationModalClosed();

    expect(component.isDestinationModalOpen()).toBe(false);
    expect(billingSpy).toHaveBeenCalled();
  });
});
