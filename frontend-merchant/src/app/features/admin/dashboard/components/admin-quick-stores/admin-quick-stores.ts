import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminStoreSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [provideIcons({ matStorefrontOutline, matVisibilityOutline })],
  selector: 'app-admin-quick-stores',
  styleUrl: './admin-quick-stores.css',
  templateUrl: './admin-quick-stores.html',
})
export class AdminQuickStores {
  readonly stores = input.required<AdminStoreSummary[]>();
  readonly selectStore = output<AdminStoreSummary>();
}
