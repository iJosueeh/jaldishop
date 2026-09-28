import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminUserSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matGroupOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [provideIcons({ matGroupOutline })],
  selector: 'app-admin-quick-users',
  styleUrl: './admin-quick-users.css',
  templateUrl: './admin-quick-users.html',
})
export class AdminQuickUsers {
  readonly users = input.required<AdminUserSummary[]>();
}
