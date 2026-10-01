import { Component, HostListener, effect, input, output, signal } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matDeleteOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matDeleteOutline,
      matWarningOutline,
    }),
  ],
  templateUrl: './confirm-modal.html',
  styleUrl: './confirm-modal.css',
})
export class ConfirmModal {
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('¿Eliminar elemento?');
  readonly message = input<string>('Esta acción no se puede deshacer. ¿Deseas continuar?');
  readonly confirmText = input<string>('Eliminar');
  readonly cancelText = input<string>('Cancelar');
  readonly isDanger = input<boolean>(true);

  readonly confirm = output<void>();
  readonly cancel = output<void>();
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
  readonly isOpenChange = output<boolean>();

  readonly isRendered = signal<boolean>(false);
  readonly isClosing = signal<boolean>(false);

  private closeTimeoutId?: any;

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
      this.triggerExitAnimation();
    }
  });

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isRendered() && !this.isClosing()) {
      this.onCancel();
    }
  }

  onConfirm(): void {
    if (this.isClosing()) return;
    this.triggerExitAnimation(() => {
      this.confirm.emit();
      this.confirmed.emit();
      this.isOpenChange.emit(false);
    });
  }

  onCancel(): void {
    if (this.isClosing()) return;
    this.triggerExitAnimation(() => {
      this.cancel.emit();
      this.cancelled.emit();
      this.isOpenChange.emit(false);
    });
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.onCancel();
    }
  }

  private triggerExitAnimation(callback?: () => void): void {
    if (this.closeTimeoutId) {
      clearTimeout(this.closeTimeoutId);
    }
    this.isClosing.set(true);
    this.closeTimeoutId = setTimeout(() => {
      this.isRendered.set(false);
      this.isClosing.set(false);
      this.closeTimeoutId = undefined;
      callback?.();
    }, 220);
  }
}
