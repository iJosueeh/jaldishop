import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DashboardMetrics } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matViewWeekOutline,
  matReceiptLongOutline,
  matTimerOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';
import { CapacityService } from '../../../../core/services/capacity.service';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matViewWeekOutline,
      matReceiptLongOutline,
      matTimerOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-dashboard-kpis',
  styleUrl: './dashboard-kpis.css',
  templateUrl: './dashboard-kpis.html',
})
export class DashboardKpis implements OnInit {
  private readonly capacityService = inject(CapacityService);

  readonly capacityTotal = computed(() => this.capacityService.todayTotalCapacity());
  readonly capacityOccupied = signal<number>(0);

  readonly metrics = signal<DashboardMetrics>({
    capacityOccupied: 0,
    capacityTotal: 0,
    shiftSchedule: 'Sin pedidos hoy',
    totalOrdersToday: 0,
    confirmedOrders: 0,
    inPrepOrders: 0,
    readyOrders: 0,
    completedOrders: 0,
    operatingRhythmMin: 0,
    targetRhythmMin: 0,
  });

  readonly shiftSchedule = computed(() => {
    const todayConfigs = this.capacityService.todayConfigurations();
    if (todayConfigs.length === 0) {
      return 'Sin franjas configuradas hoy';
    }
    const times = todayConfigs
      .map((c) => `${c.startTime.substring(0, 5)} - ${c.endTime.substring(0, 5)}`)
      .join(', ');
    return `Horario hoy: ${times}`;
  });

  readonly capacityPercentage = computed(() => {
    const total = this.capacityTotal();
    if (total <= 0) return 0;
    return Math.round((this.capacityOccupied() / total) * 100);
  });

  readonly availableCapacity = computed(() => {
    return Math.max(0, this.capacityTotal() - this.capacityOccupied());
  });

  readonly capacityBlocks = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    const percentage = total > 0 ? (occupied / total) * 10 : 0;

    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.round(percentage),
    }));
  });

  ngOnInit(): void {
    if (this.capacityService.configurations().length === 0) {
      this.capacityService.getConfigurations().subscribe();
    }
  }
}
