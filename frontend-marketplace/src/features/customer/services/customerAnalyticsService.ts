import { CustomerAnalyticsSummary } from '../types/customer.types';

export interface AnalyticsDateRangeParams {
  from?: string;
  to?: string;
}

function buildAnalyticsQuery(params?: AnalyticsDateRangeParams): string {
  const query = new URLSearchParams();
  if (params?.from) query.set('from', params.from);
  if (params?.to) query.set('to', params.to);
  const qs = query.toString();
  return qs ? `?${qs}` : '';
}

function parseFilenameFromDisposition(disposition: string | null): string {
  if (!disposition) return 'jaldishop-mis-estadisticas.pdf';
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  return match?.[1] || 'jaldishop-mis-estadisticas.pdf';
}

export const customerAnalyticsService = {
  async getSummary(params?: AnalyticsDateRangeParams): Promise<CustomerAnalyticsSummary> {
    const res = await fetch(`/api/customer/analytics/summary${buildAnalyticsQuery(params)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No pudimos cargar tus estadísticas de compras.');
    }

    return res.json();
  },

  async exportPdf(params?: AnalyticsDateRangeParams): Promise<{ blob: Blob; filename: string }> {
    const res = await fetch(`/api/customer/analytics/export/pdf${buildAnalyticsQuery(params)}`, {
      method: 'GET',
      headers: {
        Accept: 'application/pdf',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No pudimos exportar tu reporte PDF.');
    }

    const blob = await res.blob();
    const filename = parseFilenameFromDisposition(res.headers.get('Content-Disposition'));
    return { blob, filename };
  },
};
