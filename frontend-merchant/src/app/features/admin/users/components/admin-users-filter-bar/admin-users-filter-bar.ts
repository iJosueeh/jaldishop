import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matSearchOutline, matCloseOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matSearchOutline, matCloseOutline })],
  selector: 'app-admin-users-filter-bar',
  styleUrl: './admin-users-filter-bar.css',
  templateUrl: './admin-users-filter-bar.html',
})
export class AdminUsersFilterBar {
  readonly searchQuery = input<string>('');
  readonly selectedRole = input<string>('ALL');
  readonly selectedStatus = input<string>('ALL');

  readonly searchChange = output<string>();
  readonly roleChange = output<string>();
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
