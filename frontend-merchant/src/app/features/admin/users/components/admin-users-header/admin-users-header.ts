import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matRefreshOutline })],
  selector: 'app-admin-users-header',
  styleUrl: './admin-users-header.css',
  templateUrl: './admin-users-header.html',
})
export class AdminUsersHeader {
  readonly totalCount = input.required<number>();
  readonly isLoading = input<boolean>(false);
  readonly refresh = output<void>();
}
