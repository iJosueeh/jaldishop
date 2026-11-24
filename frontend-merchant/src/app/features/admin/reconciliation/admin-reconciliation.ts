import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth-service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmModal } from '../../../shared/components/confirm-modal/confirm-modal';
import {
  OrderInvestigationResult,
  OrderReconciliationResult,
  ReconciliationCandidateItem,
} from '../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matBoltOutline,
  matSearchOutline,
  matCheckCircleOutline,
  matWarningOutline,
  matErrorOutline,
  matHelpOutline,
  matRefreshOutline,
  matReceiptLongOutline,
  matContentCopyOutline,
  matArrowDropDownOutline,
  matFilterListOutline,
  matStoreOutline,
  matPersonOutline,
  matPaymentsOutline,
  matCloseOutline,
  matInfoOutline,
  matOpenInNewOutline,
} from '@ng-icons/material-symbols/outline';

export interface AuditSessionInvestigation {
  attemptId: string;
  timestamp: Date;
  outcome: string;
  reason: string;
  reconciled: boolean;
}

@Component({
  imports: [CommonModule, FormsModule, RouterLink, NgIcon, ConfirmModal],
  providers: [
    provideIcons({
      matBoltOutline,
      matSearchOutline,
      matCheckCircleOutline,
      matWarningOutline,
      matErrorOutline,
      matHelpOutline,
      matRefreshOutline,
      matReceiptLongOutline,
      matContentCopyOutline,
      matArrowDropDownOutline,
      matFilterListOutline,
      matStoreOutline,
      matPersonOutline,
      matPaymentsOutline,
      matCloseOutline,
      matInfoOutline,
      matOpenInNewOutline,
    }),
  ],
  selector: 'app-admin-reconciliation',
  styleUrl: './admin-reconciliation.css',
  templateUrl: './admin-reconciliation.html',
})
export class AdminReconciliation implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toast = inject(ToastService);

  readonly analyticsSummary = this.adminService.analyticsSummary;
  readonly isLoadingAnalytics = this.adminService.isLoadingAnalytics;

  // Candidatos de contingencia
  readonly candidates = signal<ReconciliationCandidateItem[]>([]);
  readonly isLoadingCandidates = signal<boolean>(false);
  readonly isDropdownOpen = signal<boolean>(false);
  readonly dropdownFilter = signal<string>('');
  readonly selectedCandidate = signal<ReconciliationCandidateItem | null>(null);

  // Formulario de Investigación
  attemptIdInput = signal<string>('');
  readonly isInvestigating = signal<boolean>(false);
  readonly isReconciling = signal<boolean>(false);

  // Resultado actual de investigación
  readonly investigationResult = signal<OrderInvestigationResult | null>(null);
  readonly reconciliationResult = signal<OrderReconciliationResult | null>(null);
  readonly inputError = signal<string | null>(null);

  // Historial de la sesión actual
  readonly sessionHistory = signal<AuditSessionInvestigation[]>([]);
  readonly isHistoryModalOpen = signal<boolean>(false);

  // Candidatos filtrados en el dropdown (Top 5 visible para rendimiento)
  readonly filteredCandidates = computed(() => {
    const list = this.candidates();
    const query = this.dropdownFilter().trim().toLowerCase();
    if (!query) {
      return list;
    }
    return list.filter((c) =>
      c.storeName?.toLowerCase().includes(query) ||
      c.customerEmail?.toLowerCase().includes(query) ||
      c.customerName?.toLowerCase().includes(query) ||
      c.providerPaymentId?.toLowerCase().includes(query) ||
      c.attemptId.toLowerCase().includes(query) ||
      c.amount.toString().includes(query)
    );
  });

  private readonly uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  private readonly authService = inject(AuthService);
  readonly isPrimaryAdmin = this.authService.isAdmin;

  ngOnInit(): void {
    this.adminService.loadAnalyticsSummary().subscribe();
    this.loadCandidates();
    this.loadHistory();
  }

  loadHistory(): void {
    this.adminService.getReconciliationHistory(10).subscribe({
      next: (items) => {
        this.sessionHistory.set(
          items.map((item) => ({
            attemptId: item.attemptId,
            timestamp: new Date(item.timestamp),
            outcome: item.outcome,
            reason: item.reason,
            reconciled: item.reconciled,
          })),
        );
      },
      error: () => {
        // Silencioso si no hay historial previo
      },
    });
  }

  loadCandidates(): void {
    this.isLoadingCandidates.set(true);
    this.adminService.getReconciliationCandidates(15).subscribe({
      next: (data) => {
        this.candidates.set(data || []);
        this.isLoadingCandidates.set(false);
      },
      error: () => {
        this.isLoadingCandidates.set(false);
      },
    });
  }

  toggleDropdown(): void {
    this.isDropdownOpen.update((open) => !open);
    if (!this.isDropdownOpen()) {
      this.dropdownFilter.set('');
    }
  }

  selectCandidate(candidate: ReconciliationCandidateItem): void {
    this.selectedCandidate.set(candidate);
    this.attemptIdInput.set(candidate.attemptId);
    this.isDropdownOpen.set(false);
    this.dropdownFilter.set('');
    this.inputError.set(null);
    this.investigate();
  }

  clearSelectedCandidate(): void {
    this.selectedCandidate.set(null);
    this.attemptIdInput.set('');
    this.investigationResult.set(null);
    this.reconciliationResult.set(null);
    this.inputError.set(null);
  }

  getDiagnosticActionRecommendation(
    outcome: string | undefined,
    reason?: string
  ): { title: string; advice: string; canReconcile: boolean; isActionable: boolean } {
    if (outcome === 'INCONCLUSIVE' && reason === 'NOT_UNKNOWN') {
      return {
        title: 'Intento Procesado Previamente',
        advice: 'El intento ya superó la fase de creación remota inicial (no está en limbo UNKNOWN). Si el cobro fue aprobado en pasarela, puedes forzar conciliación directamente para verificar o crear el pedido.',
        canReconcile: true,
        isActionable: true,
      };
    }

    switch (outcome) {
      case 'VERIFIED':
        return {
          title: 'Cobro Legítimo Confirmado',
          advice: 'La pasarela confirmó el débito al cliente. Puedes ejecutar "Forzar Conciliación" para generar la orden en estado CONFIRMED.',
          canReconcile: true,
          isActionable: true,
        };
      case 'INCONCLUSIVE':
        return {
          title: 'Diagnóstico No Concluyente',
          advice: 'La orden no existe aún en la pasarela o no ha completado el ciclo de pago. Sugerencia: espera 5 minutos o solicita al cliente el comprobante de pago.',
          canReconcile: false,
          isActionable: false,
        };
      case 'CONTRADICTION':
        return {
          title: 'Conflicto o Contradicción de Datos',
          advice: 'La identidad del intento cambió o el modo de checkout difiere. No se recomienda conciliar sin revisión directa de la base de datos.',
          canReconcile: false,
          isActionable: false,
        };
      case 'TECHNICAL_ERROR':
        return {
          title: 'Error de Red / Conectividad con Pasarela',
          advice: 'Ocurrió un error al contactar las APIs de Mercado Pago. Verifica tus credenciales o reintenta el diagnóstico en unos momentos.',
          canReconcile: false,
          isActionable: true,
        };
      default:
        return {
          title: 'Estado Transaccional Indeterminado',
          advice: 'Revisa el ID de transacción y consulta el estado en la consola de Mercado Pago.',
          canReconcile: false,
          isActionable: false,
        };
    }
  }

  onInputId(val: string): void {
    const trimmed = val.trim();
    this.attemptIdInput.set(trimmed);
    if (this.inputError()) {
      this.inputError.set(null);
    }

    // Si el usuario pega un ID de Mercado Pago, buscar coincidencia entre los candidatos
    const matched = this.candidates().find(
      (c) => c.providerPaymentId === trimmed || c.attemptId === trimmed
    );
    if (matched) {
      this.selectedCandidate.set(matched);
      this.attemptIdInput.set(matched.attemptId);
    } else if (this.selectedCandidate() && this.selectedCandidate()?.attemptId !== trimmed) {
      this.selectedCandidate.set(null);
    }
  }

  private readonly investigationCache = new Map<
    string,
    { result: OrderInvestigationResult; cachedAt: number }
  >();
  private static readonly CACHE_TTL_MS = 60_000;
  readonly selectedHistoryItemSummary = signal<AuditSessionInvestigation | null>(null);
  readonly isCachedDiagnostic = signal<boolean>(false);

  investigate(forceRefresh = false): void {
    const id = this.attemptIdInput().trim();
    if (!this.validateUuid(id)) return;

    this.selectedHistoryItemSummary.set(null);

    if (!forceRefresh) {
      const cached = this.investigationCache.get(id);
      if (cached && Date.now() - cached.cachedAt < AdminReconciliation.CACHE_TTL_MS) {
        this.investigationResult.set(cached.result);
        this.reconciliationResult.set(null);
        this.isCachedDiagnostic.set(true);
        this.toast.info(
          'Mostrando diagnóstico reciente en caché. Usa "Forzar consulta en vivo" para consultar nuevamente a Mercado Pago.',
          'Diagnóstico en Caché'
        );
        return;
      }
    }

    this.isCachedDiagnostic.set(false);
    this.isInvestigating.set(true);
    this.investigationResult.set(null);
    this.reconciliationResult.set(null);

    this.adminService.investigatePaymentAttempt(id).subscribe({
      next: (res) => {
        this.isInvestigating.set(false);
        this.investigationResult.set(res);
        this.investigationCache.set(id, { result: res, cachedAt: Date.now() });
        const outcome = res.outcome || res.status || 'UNKNOWN';
        this.toast.success(
          `Diagnóstico completado: ${outcome}`,
          'Investigación Transaccional'
        );
        this.loadHistory();
      },
      error: (err) => {
        this.isInvestigating.set(false);
        const msg = err.error?.message || 'Error al comunicarse con la pasarela.';
        this.toast.error(msg, 'Fallo de Investigación');
      },
    });
  }

  readonly isReconcileConfirmOpen = signal<boolean>(false);

  requestReconcile(): void {
    const id = this.attemptIdInput().trim();
    if (!this.validateUuid(id)) return;
    this.isReconcileConfirmOpen.set(true);
  }

  reconcile(): void {
    this.isReconcileConfirmOpen.set(false);
    const id = this.attemptIdInput().trim();
    if (!this.validateUuid(id)) return;

    this.isReconciling.set(true);

    this.adminService.reconcilePaymentAttempt(id).subscribe({
      next: (res) => {
        this.isReconciling.set(false);
        this.investigationCache.delete(id);
        this.isCachedDiagnostic.set(false);
        this.selectedHistoryItemSummary.set(null);
        this.reconciliationResult.set(res);
        this.candidates.update((list) =>
          list.filter((c) => c.attemptId !== id && c.paymentId !== id)
        );
        if (
          this.selectedCandidate()?.attemptId === id ||
          this.selectedCandidate()?.paymentId === id
        ) {
          this.selectedCandidate.set(null);
        }
        this.toast.success(
          `Transacción conciliada exitosamente. Orden ${res.orderNumber || res.orderId}.`,
          'Conciliación Exitosa'
        );
        this.loadHistory();
        this.loadCandidates();
        this.adminService.loadAnalyticsSummary().subscribe();
      },
      error: (err) => {
        this.isReconciling.set(false);
        const msg = err.error?.message || 'No fue posible conciliar la orden.';
        this.toast.error(msg, 'Error de Conciliación');
      },
    });
  }

  private validateUuid(val: string): boolean {
    if (!val) {
      this.inputError.set('Ingrese un UUID de intento de pago válido.');
      return false;
    }
    if (!this.uuidRegex.test(val)) {
      this.inputError.set('Formato inválido. Debe ser un UUID (ej: 123e4567-e89b-12d3-a456-426614174000).');
      return false;
    }
    this.inputError.set(null);
    return true;
  }

  private recordSessionHistory(
    attemptId: string,
    outcome: string,
    reason: string,
    reconciled: boolean
  ): void {
    const entry: AuditSessionInvestigation = {
      attemptId,
      timestamp: new Date(),
      outcome,
      reason,
      reconciled,
    };
    this.sessionHistory.update((prev) => [entry, ...prev.slice(0, 9)]);
  }

  copyToClipboard(val: string): void {
    navigator.clipboard?.writeText(val);
    this.toast.success('UUID copiado al portapapeles', 'Copiado');
  }

  openHistoryModal(): void {
    this.isHistoryModalOpen.set(true);
  }

  closeHistoryModal(): void {
    this.isHistoryModalOpen.set(false);
  }

  selectHistoryItem(item: AuditSessionInvestigation): void {
    this.isHistoryModalOpen.set(false);
    this.attemptIdInput.set(item.attemptId);
    this.inputError.set(null);
    this.reconciliationResult.set(null);

    const cached = this.investigationCache.get(item.attemptId);
    if (cached && Date.now() - cached.cachedAt < AdminReconciliation.CACHE_TTL_MS) {
      this.investigationResult.set(cached.result);
      this.isCachedDiagnostic.set(true);
      this.selectedHistoryItemSummary.set(null);
      return;
    }

    this.investigationResult.set(null);
    this.isCachedDiagnostic.set(false);
    this.selectedHistoryItemSummary.set(item);
  }
}
