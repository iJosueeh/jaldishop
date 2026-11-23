import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matWarehouseOutline, matRefreshOutline } from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-inventory-header',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matWarehouseOutline,
      matRefreshOutline,
    }),
  ],
  templateUrl: './inventory-header.html',
  styleUrl: './inventory-header.css',
})
export class InventoryHeader {
  readonly totalCount = input<number>(0);
  readonly configuredCount = input<number>(0);
  readonly notConfiguredCount = input<number>(0);
  readonly lowStockCount = input<number>(0);
  readonly isLoading = input<boolean>(false);

  readonly refresh = output<void>();
}
