import { Component, computed, signal } from '@angular/core';
import { DashboardMetrics } from '../../interface/dashboard.models';

@Component({
  imports: [],
  selector: 'app-dashboard-kpis',
  styleUrl: './dashboard-kpis.css',
  templateUrl: './dashboard-kpis.html',
})
export class DashboardKpis {
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

  readonly capacityPercentage = computed(() => {
    const total = this.metrics().capacityTotal;
    if (total <= 0) return 0;
    return Math.round((this.metrics().capacityOccupied / total) * 100);
  });

  readonly availableCapacity = computed(() => {
    return Math.max(0, this.metrics().capacityTotal - this.metrics().capacityOccupied);
  });

  readonly capacityBlocks = computed(() => {
    const occupied = this.metrics().capacityOccupied;
    const total = this.metrics().capacityTotal || 10;
    const ratio = Math.min(1, Math.max(0, occupied / total));
    const filledBlocks = Math.round(ratio * 10);

    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < filledBlocks,
    }));
  });
}
