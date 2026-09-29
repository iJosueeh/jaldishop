import { Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { AdminUserSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matGroupOutline,
  matStorefrontOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [SlicePipe, NgIcon],
  providers: [
    provideIcons({
      matGroupOutline,
      matStorefrontOutline,
      matVisibilityOutline,
    }),
  ],
  selector: 'app-admin-users-table',
  styleUrl: './admin-users-table.css',
  templateUrl: './admin-users-table.html',
})
export class AdminUsersTable {
  readonly users = input.required<AdminUserSummary[]>();
  readonly selectUser = output<AdminUserSummary>();
  readonly toggleStatus = output<AdminUserSummary>();
}
