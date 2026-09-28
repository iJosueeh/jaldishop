import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matRefreshOutline })],
  selector: 'app-admin-merchants-header',
  styleUrl: './admin-merchants-header.css',
  templateUrl: './admin-merchants-header.html',
})
export class AdminMerchantsHeader {
  readonly totalCount = input.required<number>();
  readonly isLoading = input<boolean>(false);
  readonly refresh = output<void>();
}
