import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matScheduleOutline,
  matSearchOutline,
  matCloseOutline,
  matFilterListOutline,
  matArrowDropDownOutline,
} from '@ng-icons/material-symbols/outline';
import { OrderFilterTab } from '../../../../core/models/order.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matScheduleOutline,
      matSearchOutline,
      matCloseOutline,
      matFilterListOutline,
      matArrowDropDownOutline,
    }),
  ],
  selector: 'app-orders-filter-bar',
  styleUrl: './orders-filter-bar.css',
  templateUrl: './orders-filter-bar.html',
})
export class OrdersFilterBar {
  readonly activeTab = input<OrderFilterTab>('ALL');
  readonly totalCount = input<number | string>(0);
  readonly confirmedCount = input<number | string>(0);
  readonly inPrepCount = input<number | string>(0);
  readonly readyCount = input<number | string>(0);
  readonly completedCount = input<number | string>(0);
  readonly searchQuery = input<string>('');
  readonly selectedChannel = input<string>('ALL');

  readonly tabChange = output<OrderFilterTab>();
  readonly searchChange = output<string>();
  readonly channelChange = output<string>();
  readonly clearSearch = output<void>();

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchChange.emit(value);
  }

  onClearSearch(): void {
    this.clearSearch.emit();
  }

  onChannelSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.channelChange.emit(value);
  }
}
