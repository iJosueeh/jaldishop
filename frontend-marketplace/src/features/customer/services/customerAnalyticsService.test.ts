import { beforeEach, describe, expect, it, vi } from 'vitest';
import { customerAnalyticsService } from './customerAnalyticsService';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('customerAnalyticsService', () => {
  it('fetches analytics summary with from/to params', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          userId: 'u-1',
          period: { from: '2026-10-25', to: '2026-11-24', timeZone: 'America/Lima' },
          spending: {
            userId: 'u-1',
            totalOrdersCount: 3,
            totalSpentAmount: 120.5,
            totalRefundedAmount: 0,
            firstOrderAt: null,
            lastOrderAt: null,
          },
          topPurchasedProducts: [],
        }),
        { status: 200 }
      )
    );

    const summary = await customerAnalyticsService.getSummary({
      from: '2026-10-25',
      to: '2026-11-24',
    });

    expect(summary.spending?.totalOrdersCount).toBe(3);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/customer/analytics/summary?from=2026-10-25&to=2026-11-24',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('exports PDF blob and parses filename from Content-Disposition', async () => {
    const blobData = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    fetchMock.mockResolvedValue(
      new Response(blobData, {
        status: 200,
        headers: {
          'Content-Disposition': 'attachment; filename="reporte-clientes.pdf"',
        },
      })
    );

    const result = await customerAnalyticsService.exportPdf({
      from: '2026-10-25',
      to: '2026-11-24',
    });

    expect(result.filename).toBe('reporte-clientes.pdf');
    expect(result.blob).toBeInstanceOf(Blob);
  });
});
