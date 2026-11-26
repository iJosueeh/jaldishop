import { ApiException } from '@/core/api/apiClient';

/**
 * Maps ApiException and network errors into consistent Spanish user-facing messages
 * for authentication, registration, and email verification flows.
 */
export function mapAuthErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiException) {
    if (error.status === 429) {
      return (
        error.errorData?.message ||
        'Demasiados intentos. Por favor, espera unos minutos antes de volver a intentarlo.'
      );
    }
    if (error.status === 408 || error.status === 503) {
      return (
        error.errorData?.message ||
        'El servicio no se encuentra disponible temporalmente. Inténtalo nuevamente en unos instantes.'
      );
    }
    if (error.status === 400 || error.status === 401 || error.status === 403 || error.status === 409 || error.status === 422) {
      return error.errorData?.message || error.message || fallback;
    }
    return error.errorData?.message || error.message || fallback;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

/**
 * Masks an email address for safe UI confirmation (e.g., "juan.perez@correo.com" -> "ju***@correo.com").
 */
export function maskEmail(email: string): string {
  const trimmed = email.trim();
  const atIndex = trimmed.indexOf('@');
  if (atIndex <= 0) return trimmed;
  const local = trimmed.slice(0, atIndex);
  const domain = trimmed.slice(atIndex);
  const visible = local.slice(0, Math.min(2, local.length));
  return `${visible}***${domain}`;
}
