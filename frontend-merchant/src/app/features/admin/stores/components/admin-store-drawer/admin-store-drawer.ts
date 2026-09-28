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

  readonly isClosing = signal<boolean>(false);
  readonly isCopied = signal<boolean>(false);

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

  onToggleStatus(): void {
    const currentStore = this.store();
    if (currentStore) {
      this.statusChange.emit(currentStore);
    }
  }
}
