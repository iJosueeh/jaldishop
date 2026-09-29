import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matRefreshOutline })],
  selector: 'app-admin-stores-header',
  styleUrl: './admin-stores-header.css',
  templateUrl: './admin-stores-header.html',
})
export class AdminStoresHeader {
  readonly totalCount = input.required<number>();
  readonly isLoading = input<boolean>(false);
  readonly refresh = output<void>();
}
