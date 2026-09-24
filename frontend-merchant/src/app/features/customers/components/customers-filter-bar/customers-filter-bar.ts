import { Component, input, output } from '@angular/core';
import { CustomerFilterTab } from '../../../../core/models/customer.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matSearchOutline, matCloseOutline } from '@ng-icons/material-symbols/outline';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-customers-filter-bar',
  imports: [NgIcon, FormsModule],
  providers: [
    provideIcons({
      matSearchOutline,
      matCloseOutline,
    }),
  ],
  templateUrl: './customers-filter-bar.html',
  styleUrl: './customers-filter-bar.css',
})
export class CustomersFilterBar {
  readonly activeTab = input.required<CustomerFilterTab>();
  readonly searchQuery = input<string>('');
  readonly totalCount = input<number>(0);
  readonly vipCount = input<number>(0);
  readonly frequentCount = input<number>(0);
  readonly newCount = input<number>(0);

  readonly tabChange = output<CustomerFilterTab>();
  readonly searchChange = output<string>();

  onSelectTab(tab: CustomerFilterTab): void {
    this.tabChange.emit(tab);
  }

  onSearchInput(value: string): void {
    this.searchChange.emit(value);
  }

  onClearSearch(): void {
    this.searchChange.emit('');
  }
}
