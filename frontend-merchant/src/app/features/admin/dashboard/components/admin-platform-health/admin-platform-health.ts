import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminDashboardMetrics } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCheckCircleOutline,
  matWarningOutline,
  matBoltOutline,
  matShieldOutline,
  matStorefrontOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matCheckCircleOutline,
      matWarningOutline,
      matBoltOutline,
      matShieldOutline,
      matStorefrontOutline,
    }),
  ],
  selector: 'app-admin-platform-health',
  styleUrl: './admin-platform-health.css',
  templateUrl: './admin-platform-health.html',
})
export class AdminPlatformHealth {
  readonly metrics = input.required<AdminDashboardMetrics>();
  readonly isLoading = input<boolean>(false);

  readonly filterSuspended = output<void>();

  readonly activeRatio = computed(() => {
    const total = this.metrics().totalStores;
    if (total === 0) return 100;
    return Math.round((this.metrics().activeStores / total) * 100);
  });

  readonly suspendedRatio = computed(() => {
    const total = this.metrics().totalStores;
    if (total === 0) return 0;
    return Math.round((this.metrics().suspendedStores / total) * 100);
  });

  readonly hasIssues = computed(() => {
    return this.metrics().suspendedStores > 0;
  });
}
