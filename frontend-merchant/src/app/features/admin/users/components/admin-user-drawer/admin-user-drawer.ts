import {
  Component,
  HostListener,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminUserSummary } from '../../../../../core/models/admin.models';
import { ToastService } from '../../../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matContentCopyOutline,
  matCallOutline,
  matMailOutline,
  matShieldOutline,
  matStorefrontOutline,
  matCalendarMonthOutline,
  matKeyOutline,
  matCheckCircleOutline,
  matCancelOutline,
  matChatOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matContentCopyOutline,
      matCallOutline,
      matMailOutline,
      matShieldOutline,
      matStorefrontOutline,
      matCalendarMonthOutline,
      matKeyOutline,
      matCheckCircleOutline,
      matCancelOutline,
      matChatOutline,
    }),
  ],
  selector: 'app-admin-user-drawer',
  styleUrl: './admin-user-drawer.css',
  templateUrl: './admin-user-drawer.html',
})
export class AdminUserDrawer {
  private readonly toastService = inject(ToastService);

  readonly user = input<AdminUserSummary | null>(null);
  readonly isOpen = input<boolean>(false);
  readonly closeDrawer = output<void>();
  readonly statusChange = output<AdminUserSummary>();

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
      this.toastService.info('ID de usuario copiado al portapapeles.', 'Copiado');
      setTimeout(() => this.isCopied.set(false), 2000);
    });
  }

  onToggleStatus(): void {
    const currentUser = this.user();
    if (currentUser) {
      this.statusChange.emit(currentUser);
    }
  }
}
