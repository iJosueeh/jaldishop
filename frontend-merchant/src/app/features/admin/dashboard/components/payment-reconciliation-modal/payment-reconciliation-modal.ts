import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../../core/services/admin.service';
import {
  OrderInvestigationResult,
  OrderReconciliationResult,
} from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matBoltOutline,
  matCheckCircleOutline,
  matWarningOutline,
  matSearchOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matBoltOutline,
      matCheckCircleOutline,
      matWarningOutline,
      matSearchOutline,
      matRefreshOutline,
    }),
  ],
  selector: 'app-payment-reconciliation-modal',
  styleUrl: './payment-reconciliation-modal.css',
  templateUrl: './payment-reconciliation-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentReconciliationModal {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isOpen = input<boolean>(false);
  readonly closeModal = output<void>();

  readonly isRendered = signal<boolean>(false);
  readonly isClosing = signal<boolean>(false);
  private closeTimeoutId?: ReturnType<typeof setTimeout>;

  private readonly syncOpenEffect = effect(() => {
    const open = this.isOpen();
    if (open) {
      if (this.closeTimeoutId) {
        clearTimeout(this.closeTimeoutId);
        this.closeTimeoutId = undefined;
      }
      this.isRendered.set(true);
      this.isClosing.set(false);
    } else if (this.isRendered() && !this.isClosing()) {
      this.triggerExit();
    }
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.closeTimeoutId) {
        clearTimeout(this.closeTimeoutId);
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isRendered() && !this.isClosing()) {
      this.dismiss();
    }
  }

  readonly attemptId = signal<string>('');
  readonly isInvestigating = signal<boolean>(false);
  readonly isReconciling = signal<boolean>(false);
  readonly investigationResult = signal<OrderInvestigationResult | null>(null);
  readonly reconciliationResult = signal<OrderReconciliationResult | null>(null);
  readonly errorMessage = signal<string | null>(null);

  isValidUuid(): boolean {
    const trimmed = this.attemptId().trim();
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(trimmed);
  }

  onInvestigate(): void {
    if (!this.isValidUuid()) {
      this.errorMessage.set('Debe ingresar un UUID válido de intento de pago (Payment Attempt ID).');
      return;
    }

    this.errorMessage.set(null);
    this.reconciliationResult.set(null);
    this.isInvestigating.set(true);

    this.adminService.investigatePaymentAttempt(this.attemptId().trim()).subscribe({
      next: (res) => {
        this.investigationResult.set(res);
        this.isInvestigating.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'No se pudo obtener información del intento de pago.');
        this.isInvestigating.set(false);
      },
    });
  }

  onReconcile(): void {
    if (!this.isValidUuid()) return;

    this.errorMessage.set(null);
    this.isReconciling.set(true);

    this.adminService.reconcilePaymentAttempt(this.attemptId().trim()).subscribe({
      next: (res) => {
        this.reconciliationResult.set(res);
        this.isReconciling.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Error al ejecutar la conciliación.');
        this.isReconciling.set(false);
      },
    });
  }

  dismiss(): void {
    if (this.isClosing()) return;
    this.triggerExit(() => {
      this.attemptId.set('');
      this.investigationResult.set(null);
      this.reconciliationResult.set(null);
      this.errorMessage.set(null);
      this.closeModal.emit();
    });
  }

  private triggerExit(callback?: () => void): void {
    if (this.closeTimeoutId) {
      clearTimeout(this.closeTimeoutId);
    }
    this.isClosing.set(true);
    this.closeTimeoutId = setTimeout(() => {
      this.isRendered.set(false);
      this.isClosing.set(false);
      this.closeTimeoutId = undefined;
      callback?.();
    }, 180);
  }
}
