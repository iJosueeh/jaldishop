import { env } from '../config/env';
import { ApiErrorResponse } from '../types/api.types';

export class ApiException extends Error {
  constructor(
    public readonly status: number,
    public readonly statusText: string,
    public readonly errorData?: ApiErrorResponse,
    message?: string
  ) {
    super(message || errorData?.message || `HTTP ${status}: ${statusText}`);
    this.name = 'ApiException';
  }
}

interface FetchOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
  timeoutMs?: number;
}

/**
 * Robust, typed HTTP client wrapper with unified error handling
 */
export async function apiClient<T>(
  endpoint: string,
  options: FetchOptions = {}
): Promise<T> {
  const { params, timeoutMs = 10000, headers, ...customConfig } = options;

  let url = endpoint.startsWith('http') ? endpoint : `${env.apiUrl}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  try {
    const response = await fetch(url, {
      ...customConfig,
      headers: {
        ...defaultHeaders,
        ...headers,
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      let errorData: ApiErrorResponse | undefined;
      try {
        errorData = await response.json();
      } catch {
        // Fallback when response is not JSON
      }
      throw new ApiException(response.status, response.statusText, errorData);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return (await response.json()) as T;
  } catch (error: unknown) {
    if (error instanceof ApiException) {
      throw error;
    }
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiException(408, 'Request Timeout', undefined, 'La solicitud al servidor excedió el tiempo límite.');
    }
    throw new ApiException(
      500,
      'Network Error',
      undefined,
      error instanceof Error ? error.message : 'Error inesperado de conexión con el servidor.'
    );
  } finally {
    clearTimeout(timeoutId);
  }
}
