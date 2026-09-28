import { Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [SlicePipe, NgIcon],
  providers: [provideIcons({ matStorefrontOutline, matVisibilityOutline })],
  selector: 'app-admin-stores-table',
  styleUrl: './admin-stores-table.css',
  templateUrl: './admin-stores-table.html',
})
export class AdminStoresTable {
  readonly stores = input.required<AdminStoreSummary[]>();
  readonly selectStore = output<AdminStoreSummary>();
  readonly toggleStatus = output<AdminStoreSummary>();
}
