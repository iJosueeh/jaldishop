import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../../core/services/admin.service';
import { AdminInvitation, InviteAdminRequest } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matPersonAddOutline,
  matMailOutline,
  matCancelOutline,
  matCheckCircleOutline,
  matScheduleOutline,
  matRefreshOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, FormsModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matPersonAddOutline,
      matMailOutline,
      matCancelOutline,
      matCheckCircleOutline,
      matScheduleOutline,
      matRefreshOutline,
    }),
  ],
  selector: 'app-invite-admin-modal',
  styleUrl: './invite-admin-modal.css',
  templateUrl: './invite-admin-modal.html',
})
export class InviteAdminModal implements OnInit {
  readonly adminService = inject(AdminService);

  readonly isOpen = input<boolean>(false);
  readonly closeModal = output<void>();

  readonly activeTab = signal<'form' | 'list'>('form');

  // Form Fields
  readonly email = signal<string>('');
  readonly firstName = signal<string>('');
  readonly lastName = signal<string>('');
  readonly phone = signal<string>('');
  readonly isSubmitting = signal<boolean>(false);
  readonly formError = signal<string | null>(null);

  ngOnInit(): void {
    if (this.isOpen()) {
      this.adminService.loadInvitations().subscribe();
    }
  }

  setTab(tab: 'form' | 'list'): void {
    this.activeTab.set(tab);
    if (tab === 'list') {
      this.adminService.loadInvitations().subscribe();
    }
  }

  isFormValid(): boolean {
    const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email().trim());
    const firstNameValid = this.firstName().trim().length >= 2;
    const lastNameValid = this.lastName().trim().length >= 2;
    return emailValid && firstNameValid && lastNameValid;
  }

  onSubmit(): void {
    if (!this.isFormValid() || this.isSubmitting()) return;

    this.formError.set(null);
    this.isSubmitting.set(true);

    const request: InviteAdminRequest = {
      email: this.email().trim().toLowerCase(),
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
      phone: this.phone().trim() || undefined,
    };

    this.adminService.inviteAdmin(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.resetForm();
        this.setTab('list');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.formError.set(
          err.error?.message ||
            'Error al enviar la invitación. Verifique los datos o si el correo ya está registrado.'
        );
      },
    });
  }

  onRevoke(invitationId: string): void {
    this.adminService.revokeAdminInvitation(invitationId).subscribe();
  }

  resetForm(): void {
    this.email.set('');
    this.firstName.set('');
    this.lastName.set('');
    this.phone.set('');
    this.formError.set(null);
  }

  dismiss(): void {
    this.resetForm();
    this.activeTab.set('form');
    this.closeModal.emit();
  }
}
