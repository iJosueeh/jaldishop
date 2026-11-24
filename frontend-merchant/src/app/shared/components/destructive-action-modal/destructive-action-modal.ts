import { ChangeDetectionStrategy, Component, computed, HostListener, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matDeleteOutline,
  matPauseCircleOutline,
  matWarningOutline,
  matBlockOutline,
} from '@ng-icons/material-symbols/outline';
import { ApiErrorMetadata } from '../../../core/models/api-error.models';

@Component({
  selector: 'app-destructive-action-modal',
  standalone: true,
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matDeleteOutline,
      matPauseCircleOutline,
      matWarningOutline,
      matBlockOutline,
    }),
  ],
  templateUrl: './destructive-action-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DestructiveActionModal {
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('Confirmar eliminación');
  readonly message = input<string>('Esta acción eliminará el registro de forma permanente si no tiene historial operativo.');
  readonly confirmText = input<string>('Eliminar permanentemente');
  readonly cancelText = input<string>('Cancelar');
  readonly isLoading = input<boolean>(false);

  /** Estado de conflicto 409 / 403 devuelto por el backend */
  readonly errorStatus = input<number | null>(null);
  readonly errorMessage = input<string | null>(null);
  readonly conflictMetadata = input<ApiErrorMetadata | null>(null);
  readonly fallbackLabel = input<string>('Desactivar en su lugar');

  readonly confirm = output<void>();
  readonly fallback = output<void>();
  readonly cancel = output<void>();

  readonly isConflict = computed(() => this.errorStatus() === 409 && !!this.errorMessage());
  readonly isForbidden = computed(() => this.errorStatus() === 403 && !!this.errorMessage());

  readonly canOfferFallback = computed(() => {
    if (!this.isConflict()) return false;
    const meta = this.conflictMetadata();
    if (!meta?.suggestedAction) return false;
    const status = (meta.currentStatus ?? '').toUpperCase();
    return status === '' || status === 'ACTIVE';
  });

  readonly alreadyInactiveNotice = computed(() => {
    if (!this.isConflict()) return false;
    const status = (this.conflictMetadata()?.currentStatus ?? '').toUpperCase();
    return status === 'INACTIVE' || status === 'SUSPENDED' || status === 'CLOSED';
  });

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen() && !this.isLoading()) {
      this.cancel.emit();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && !this.isLoading()) {
      this.cancel.emit();
    }
  }
}
