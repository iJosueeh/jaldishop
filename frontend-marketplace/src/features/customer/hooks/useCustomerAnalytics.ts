'use client';

import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  AnalyticsDateRangeParams,
  customerAnalyticsService,
} from '../services/customerAnalyticsService';

export const CUSTOMER_ANALYTICS_QUERY_KEY = ['customer', 'analytics'] as const;

export function useCustomerAnalytics(params: AnalyticsDateRangeParams = {}) {
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  const query = useQuery({
    queryKey: [...CUSTOMER_ANALYTICS_QUERY_KEY, params.from ?? '', params.to ?? ''],
    queryFn: () => customerAnalyticsService.getSummary(params),
    staleTime: 60_000,
    retry: 1,
  });

  const downloadPdf = useCallback(async () => {
    if (isDownloadingPdf) return;
    setIsDownloadingPdf(true);
    try {
      const { blob, filename } = await customerAnalyticsService.exportPdf(params);
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Reporte PDF descargado.');
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'No pudimos descargar el reporte PDF.';
      toast.error(message);
    } finally {
      setIsDownloadingPdf(false);
    }
  }, [isDownloadingPdf, params]);

  return {
    summary: query.data ?? null,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error instanceof Error ? query.error.message : query.error ? 'Error al cargar estadísticas' : null,
    reload: query.refetch,
    downloadPdf,
    isDownloadingPdf,
  };
}
