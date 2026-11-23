import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminAnalytics } from './admin-analytics';
import { AdminService } from '../../../core/services/admin.service';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminAnalyticsSummary } from '../../../core/models/admin.models';

describe('AdminAnalytics', () => {
  let component: AdminAnalytics;
  let fixture: ComponentFixture<AdminAnalytics>;
  let adminServiceMock: any;

  const mockAnalytics: AdminAnalyticsSummary = {
    period: { from: '2026-10-24', to: '2026-11-23' },
    commercial: {
      historicalConfirmedOrdersCount: 150,
      historicalConfirmedGmv: 45000,
      activeOrdersCount: 12,
      activeCommercialVolume: 3200,
      completedOrdersCount: 138,
      productsGrossSales: 42000,
      totalDiscounts: 500,
      totalDeliveryFees: 3500,
      cancelledOrdersCount: 5,
      cancelledOrdersVolume: 1200,
      averageTicket: 300,
    },
    financial: {
      grossInflowAmount: 45500,
      approvedPaymentsCount: 152,
      settledRefundsAmount: 500,
      settledRefundsCount: 2,
      netFinancialInflow: 45000,
      orderLinkedPaymentsAmount: 44000,
      orderLinkedPaymentsCount: 150,
      orphanPaymentsAmount: 1000,
      orphanPaymentsCount: 2,
      disclosure: 'Auditoría conforme',
    },
    storeRankings: [
      {
        storeId: 'store-1',
        storeName: 'Bodega Central',
        ordersCount: 80,
        totalGmv: 25000,
        averageTicket: 312.5,
      },
    ],
  };

  beforeEach(async () => {
    adminServiceMock = {
      analyticsSummary: signal<AdminAnalyticsSummary | null>(mockAnalytics),
      isLoadingAnalytics: signal<boolean>(false),
      isExportingPdf: signal<boolean>(false),
      stores: signal<any[]>([]),
      loadAnalyticsSummary: vi.fn().mockReturnValue(of(mockAnalytics)),
      exportAnalyticsPdf: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAnalytics],
      providers: [{ provide: AdminService, useValue: adminServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAnalytics);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente y computar series de datos', () => {
    expect(component).toBeTruthy();
    expect(component.selectedRange()).toBe('30D');
    expect(component.storeRankingItems().length).toBe(1);
    expect(component.storeRankingItems()[0].label).toBe('Bodega Central');
    expect(component.distributionSegments().length).toBe(3);
  });

  it('debe cambiar de rango temporal y recargar analíticas', () => {
    component.setRange('7D');
    expect(component.selectedRange()).toBe('7D');
    expect(adminServiceMock.loadAnalyticsSummary).toHaveBeenCalled();
  });

  it('debe delegar la exportación del PDF a AdminService', () => {
    component.exportPdf();
    expect(adminServiceMock.exportAnalyticsPdf).toHaveBeenCalled();
  });
});
