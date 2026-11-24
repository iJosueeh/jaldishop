import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { AnalyticsView } from './AnalyticsView';

const mockDownloadPdf = vi.fn();
const mockReload = vi.fn();
const mockUseCustomerAnalytics = vi.fn();

vi.mock('../hooks/useCustomerAnalytics', () => ({
  useCustomerAnalytics: (params: unknown) => mockUseCustomerAnalytics(params),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('AnalyticsView Component', () => {
  it('renders KPIs, top purchased products, and triggers PDF export', () => {
    mockUseCustomerAnalytics.mockReturnValue({
      summary: {
        userId: 'u-1',
        period: { from: '2026-10-25', to: '2026-11-24', timeZone: 'America/Lima' },
        spending: {
          userId: 'u-1',
          totalOrdersCount: 5,
          totalSpentAmount: 240.0,
          totalRefundedAmount: 20.0,
          firstOrderAt: '2026-10-26T12:00:00Z',
          lastOrderAt: '2026-11-22T12:00:00Z',
        },
        topPurchasedProducts: [
          {
            variantId: 'v-1',
            productId: 'p-1',
            productName: 'Pan de Masa Madre',
            variantName: 'Hogaza 750g',
            unitsSold: 4,
            totalRevenue: 88.0,
          },
        ],
      },
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      downloadPdf: mockDownloadPdf,
      isDownloadingPdf: false,
    });

    render(<AnalyticsView />);

    expect(screen.getByText('Mis Estadísticas de Compras')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getAllByText('Pan de Masa Madre').length).toBeGreaterThanOrEqual(1);

    fireEvent.click(screen.getByRole('button', { name: /Exportar PDF/i }));
    expect(mockDownloadPdf).toHaveBeenCalledTimes(1);
  });

  it('renders empty state when customer has no purchases in period', () => {
    mockUseCustomerAnalytics.mockReturnValue({
      summary: {
        userId: 'u-1',
        period: { from: '2026-10-25', to: '2026-11-24', timeZone: 'America/Lima' },
        spending: {
          userId: 'u-1',
          totalOrdersCount: 0,
          totalSpentAmount: 0,
          totalRefundedAmount: 0,
          firstOrderAt: null,
          lastOrderAt: null,
        },
        topPurchasedProducts: [],
      },
      isLoading: false,
      isFetching: false,
      error: null,
      reload: mockReload,
      downloadPdf: mockDownloadPdf,
      isDownloadingPdf: false,
    });

    render(<AnalyticsView />);
    expect(screen.getByText(/No registras compras en este período/i)).toBeInTheDocument();
  });
});
