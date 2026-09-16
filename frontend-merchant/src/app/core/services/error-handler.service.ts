import { Service } from '@angular/core';
import { ApiError, NormalizedApiError } from '../models/api-error.models';
import { HttpErrorResponse } from '@angular/common/http';

@Service()
export class ErrorHandlerService {
  normalize(error: unknown): NormalizedApiError {
    if (!(error instanceof HttpErrorResponse)) {
      return {
        status: 0,
        code: 'UNKNOWN_ERROR',
        message: 'Ocurrio un error inesperado. Inténtalo nuevamente.',
        fieldErrors: {},
        timestamp: new Date(),
      };
    }

    if (error.status === 0) {
      return {
        status: 0,
        code: 'NETWORK_ERROR',
        message: 'No se pudo conectar con el servidor. Revisa tu conexión a internet.',
        fieldErrors: {},
        timestamp: new Date(),
      };
    }

    const apiError = this.extractApiError(error);

    return {
      status: error.status,
      code: apiError?.code ?? this.getDefaultCodeForStatus(error.status),
      message: apiError?.message ?? this.getDefaultMessageForStatus(error.status),
      fieldErrors: apiError?.errors ?? {},
      timestamp: new Date(),
    };
  }

  private extractApiError(error: HttpErrorResponse): ApiError | null {
    if (
      error.error &&
      typeof error.error === 'object' &&
      'code' in error.error &&
      'message' in error.error
    ) {
      return error.error as ApiError;
    }
    return null;
  }

  private getDefaultCodeForStatus(status: number): string {
    switch (status) {
      case 400:
        return 'VALIDATION_ERROR';
      case 401:
        return 'UNAUTHORIZED';
      case 403:
        return 'FORBIDDEN';
      case 404:
        return 'NOT_FOUND';
      case 409:
        return 'CONFLICT';
      case 422:
        return 'UNPROCESSABLE_ENTITY';
      case 500:
        return 'INTERNAL_SERVER_ERROR';
      default:
        return 'HTTP_ERROR';
    }
  }

  private getDefaultMessageForStatus(status: number): string {
    switch (status) {
      case 400:
        return 'La solicitud contiene datos inválidos o incompletos.';
      case 401:
        return 'Tu sesión no es válida o ha expirado. Ingresa nuevamente.';
      case 403:
        return 'No tienes permisos para realizar esta acción.';
      case 404:
        return 'El recurso solicitado no fue encontrado.';
      case 409:
        return 'Ya existe un registro con los mismos datos o hay un conflicto operativo.';
      case 422:
        return 'No se pudo procesar la solicitud debido a una regla de negocio.';
      case 500:
        return 'Ocurrió un error en el servidor. Nuestro equipo ya fue notificado.';
      default:
        return 'Ocurrió un error al procesar la solicitud.';
    }
  }
}
