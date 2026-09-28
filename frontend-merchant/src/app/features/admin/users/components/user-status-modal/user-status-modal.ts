import { Component, input, output } from '@angular/core';
import { AdminUserSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCloseOutline,
  matWarningOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCloseOutline,
      matWarningOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-user-status-modal',
  styleUrl: './user-status-modal.css',
  templateUrl: './user-status-modal.html',
})
export class UserStatusModal {
  readonly isOpen = input<boolean>(false);
  readonly user = input<AdminUserSummary | null>(null);

  readonly confirm = output<AdminUserSummary>();
  readonly cancel = output<void>();
}
