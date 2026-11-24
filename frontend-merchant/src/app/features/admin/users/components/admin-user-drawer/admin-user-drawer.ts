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
import { AdminService } from '../../../../../core/services/admin.service';
import { ToastService } from '../../../../../core/services/toast.service';
import { AuthService } from '../../../../../core/services/auth-service';
import { ErrorHandlerService } from '../../../../../core/services/error-handler.service';
import { ApiErrorMetadata } from '../../../../../core/models/api-error.models';
import { DestructiveActionModal } from '../../../../../shared/components/destructive-action-modal/destructive-action-modal';
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
  matDeleteOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon, DestructiveActionModal],
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
      matDeleteOutline,
    }),
  ],
  selector: 'app-admin-user-drawer',
  styleUrl: './admin-user-drawer.css',
  templateUrl: './admin-user-drawer.html',
})
export class AdminUserDrawer {
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);

  readonly user = input<AdminUserSummary | null>(null);
  readonly isOpen = input<boolean>(false);
  readonly closeDrawer = output<void>();
  readonly statusChange = output<AdminUserSummary>();
  readonly roleToggle = output<{ user: AdminUserSummary; role: AdminUserRole }>();
  readonly roleChangeRequest = output<{
    user: AdminUserSummary;
    targetRole: AdminUserRole;
    action: 'ADD' | 'REMOVE';
  }>();

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
    if (!u) return;
    this.authService.forgotPassword(u.email).subscribe({
      next: () => {
        this.toastService.success(
          `Enlace de restablecimiento de contraseña enviado a ${u.email}`,
          'Acción de Soporte'
        );
      },
      error: () => {
        this.toastService.error('No se pudo enviar el enlace de restablecimiento.', 'Error');
      },
    });
  }

  onToggleRole(role: AdminUserRole): void {
    const u = this.user();
    if (u) {
      const isAssigned = u.roles.includes(role);
      const action = isAssigned ? 'REMOVE' : 'ADD';
      this.roleToggle.emit({ user: u, role });
      this.roleChangeRequest.emit({ user: u, targetRole: role, action });
    }
  }

  onNoteInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.adminNote.set(value);
  }

  onSaveNote(): void {
    this.toastService.info('La persistencia de notas internas estará disponible próximamente.', 'Nota Admin');
  }

  private readonly adminService = inject(AdminService);
  private readonly errorHandler = inject(ErrorHandlerService);
  readonly isDeleteModalOpen = signal<boolean>(false);
  readonly isDeletingUser = signal<boolean>(false);
  readonly deleteErrorStatus = signal<number | null>(null);
  readonly deleteErrorMessage = signal<string | null>(null);
  readonly deleteConflictMetadata = signal<ApiErrorMetadata | null>(null);

  requestDeleteUser(): void {
    this.deleteErrorStatus.set(null);
    this.deleteErrorMessage.set(null);
    this.deleteConflictMetadata.set(null);
    this.isDeleteModalOpen.set(true);
  }

  cancelDeleteUser(): void {
    if (!this.isDeletingUser()) {
      this.isDeleteModalOpen.set(false);
    }
  }

  confirmDeleteUser(): void {
    const u = this.user();
    if (!u) return;
    this.isDeletingUser.set(true);
    this.adminService.deleteUser(u.id).subscribe({
      next: (res) => {
        this.isDeletingUser.set(false);
        this.isDeleteModalOpen.set(false);
        this.toastService.success(res.message, 'Usuario Eliminado');
        this.dismiss();
      },
      error: (err) => {
        this.isDeletingUser.set(false);
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

  fallbackSuspendUser(): void {
    this.isDeleteModalOpen.set(false);
    this.onToggleStatus();
  }
}

