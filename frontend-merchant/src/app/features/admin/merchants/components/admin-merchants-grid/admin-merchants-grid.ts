import { Component, input, output } from '@angular/core';
import { AdminUserSummary } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matStorefrontOutline,
  matPersonOutline,
  matChatOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matStorefrontOutline,
      matPersonOutline,
      matChatOutline,
      matVisibilityOutline,
    }),
  ],
  selector: 'app-admin-merchants-grid',
  styleUrl: './admin-merchants-grid.css',
  templateUrl: './admin-merchants-grid.html',
})
export class AdminMerchantsGrid {
  readonly merchants = input.required<AdminUserSummary[]>();
  readonly selectMerchant = output<AdminUserSummary>();
  readonly toggleStatus = output<AdminUserSummary>();
}
