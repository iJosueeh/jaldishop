import {
  Component,
  HostListener,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminUserSummary, AdminUserRole } from '../../../../../core/models/admin.models';
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
  matLockOutline,
  matNotesOutline,
  matCheckOutline,
  matBoltOutline,
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
      matLockOutline,
      matNotesOutline,
      matCheckOutline,
      matBoltOutline,
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
  readonly roleToggle = output<{ user: AdminUserSummary; role: AdminUserRole }>();

  readonly isClosing = signal<boolean>(false);
  readonly isCopied = signal<boolean>(false);
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

  onSendPasswordReset(): void {
    const u = this.user();
    if (u) {
      this.toastService.success(
        `Enlace de restablecimiento de contraseña enviado a ${u.email}`,
        'Acción de Soporte'
      );
    }
  }

  onToggleRole(role: AdminUserRole): void {
    const u = this.user();
    if (u) {
      this.roleToggle.emit({ user: u, role });
      this.toastService.info(
        `Solicitud de cambio de rol (${role}) registrada para ${u.fullName}.`,
        'Gestión de Roles'
      );
    }
  }

  onNoteInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.adminNote.set(value);
  }

  onSaveNote(): void {
    this.isNoteSaved.set(true);
    this.toastService.success('Nota interna de auditoría guardada.', 'Nota Admin');
    setTimeout(() => this.isNoteSaved.set(false), 2500);
  }
}

