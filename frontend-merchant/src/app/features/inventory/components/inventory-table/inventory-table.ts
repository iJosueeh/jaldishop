import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matInventory2Outline,
  matErrorOutline,
  matEditOutline,
  matNotificationsOutline,
} from '@ng-icons/material-symbols/outline';
import { InventoryResponse } from '../../../../core/models/inventory.models';

@Component({
  selector: 'app-inventory-table',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matInventory2Outline,
      matErrorOutline,
      matEditOutline,
      matNotificationsOutline,
    }),
  ],
  templateUrl: './inventory-table.html',
  styleUrl: './inventory-table.css',
})
export class InventoryTable {
  readonly items = input<InventoryResponse[]>([]);
  readonly isLoading = input<boolean>(false);
  readonly hasError = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);
  readonly isFilterActive = input<boolean>(false);
  readonly updatingVariantId = input<string | null>(null);

  readonly editStock = output<InventoryResponse>();
  readonly editThreshold = output<InventoryResponse>();
  readonly retry = output<void>();
  readonly clearFilters = output<void>();

  onRowClick(item: InventoryResponse): void {
    if (item.tracksInventory) {
      this.editStock.emit(item);
    }
  }
}
