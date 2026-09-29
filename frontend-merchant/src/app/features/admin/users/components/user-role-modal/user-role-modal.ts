import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminUserRole, AdminUserSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matWarningOutline,
  matShieldOutline,
  matStorefrontOutline,
  matPersonOutline,
  matCheckOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matWarningOutline,
      matShieldOutline,
      matStorefrontOutline,
      matPersonOutline,
      matCheckOutline,
    }),
  ],
  selector: 'app-user-role-modal',
  styleUrl: './user-role-modal.css',
  templateUrl: './user-role-modal.html',
})
export class UserRoleModal {
  readonly isOpen = input<boolean>(false);
  readonly user = input<AdminUserSummary | null>(null);
  readonly targetRole = input<AdminUserRole | null>(null);
  readonly action = input<'ADD' | 'REMOVE'>('ADD');

  readonly confirm = output<{
    user: AdminUserSummary;
    targetRole: AdminUserRole;
    action: 'ADD' | 'REMOVE';
    newRoles: AdminUserRole[];
  }>();
  readonly cancel = output<void>();

  readonly calculatedNewRoles = computed<AdminUserRole[]>(() => {
    const u = this.user();
    const role = this.targetRole();
    const act = this.action();
    if (!u || !role) return [];

    if (act === 'ADD') {
      return Array.from(new Set([...u.roles, role]));
    } else {
      const filtered = u.roles.filter((r) => r !== role);
      // Al menos debe quedar un rol (CUSTOMER por defecto si se vacía)
      return filtered.length > 0 ? filtered : ['CUSTOMER'];
    }
  });

  onConfirm(): void {
    const u = this.user();
    const role = this.targetRole();
    if (u && role) {
      this.confirm.emit({
        user: u,
        targetRole: role,
        action: this.action(),
        newRoles: this.calculatedNewRoles(),
      });
    }
  }
}
