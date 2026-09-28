import { Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matStorefrontOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [SlicePipe, NgIcon],
  providers: [provideIcons({ matStorefrontOutline })],
  selector: 'app-admin-stores-table',
  styleUrl: './admin-stores-table.css',
  templateUrl: './admin-stores-table.html',
})
export class AdminStoresTable {
  readonly stores = input.required<AdminStoreSummary[]>();
  readonly toggleStatus = output<AdminStoreSummary>();
}
