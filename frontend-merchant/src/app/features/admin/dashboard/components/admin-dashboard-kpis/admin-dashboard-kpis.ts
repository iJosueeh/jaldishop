import { Component, input } from '@angular/core';
import { AdminDashboardMetrics } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matGroupOutline,
  matPersonOutline,
  matStorefrontOutline,
  matShieldOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matGroupOutline,
      matPersonOutline,
      matStorefrontOutline,
      matShieldOutline,
    }),
  ],
  selector: 'app-admin-dashboard-kpis',
  styleUrl: './admin-dashboard-kpis.css',
  templateUrl: './admin-dashboard-kpis.html',
})
export class AdminDashboardKpis {
  readonly metrics = input.required<AdminDashboardMetrics>();
}
