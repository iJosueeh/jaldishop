import {
  Component,
  HostListener,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { ToastService } from '../../../../../core/services/toast.service';
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
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
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
    }),
  ],
  selector: 'app-admin-store-drawer',
  styleUrl: './admin-store-drawer.css',
  templateUrl: './admin-store-drawer.html',
})
export class AdminStoreDrawer {
  private readonly toastService = inject(ToastService);

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
    const url = `${window.location.origin}/store/${slug}`;
    navigator.clipboard.writeText(url).then(() => {
      this.isLinkCopied.set(true);
      this.toastService.info('Enlace público de la tienda copiado.', 'Enlace Copiado');
      setTimeout(() => this.isLinkCopied.set(false), 2000);
    });
  }

  openPublicStore(slug: string): void {
    const url = `/store/${slug}`;
    window.open(url, '_blank');
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
      const label = type === 'pickup' ? 'Recogida (Pickup)' : 'Entrega (Delivery)';
      this.toastService.info(
        `Modalidad ${label} actualizada para ${s.name}.`,
        'Configuración de Tienda'
      );
    }
  }

  onNoteInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.adminNote.set(value);
  }

  onSaveNote(): void {
    this.isNoteSaved.set(true);
    this.toastService.success('Nota de auditoría de tienda guardada.', 'Nota Admin');
    setTimeout(() => this.isNoteSaved.set(false), 2500);
  }
}

