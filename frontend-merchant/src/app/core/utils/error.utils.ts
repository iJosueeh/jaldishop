export interface ApiErrorResponse {
  code?: string;
  message?: string;
  errors?: Record<string, string>;
}

export interface ExtractedApiError {
  summary: string;
  fieldErrors: Record<string, string>;
}

/**
 * Extrae mensajes de error amigables y mapa de errores por campo devueltos por Spring Boot (GlobalExceptionHandler)
 */
export function extractApiErrorMessage(err: any, fallback = 'Ocurrió un error inesperado al procesar la solicitud.'): ExtractedApiError {
  if (!err) {
    return { summary: fallback, fieldErrors: {} };
  }

  const errorObj = err.error as ApiErrorResponse;
  if (!errorObj) {
    const rawMsg = err.message || fallback;
    return { summary: rawMsg, fieldErrors: {} };
  }

  const summary = errorObj.message || fallback;
  const fieldErrors = errorObj.errors || {};

  return { summary, fieldErrors };
}
