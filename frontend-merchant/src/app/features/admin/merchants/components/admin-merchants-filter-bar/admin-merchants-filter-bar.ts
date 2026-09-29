import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matSearchOutline, matCloseOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matSearchOutline, matCloseOutline })],
  selector: 'app-admin-merchants-filter-bar',
  styleUrl: './admin-merchants-filter-bar.css',
  templateUrl: './admin-merchants-filter-bar.html',
})
export class AdminMerchantsFilterBar {
  readonly searchQuery = input<string>('');
  readonly selectedStatus = input<string>('ALL');

  readonly searchChange = output<string>();
  readonly statusChange = output<string>();

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchChange.emit(value);
  }

  clearSearch(): void {
    this.searchChange.emit('');
  }

  onStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.statusChange.emit(value);
  }
}
