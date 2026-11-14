import { Component, computed, input } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocalShippingOutline,
  matTimerOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matLocalShippingOutline,
      matTimerOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-orders-kpis',
  styleUrl: './orders-kpis.css',
  templateUrl: './orders-kpis.html',
})
export class OrdersKpis {
  readonly capacityTotal = input<number>(0);
  readonly capacityOccupied = input<number | null>(null);
  readonly remainingDeliveries = input<number>(0);
  readonly inPrepCount = input<number>(0);
  readonly readyCount = input<number>(0);
  readonly averagePaceMin = input<number | null>(null);

  readonly hasCapacityOccupied = computed(() => {
    return this.capacityOccupied() !== null && this.capacityOccupied() !== undefined;
  });

  readonly hasAveragePace = computed(() => {
    const pace = this.averagePaceMin();
    return pace !== null && pace !== undefined && pace > 0;
  });

  readonly availableCapacity = computed(() => {
    const occupied = this.capacityOccupied();
    if (occupied === null || occupied === undefined) return null;
    return Math.max(0, this.capacityTotal() - occupied);
  });

  readonly capacityBlocks = computed(() => {
    const occupied = this.capacityOccupied();
    const total = this.capacityTotal();
    const percentage =
      occupied !== null && occupied !== undefined && total > 0
        ? (occupied / total) * 10
        : 0;

    return Array.from({ length: 10 }, (_, i) => ({
      index: i,
      isFilled: i < Math.round(percentage),
    }));
  });
}
