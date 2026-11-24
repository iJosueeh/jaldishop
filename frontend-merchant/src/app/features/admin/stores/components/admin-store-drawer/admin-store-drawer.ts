import {
  Component,
  HostListener,
  inject,
  input,
  OnChanges,
  output,
  signal,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../../core/services/admin.service';
import {
  AdminAnalyticsSummary,
  AdminStoreDetail,
  AdminStoreSummary,
} from '../../../../../core/models/admin.models';
import { ToastService } from '../../../../../core/services/toast.service';
import { ErrorHandlerService } from '../../../../../core/services/error-handler.service';
import { ApiErrorMetadata } from '../../../../../core/models/api-error.models';
import { DestructiveActionModal } from '../../../../../shared/components/destructive-action-modal/destructive-action-modal';
import { environment } from '../../../../../../environments/environment';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matContentCopyOutline,
  matStorefrontOutline,
  matPersonOutline,
  matCallOutline,
  matMailOutline,
  matLocationOnOutline,
  matLocalShippingOutline,
  matShoppingBagOutline,
  matCalendarMonthOutline,
  matCheckCircleOutline,
  matCancelOutline,
  matKeyOutline,
  matOpenInNewOutline,
  matChatOutline,
  matNotesOutline,
  matCheckOutline,
  matBoltOutline,
  matTuneOutline,
  matReceiptLongOutline,
  matAccountBalanceOutline,
  matPaymentsOutline,
  matDeleteOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon, DestructiveActionModal],
  providers: [
    provideIcons({
      matCloseOutline,
      matContentCopyOutline,
      matStorefrontOutline,
      matPersonOutline,
      matCallOutline,
      matMailOutline,
      matLocationOnOutline,
      matLocalShippingOutline,
      matShoppingBagOutline,
      matCalendarMonthOutline,
      matCheckCircleOutline,
      matCancelOutline,
      matKeyOutline,
      matOpenInNewOutline,
      matChatOutline,
      matNotesOutline,
      matCheckOutline,
      matBoltOutline,
      matTuneOutline,
      matReceiptLongOutline,
      matAccountBalanceOutline,
      matPaymentsOutline,
      matDeleteOutline,
    }),
  ],
  selector: 'app-admin-store-drawer',
  styleUrl: './admin-store-drawer.css',
  templateUrl: './admin-store-drawer.html',
})
export class AdminStoreDrawer implements OnChanges {
  private readonly toastService = inject(ToastService);
  private readonly adminService = inject(AdminService);

  readonly store = input<AdminStoreSummary | null>(null);
  readonly isOpen = input<boolean>(false);
  readonly closeDrawer = output<void>();
  readonly statusChange = output<AdminStoreSummary>();
  readonly modalityChange = output<{ store: AdminStoreSummary; type: 'pickup' | 'delivery' }>();

  readonly isClosing = signal<boolean>(false);
  readonly isCopied = signal<boolean>(false);
  readonly isLinkCopied = signal<boolean>(false);
  readonly adminNote = signal<string>('');
  readonly isNoteSaved = signal<boolean>(false);

  // Datos Enriquecidos (Fiscal, Liquidación y Métricas de Tienda)
  readonly storeDetail = signal<AdminStoreDetail | null>(null);
  readonly isLoadingDetail = signal<boolean>(false);
  readonly storeAnalytics = signal<AdminAnalyticsSummary | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if (this.isOpen() && this.store()) {
      this.loadStoreFullData(this.store()!.id);
    }
  }

  loadStoreFullData(storeId: string): void {
    this.isLoadingDetail.set(true);
    this.adminService.getStoreDetail(storeId, true).subscribe({
      next: (detail) => {
        this.storeDetail.set(detail);
        this.isLoadingDetail.set(false);
      },
      error: () => this.isLoadingDetail.set(false),
    });

    this.adminService.loadAnalyticsSummary(undefined, undefined, storeId).subscribe({
      next: (analytics) => this.storeAnalytics.set(analytics),
      error: () => {},
    });
  }

  @HostListener('window:keydown.escape')
  handleEscape(): void {
    if (this.isOpen() && !this.isClosing()) {
      this.dismiss();
    }
  }

  dismiss(): void {
    if (this.isClosing()) return;
    this.isClosing.set(true);
    setTimeout(() => {
      this.isClosing.set(false);
      this.closeDrawer.emit();
    }, 180);
  }

  copyId(id: string): void {
    navigator.clipboard.writeText(id).then(() => {
      this.isCopied.set(true);
      this.toastService.info('ID de tienda copiado al portapapeles.', 'Copiado');
      setTimeout(() => this.isCopied.set(false), 2000);
    });
  }

  copyStoreLink(slug: string): void {
    const url = `${environment.marketplaceUrl}/store/${slug}`;
    navigator.clipboard.writeText(url).then(() => {
      this.isLinkCopied.set(true);
      this.toastService.info('Enlace público de la tienda copiado.', 'Enlace Copiado');
      setTimeout(() => this.isLinkCopied.set(false), 2000);
    });
  }

  openPublicStore(slug: string): void {
    const url = `${environment.marketplaceUrl}/store/${slug}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  onToggleStatus(): void {
    const currentStore = this.store();
    if (currentStore) {
      this.statusChange.emit(currentStore);
    }
  }

  onToggleModality(type: 'pickup' | 'delivery'): void {
    const s = this.store();
    if (s) {
      this.modalityChange.emit({ store: s, type });
      this.toastService.warning(
        'La modificación directa de modalidades desde Admin requiere autorización del comercio.',
        'Acción No Disponible'
      );
    }
  }

  onNoteInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.adminNote.set(value);
  }

  onSaveNote(): void {
    this.toastService.info('La persistencia de notas internas estará disponible próximamente.', 'Notas Admin');
  }

  private readonly errorHandler = inject(ErrorHandlerService);
  readonly isDeleteModalOpen = signal<boolean>(false);
  readonly isDeletingStore = signal<boolean>(false);
  readonly deleteErrorStatus = signal<number | null>(null);
  readonly deleteErrorMessage = signal<string | null>(null);
  readonly deleteConflictMetadata = signal<ApiErrorMetadata | null>(null);

  requestDeleteStore(): void {
    this.deleteErrorStatus.set(null);
    this.deleteErrorMessage.set(null);
    this.deleteConflictMetadata.set(null);
    this.isDeleteModalOpen.set(true);
  }

  cancelDeleteStore(): void {
    if (!this.isDeletingStore()) {
      this.isDeleteModalOpen.set(false);
    }
  }

  confirmDeleteStore(): void {
    const s = this.store();
    if (!s) return;
    this.isDeletingStore.set(true);
    this.adminService.deleteStore(s.id).subscribe({
      next: (res) => {
        this.isDeletingStore.set(false);
        this.isDeleteModalOpen.set(false);
        this.toastService.success(res.message, 'Tienda Eliminada');
        this.dismiss();
      },
      error: (err) => {
        this.isDeletingStore.set(false);
        const normalized = this.errorHandler.normalize(err);
        if (normalized.status === 409 || normalized.status === 403) {
          this.deleteErrorStatus.set(normalized.status);
          this.deleteErrorMessage.set(normalized.message);
          this.deleteConflictMetadata.set(normalized.metadata ?? null);
        } else {
          this.isDeleteModalOpen.set(false);
          this.toastService.error(normalized.message);
        }
      },
    });
  }

  fallbackSuspendStore(): void {
    this.isDeleteModalOpen.set(false);
    this.onToggleStatus();
  }
}
