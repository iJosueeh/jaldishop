import { Component, computed, inject, OnInit } from '@angular/core';
import { DashboardMetrics } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matViewWeekOutline,
  matReceiptLongOutline,
  matTimerOutline,
  matCheckCircleOutline,
  matEventBusyOutline,
  matBlockOutline,
  matStarOutline,
} from '@ng-icons/material-symbols/outline';
import { CapacityService } from '../../../../core/services/capacity.service';
import { OrderService } from '../../../../core/services/order.service';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matViewWeekOutline,
      matReceiptLongOutline,
      matTimerOutline,
      matCheckCircleOutline,
      matEventBusyOutline,
      matBlockOutline,
      matStarOutline,
    }),
  ],
  selector: 'app-dashboard-kpis',
  styleUrl: './dashboard-kpis.css',
  templateUrl: './dashboard-kpis.html',
})
export class DashboardKpis implements OnInit {
  private readonly capacityService = inject(CapacityService);
  readonly orderService = inject(OrderService);

  readonly todayOrderCounts = computed(() => this.orderService.todayOrderCounts());
  readonly isTodayCountsLoading = computed(() => this.orderService.isTodayCountsLoading());
  readonly todayCountsError = computed(() => this.orderService.todayCountsError());
  readonly isTodayOutdated = computed(() => this.orderService.isTodayOutdated());

  readonly todayException = computed(() => this.capacityService.todayException());
  readonly capacityTotal = computed(() => this.capacityService.todayEffectiveCapacityReal());
  readonly isClosedToday = computed(() => this.capacityService.isTodayClosedReal());

  readonly capacityOccupied = computed<number | null>(() => {
    return this.capacityService.todayOccupied();
  });

  readonly shiftSchedule = computed(() => {
    const exc = this.todayException();
    if (exc) {
      if (exc.exceptionCapacity === 0) {
        return exc.reason ? `Cerrado: ${exc.reason}` : 'Cerrado por excepción hoy';
      }
      return exc.reason ? `Fecha especial: ${exc.reason}` : 'Capacidad especial configurada hoy';
    }

    const todayConfigs = this.capacityService.todayConfigurations();
    if (todayConfigs.length === 0) {
      return 'Sin franjas configuradas hoy';
    }
    const times = todayConfigs
      .map((c) => `${c.startTime.substring(0, 5)} - ${c.endTime.substring(0, 5)}`)
      .join(', ');
    return `Horario hoy: ${times}`;
  });

  readonly metrics = computed<DashboardMetrics>(() => {
    const counts = this.todayOrderCounts();
    return {
      capacityOccupied: this.capacityOccupied(),
      capacityTotal: this.capacityTotal(),
      shiftSchedule: this.shiftSchedule(),
      totalOrdersToday: counts?.all ?? 0,
      confirmedOrders: counts?.confirmed ?? 0,
      inPrepOrders: counts?.inPreparation ?? 0,
      readyOrders: counts?.ready ?? 0,
      completedOrders: counts?.completed ?? 0,
      operatingRhythmMin: null,
      targetRhythmMin: null,
    };
  });

  readonly capacityPercentage = computed<number | null>(() => {
    return this.capacityService.todayOccupancyPercentage();
  });

  readonly availableCapacity = computed<number | null>(() => {
    return this.capacityService.todayAvailable();
  });

  readonly isOverCapacity = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    return occupied !== null && total > 0 && occupied > total;
  });

  readonly isOccupancyLoading = computed(() => this.capacityService.isLoadingOccupancy());

  readonly capacityBlocks = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    const percentage =
      occupied !== null && occupied !== undefined && total > 0
        ? (occupied / total) * 10
        : 0;

    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.min(10, Math.round(percentage)),
    }));
  });

  ngOnInit(): void {
    if (this.capacityService.configurations().length === 0) {
      this.capacityService.getConfigurations().subscribe();
    }
    if (this.capacityService.exceptions().length === 0) {
      this.capacityService.getExceptions().subscribe();
    }
    this.capacityService.loadDailyOccupancy().subscribe();
    if (this.orderService.orders().length === 0) {
      this.orderService.loadOrders().subscribe();
    }
    this.orderService.loadTodayOrderCounts().subscribe();
  }
}
