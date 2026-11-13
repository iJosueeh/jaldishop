'use client';

import React, { useEffect, useState, useCallback, useRef } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import {
  CheckCircle2,
  Clock,
  AlertCircle,
  XCircle,
  HelpCircle,
  RefreshCw,
  ShoppingBag,
  ArrowRight,
  ExternalLink,
  ShieldCheck,
  RotateCcw,
} from 'lucide-react';
import { Card } from '@/shared/components/ui/Card';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import {
  checkoutService,
  getPendingPayment,
  clearPendingPayment,
  PaymentServiceError,
} from '../services/checkoutService';
import {
  CustomerPaymentStatusResponse,
  PendingPaymentCorrelation,
} from '../types/checkout.types';

export const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
export const ATTEMPT_PREFIX = 'pa1_';

export type PaymentResolutionState =
  | 'RESOLVING_CORRELATION'
  | 'POLLING'
  | 'ORDER_CONFIRMED'
  | 'INCIDENCE'
  | 'FAILED'
  | 'APPROVED_PENDING_ORDER'
  | 'PENDING'
  | 'SESSION_EXPIRED'
  | 'UNAUTHORIZED'
  | 'CORRELATION_ERROR'
  | 'UNKNOWN_ERROR';

export interface PaymentReturnViewProps {
  /**
   * The nominal route where the customer landed (/checkout/success, /checkout/pending, /checkout/failure).
   * Note: The UI must NOT trust this route blindly; state is derived exclusively from the backend.
   */
  expectedPath?: 'success' | 'pending' | 'failure';
}

/**
 * Validates the runtime shape of a candidate PendingPaymentCorrelation.
 */
export function isValidCorrelation(obj: unknown): obj is PendingPaymentCorrelation {
  if (!obj || typeof obj !== 'object') return false;
  const c = obj as Record<string, unknown>;
  return (
    typeof c.paymentId === 'string' &&
    UUID_REGEX.test(c.paymentId.trim()) &&
    typeof c.preferenceId === 'string' &&
    typeof c.capacityReservationId === 'string'
  );
}

/**
 * Conditionally clears pending payment correlation only if the stored paymentId matches the confirmed payment.
 * If storage contains a newer or distinct correlation, it is safely preserved.
 */
export function clearMatchingPendingPayment(confirmedPaymentId: string): boolean {
  try {
    const stored = getPendingPayment();
    if (!isValidCorrelation(stored)) {
      return false;
    }
    if (stored.paymentId.trim().toLowerCase() === confirmedPaymentId.trim().toLowerCase()) {
      clearPendingPayment();
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

export interface RefundStatusInfo {
  label: string;
  badgeVariant: 'warning' | 'jade' | 'success' | 'danger' | 'default';
  description: string;
  showAmount: boolean;
}

/**
 * Maps the backend refundStatus strictly without assuming or promising actions not backed by official data.
 */
export function getRefundStatusInfo(refundStatus: string | null | undefined): RefundStatusInfo {
  switch (refundStatus) {
    case 'PENDING':
      return {
        label: 'Devolución Pendiente',
        badgeVariant: 'warning',
        description: 'La devolución se encuentra registrada y pendiente de procesamiento por la pasarela de pagos.',
        showAmount: true,
      };
    case 'PROCESSING':
      return {
        label: 'Devolución en Proceso',
        badgeVariant: 'warning',
        description: 'La devolución está en proceso de gestión con la entidad de pago.',
        showAmount: true,
      };
    case 'COMPLETED':
      return {
        label: 'Devolución Procesada',
        badgeVariant: 'success',
        description: 'La devolución fue procesada en la pasarela. La acreditación efectiva dependerá de los tiempos y políticas de tu entidad bancaria o medio de pago.',
        showAmount: true,
      };
    case 'FAILED':
      return {
        label: 'Devolución No Completada',
        badgeVariant: 'danger',
        description: 'La devolución no pudo completarse de forma automática y requiere revisión manual por nuestro equipo de soporte.',
        showAmount: true,
      };
    case 'NOT_REQUIRED':
      return {
        label: 'No Aplica Devolución',
        badgeVariant: 'default',
        description: 'Para esta incidencia no se requirió la emisión de un reembolso monetario.',
        showAmount: false,
      };
    default:
      return {
        label: 'Estado No Confirmado',
        badgeVariant: 'default',
        description: 'No se dispone de una confirmación oficial del estado de devolución en este momento. Si tienes consultas sobre tu cobro, comunícate con nuestro centro de ayuda.',
        showAmount: false,
      };
  }
}

/**
 * Resolves the paymentId candidate from sessionStorage and searchParams.
 * Returns either:
 * - { status: 'RESOLVED', paymentId: string }
 * - { status: 'ERROR', code: 'DISCREPANCY' | 'MISSING_CORRELATION' | 'ATTEMPT_ID_ONLY' | 'INVALID_UUID' }
 */
export function resolveCorrelationId(
  storedCorrelation: PendingPaymentCorrelation | null,
  searchParams: { get: (key: string) => string | null }
): { status: 'RESOLVED'; paymentId: string } | { status: 'ERROR'; code: string } {
  const storedId = storedCorrelation?.paymentId?.trim();
  const rawExternalRef = searchParams.get('external_reference')?.trim();

  let externalCandidate: string | null = null;
  let isAttemptReference = false;

  if (rawExternalRef) {
    if (rawExternalRef.startsWith(ATTEMPT_PREFIX)) {
      isAttemptReference = true;
    } else if (UUID_REGEX.test(rawExternalRef)) {
      externalCandidate = rawExternalRef;
    }
  }

  // 1. If stored paymentId exists and is valid
  if (storedId && UUID_REGEX.test(storedId)) {
    // If there is also a distinct valid external UUID candidate, stop on discrepancy
    if (externalCandidate && externalCandidate.toLowerCase() !== storedId.toLowerCase()) {
      return { status: 'ERROR', code: 'DISCREPANCY' };
    }
    return { status: 'RESOLVED', paymentId: storedId };
  }

  // 2. No valid stored paymentId, check external candidate
  if (externalCandidate) {
    return { status: 'RESOLVED', paymentId: externalCandidate };
  }

  // 3. If external reference was an attempt ID (pa1_) but no stored correlation exists
  if (isAttemptReference) {
    return { status: 'ERROR', code: 'ATTEMPT_ID_ONLY' };
  }

  if (rawExternalRef && !UUID_REGEX.test(rawExternalRef)) {
    return { status: 'ERROR', code: 'INVALID_UUID' };
  }

  return { status: 'ERROR', code: 'MISSING_CORRELATION' };
}

export const PaymentReturnView: React.FC<PaymentReturnViewProps> = ({ expectedPath = 'success' }) => {
  const searchParams = useSearchParams();

  const [state, setState] = useState<PaymentResolutionState>('RESOLVING_CORRELATION');
  const [resolvedPaymentId, setResolvedPaymentId] = useState<string | null>(null);
  const [statusData, setStatusData] = useState<CustomerPaymentStatusResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isRetrying, setIsRetrying] = useState<boolean>(false);

  const abortControllerRef = useRef<AbortController | null>(null);

  const executeStatusVerification = useCallback(async (paymentId: string) => {
    // Abort any existing in-flight poll
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setState('POLLING');
    setErrorMessage(null);

    try {
      // Poll with 10 attempts, 2.5s interval
      const res = await checkoutService.pollPaymentStatus(paymentId, {
        maxAttempts: 10,
        intervalMs: 2500,
        signal: controller.signal,
      });

      setStatusData(res);

      // Deterministic State Machine evaluation
      // Rule 1: Commercial incidence active
      if (res.hasIncidence) {
        setState('INCIDENCE');
        return;
      }

      // Rule 2: Order confirmed and persisted
      if (res.orderCreated && res.orderNumber) {
        setState('ORDER_CONFIRMED');
        // Safely clean storage ONLY if stored correlation matches this confirmed payment
        clearMatchingPendingPayment(res.paymentId || paymentId);
        return;
      }

      // Rule 3: Payment failed / rejected
      if (res.paymentStatus === 'FAILED') {
        setState('FAILED');
        return;
      }

      // Rule 4: Payment approved without order
      if (res.paymentStatus === 'APPROVED') {
        setState('APPROVED_PENDING_ORDER');
        return;
      }

      // Rule 5: Pending or Processing
      if (res.paymentStatus === 'PENDING' || res.paymentStatus === 'PROCESSING') {
        setState('PENDING');
        return;
      }

      // Default fallback
      setState('UNKNOWN_ERROR');
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        // Polling canceled by user or unmount
        return;
      }

      if (err instanceof PaymentServiceError) {
        if (err.status === 401) {
          setState('SESSION_EXPIRED');
          return;
        }
        if (err.status === 403) {
          setState('UNAUTHORIZED');
          return;
        }
        if (err.status === 404) {
          setState('CORRELATION_ERROR');
          setErrorMessage('No encontramos un registro de pago asociado a esta sesión.');
          return;
        }
      }

      // Transient network or server error - Treat as UNKNOWN_ERROR, NEVER as FAILED
      setState('UNKNOWN_ERROR');
      setErrorMessage(
        err instanceof Error ? err.message : 'No fue posible verificar el estado en este momento.'
      );
    } finally {
      setIsRetrying(false);
    }
  }, []);

  // Initial correlation resolution on mount
  useEffect(() => {
    let rawStored: PendingPaymentCorrelation | null = null;
    try {
      const parsed = getPendingPayment();
      if (isValidCorrelation(parsed)) {
        rawStored = parsed;
      }
    } catch {
      rawStored = null;
    }

    const resolution = resolveCorrelationId(rawStored, searchParams);

    if (resolution.status === 'RESOLVED') {
      setResolvedPaymentId(resolution.paymentId);
      executeStatusVerification(resolution.paymentId);
    } else {
      setState('CORRELATION_ERROR');
      if (resolution.code === 'DISCREPANCY') {
        setErrorMessage(
          'Se detectó un conflicto de seguridad entre la sesión local y los parámetros de retorno. La consulta fue detenida.'
        );
      } else if (resolution.code === 'ATTEMPT_ID_ONLY') {
        setErrorMessage(
          'No fue posible recuperar la sesión de pago debido a que el retorno solo contiene una referencia de intento y no la correlación activa de la compra. Si el pedido no llegó a crearse, no figurará en tu historial.'
        );
      } else {
        setErrorMessage(
          'No se encontró una correlación de pago válida para verificar en esta sesión. Si no se generó un pedido confirmado, este no figurará en tu historial.'
        );
      }
    }

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [searchParams, executeStatusVerification]);

  const handleManualRetry = () => {
    if (!resolvedPaymentId) return;
    setIsRetrying(true);
    executeStatusVerification(resolvedPaymentId);
  };

  return (
    <div className="min-h-[70vh] flex items-center justify-center p-4 sm:p-6 my-8">
      <Card className="w-full max-w-xl p-6 sm:p-8 bg-white border border-[#e7e0d6] shadow-sm text-center">
        {/* State: Polling / Resolving */}
        {(state === 'RESOLVING_CORRELATION' || state === 'POLLING') && (
          <div className="space-y-6" data-testid="state-polling">
            <div className="inline-flex p-4 rounded-full bg-[#f0fdfa] text-[#005141] mb-2 animate-pulse">
              <Clock className="w-10 h-10 animate-spin" style={{ animationDuration: '3s' }} />
            </div>
            <div className="space-y-2">
              <Badge variant="jade" size="lg">
                Verificando pago con el banco
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                Estamos confirmando tu transacción
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                Mercado Pago ha procesado tu solicitud. Estamos sincronizando la confirmación oficial con nuestros servidores.
              </p>
            </div>
            <div className="pt-4 flex justify-center">
              <div className="flex items-center gap-2 text-xs text-[#78716c]">
                <ShieldCheck className="w-4 h-4 text-[#005141]" />
                <span>Verificación segura y cifrada en curso...</span>
              </div>
            </div>
          </div>
        )}

        {/* State: Order Confirmed */}
        {state === 'ORDER_CONFIRMED' && statusData && (
          <div className="space-y-6" data-testid="state-order-confirmed">
            <div className="inline-flex p-4 rounded-full bg-[#f0fdfa] text-[#005141] mb-2">
              <CheckCircle2 className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="success" size="lg">
                ¡Compra Exitosa!
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                ¡Tu pedido está confirmado!
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                Hemos recibido tu pago y tu pedido ya está registrado en el comercio.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#f7f3ec] border border-[#e7e0d6]/80 text-left space-y-2">
              <div className="flex justify-between items-center text-sm">
                <span className="text-[#57534e]">Número de Pedido:</span>
                <span className="font-bold text-[#1c1917] font-mono text-base">
                  #{statusData.orderNumber}
                </span>
              </div>
              <div className="flex justify-between items-center text-sm">
                <span className="text-[#57534e]">Total pagado:</span>
                <span className="font-bold text-[#005141]">
                  {statusData.currency} {Number(statusData.amount).toFixed(2)}
                </span>
              </div>
              {statusData.orderStatus && (
                <div className="flex justify-between items-center text-sm pt-1 border-t border-[#e7e0d6]/50">
                  <span className="text-[#57534e]">Estado del despacho:</span>
                  <Badge variant="jade" size="sm">
                    {statusData.orderStatus}
                  </Badge>
                </div>
              )}
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link href={`/mi-jaldishop/pedidos/${encodeURIComponent(statusData.orderNumber!)}`} className="w-full sm:w-auto">
                <Button variant="primary" size="lg" className="w-full" rightIcon={<ArrowRight className="w-4 h-4" />}>
                  Ver detalle del pedido
                </Button>
              </Link>
              <Link href="/mi-jaldishop/pedidos" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Ir a Mis Pedidos
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Commercial Incidence */}
        {state === 'INCIDENCE' && statusData && (() => {
          const refundInfo = getRefundStatusInfo(statusData.refundStatus);
          return (
            <div className="space-y-6" data-testid="state-incidence">
              <div className="inline-flex p-4 rounded-full bg-amber-50 text-amber-600 mb-2">
                <AlertCircle className="w-12 h-12" />
              </div>
              <div className="space-y-2">
                <Badge variant="warning" size="lg">
                  Revisión Requerida
                </Badge>
                <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                  Incidencia en la confirmación
                </h1>
                <p className="text-sm text-[#57534e] max-w-md mx-auto">
                  {statusData.incidenceMessage ||
                    'Tu pago fue recibido, pero ocurrió una eventualidad al reservar tus productos o cupo de entrega.'}
                </p>
              </div>

              {/* Refund Status Details */}
              <div className="p-4 rounded-2xl bg-amber-50/50 border border-amber-200 text-left space-y-2.5">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-[#57534e] font-medium">Estado de la devolución:</span>
                  <Badge variant={refundInfo.badgeVariant} size="sm">
                    {refundInfo.label}
                  </Badge>
                </div>
                <p className="text-xs text-[#78716c]">
                  {refundInfo.description}
                </p>
                {refundInfo.showAmount && (
                  <div className="flex justify-between items-center text-xs text-[#57534e] pt-1 border-t border-amber-200/60">
                    <span>Monto de la transacción:</span>
                    <span className="font-bold text-[#1c1917]">
                      {statusData.currency} {Number(statusData.amount).toFixed(2)}
                    </span>
                  </div>
                )}
              </div>

              <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
                <Link href="/ayuda" className="w-full sm:w-auto">
                  <Button variant="primary" size="lg" className="w-full" rightIcon={<ExternalLink className="w-3.5 h-3.5" />}>
                    Centro de Ayuda
                  </Button>
                </Link>
                <Link href="/" className="w-full sm:w-auto">
                  <Button variant="outline" size="lg" className="w-full">
                    Volver a la tienda
                  </Button>
                </Link>
              </div>
            </div>
          );
        })()}

        {/* State: Approved Pending Order */}
        {state === 'APPROVED_PENDING_ORDER' && statusData && (
          <div className="space-y-6" data-testid="state-approved-pending-order">
            <div className="inline-flex p-4 rounded-full bg-[#f0fdfa] text-[#005141] mb-2">
              <Clock className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="jade" size="lg">
                Pago Recibido
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                Pago aprobado en proceso de confirmación
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                Tu pago fue aprobado por Mercado Pago. La emisión del pedido está en proceso de sincronización con la tienda.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#f7f3ec] border border-[#e7e0d6]/80 text-left space-y-2">
              <div className="flex justify-between items-center text-sm">
                <span className="text-[#57534e]">Monto acreditado:</span>
                <span className="font-bold text-[#005141]">
                  {statusData.currency} {Number(statusData.amount).toFixed(2)}
                </span>
              </div>
              <p className="text-xs text-[#78716c]">
                El pedido aún no ha sido emitido. No figurará en tu historial de compras hasta que el sistema complete la confirmación oficial. Puedes reintentar la verificación aquí.
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Button
                variant="primary"
                size="lg"
                onClick={handleManualRetry}
                isLoading={isRetrying}
                leftIcon={<RefreshCw className="w-4 h-4" />}
                className="w-full sm:w-auto"
              >
                Actualizar estado
              </Button>
              <Link href="/ayuda" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Centro de Ayuda
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Pending / Processing */}
        {state === 'PENDING' && statusData && (
          <div className="space-y-6" data-testid="state-pending">
            <div className="inline-flex p-4 rounded-full bg-amber-50 text-amber-600 mb-2">
              <Clock className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="amber" size="lg">
                Pago Pendiente de Acreditación
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                Esperando confirmación de la entidad
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                Si pagaste mediante ventanilla, agente bancario o tu billetera digital está procesando la operación, la acreditación puede demorar unos minutos.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#f7f3ec] border border-[#e7e0d6]/80 text-left space-y-2">
              <div className="flex justify-between items-center text-sm">
                <span className="text-[#57534e]">Monto a acreditar:</span>
                <span className="font-bold text-[#1c1917]">
                  {statusData.currency} {Number(statusData.amount).toFixed(2)}
                </span>
              </div>
              <p className="text-xs text-[#78716c]">
                Una vez que Mercado Pago notifique la acreditación efectiva, tu compra se confirmará.
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Button
                variant="primary"
                size="lg"
                onClick={handleManualRetry}
                isLoading={isRetrying}
                leftIcon={<RefreshCw className="w-4 h-4" />}
                className="w-full sm:w-auto"
              >
                Reconsultar estado
              </Button>
              <Link href="/" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Volver al inicio
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Failed / Rejected */}
        {state === 'FAILED' && (
          <div className="space-y-6" data-testid="state-failed">
            <div className="inline-flex p-4 rounded-full bg-rose-50 text-rose-600 mb-2">
              <XCircle className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="danger" size="lg">
                Pago No Aprobado
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                No pudimos procesar tu pago
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                La entidad bancaria o Mercado Pago no autorizó la operación. No se ha realizado ningún cobro para este pedido.
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link href="/" className="w-full sm:w-auto">
                <Button variant="primary" size="lg" className="w-full" leftIcon={<ShoppingBag className="w-4 h-4" />}>
                  Volver al comercio
                </Button>
              </Link>
              <Link href="/ayuda" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Consultar ayuda
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Session Expired */}
        {state === 'SESSION_EXPIRED' && (
          <div className="space-y-6" data-testid="state-session-expired">
            <div className="inline-flex p-4 rounded-full bg-amber-50 text-amber-600 mb-2">
              <AlertCircle className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="warning" size="lg">
                Sesión Requerida
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                Tu sesión ha expirado
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                Por motivos de seguridad, inicia sesión nuevamente para consultar el estado oficial de tu pago. Conservamos los datos de tu compra en este dispositivo para reanudar la verificación al volver.
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link href={`/login?redirect=${encodeURIComponent(expectedPath ? `/checkout/${expectedPath}` : '/checkout/success')}`} className="w-full sm:w-auto">
                <Button variant="primary" size="lg" className="w-full">
                  Iniciar sesión
                </Button>
              </Link>
              <Link href="/" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Ir al catálogo
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Unauthorized (403) */}
        {state === 'UNAUTHORIZED' && (
          <div className="space-y-6" data-testid="state-unauthorized">
            <div className="inline-flex p-4 rounded-full bg-rose-50 text-rose-600 mb-2">
              <XCircle className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="danger" size="lg">
                Acceso no autorizado
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                No tienes permisos para ver este pago
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                La transacción solicitada no pertenece a la cuenta de usuario actualmente autenticada.
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link href="/mi-jaldishop/pedidos" className="w-full sm:w-auto">
                <Button variant="primary" size="lg" className="w-full">
                  Ver mis pedidos
                </Button>
              </Link>
              <Link href="/" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Ir al inicio
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Correlation Error */}
        {state === 'CORRELATION_ERROR' && (
          <div className="space-y-6" data-testid="state-correlation-error">
            <div className="inline-flex p-4 rounded-full bg-stone-100 text-stone-600 mb-2">
              <HelpCircle className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="default" size="lg">
                Transacción no identificada
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                No encontramos los datos del pago
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                {errorMessage ||
                  'No fue posible vincular tu regreso con un identificador de compra válido.'}
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link href="/ayuda" className="w-full sm:w-auto">
                <Button variant="primary" size="lg" className="w-full">
                  Centro de Ayuda
                </Button>
              </Link>
              <Link href="/" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Volver al comercio
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* State: Unknown Error / Network Timeout */}
        {state === 'UNKNOWN_ERROR' && (
          <div className="space-y-6" data-testid="state-unknown-error">
            <div className="inline-flex p-4 rounded-full bg-amber-50 text-amber-600 mb-2">
              <RotateCcw className="w-12 h-12" />
            </div>
            <div className="space-y-2">
              <Badge variant="amber" size="lg">
                Verificación Pendiente
              </Badge>
              <h1 className="text-2xl font-bold text-[#1c1917] tracking-tight">
                No pudimos comprobar el estado de inmediato
              </h1>
              <p className="text-sm text-[#57534e] max-w-md mx-auto">
                {errorMessage ||
                  'La conexión con el servicio de pagos tardó más de lo habitual. Tu pago no ha sido rechazado, pero requiere una reconsulta.'}
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              {resolvedPaymentId && (
                <Button
                  variant="primary"
                  size="lg"
                  onClick={handleManualRetry}
                  isLoading={isRetrying}
                  leftIcon={<RefreshCw className="w-4 h-4" />}
                  className="w-full sm:w-auto"
                >
                  Reintentar verificación
                </Button>
              )}
              <Link href="/mi-jaldishop/pedidos" className="w-full sm:w-auto">
                <Button variant="outline" size="lg" className="w-full">
                  Consultar mis pedidos
                </Button>
              </Link>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};
