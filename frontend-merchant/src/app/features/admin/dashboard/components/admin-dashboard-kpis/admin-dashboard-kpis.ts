import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminAnalyticsSummary, AdminDashboardMetrics } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matGroupOutline,
  matPersonOutline,
  matStorefrontOutline,
  matShieldOutline,
  matPaymentsOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matGroupOutline,
      matPersonOutline,
      matStorefrontOutline,
      matShieldOutline,
      matPaymentsOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-admin-dashboard-kpis',
  styleUrl: './admin-dashboard-kpis.css',
  templateUrl: './admin-dashboard-kpis.html',
})
export class AdminDashboardKpis {
  readonly metrics = input.required<AdminDashboardMetrics>();
  readonly analytics = input<AdminAnalyticsSummary | null>(null);
}
