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
  private readonly orderService = inject(OrderService);

  readonly todayException = computed(() => this.capacityService.todayException());
  readonly capacityTotal = computed(() => this.capacityService.todayEffectiveCapacity());
  readonly isClosedToday = computed(() => this.capacityService.isTodayClosed());

  readonly capacityOccupied = computed<number | null>(() => {
    // No presentar como ocupada la cantidad de pedidos de la página en memoria (Fase 1)
    return null;
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

  readonly metrics = computed<DashboardMetrics>(() => ({
    capacityOccupied: this.capacityOccupied() ?? 0,
    capacityTotal: this.capacityTotal(),
    shiftSchedule: this.shiftSchedule(),
    totalOrdersToday: this.orderService.totalOrdersCount(),
    confirmedOrders: this.orderService.confirmedCount(),
    inPrepOrders: this.orderService.inPrepCount(),
    readyOrders: this.orderService.readyCount(),
    completedOrders: this.orderService.completedCount(),
    operatingRhythmMin: 0,
    targetRhythmMin: 25,
  }));

  readonly capacityPercentage = computed<number | null>(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    if (occupied === null || total <= 0) return null;
    return Math.round((occupied / total) * 100);
  });

  readonly availableCapacity = computed<number | null>(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    if (occupied === null) return null;
    return Math.max(0, total - occupied);
  });

  readonly capacityBlocks = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    const percentage = occupied !== null && total > 0 ? (occupied / total) * 10 : 0;

    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.round(percentage),
    }));
  });

  ngOnInit(): void {
    if (this.capacityService.configurations().length === 0) {
      this.capacityService.getConfigurations().subscribe();
    }
    if (this.capacityService.exceptions().length === 0) {
      this.capacityService.getExceptions().subscribe();
    }
    if (this.orderService.orders().length === 0) {
      this.orderService.loadOrders().subscribe();
    }
  }
}
