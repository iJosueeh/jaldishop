import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matSearchOutline, matCloseOutline } from '@ng-icons/material-symbols/outline';
import { InventoryFilterStatus } from '../../../../core/services/inventory.service';

@Component({
  selector: 'app-inventory-filter-bar',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matSearchOutline,
      matCloseOutline,
    }),
  ],
  templateUrl: './inventory-filter-bar.html',
  styleUrl: './inventory-filter-bar.css',
})
export class InventoryFilterBar {
  readonly searchQuery = input<string>('');
  readonly selectedFilter = input<InventoryFilterStatus>('ALL');
  readonly configuredCount = input<number>(0);
  readonly notConfiguredCount = input<number>(0);
  readonly lowStockCount = input<number>(0);

  readonly searchChange = output<string>();
  readonly filterChange = output<InventoryFilterStatus>();
  readonly clearFilters = output<void>();

  onSearchInput(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.searchChange.emit(inputElement.value);
  }

  onClearSearch(): void {
    this.searchChange.emit('');
  }
}
