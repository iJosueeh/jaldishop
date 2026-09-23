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
  readonly capacityTotal = input<number>(10);
  readonly capacityOccupied = input<number>(8);
  readonly remainingDeliveries = input<number>(4);
  readonly inPrepCount = input<number>(2);
  readonly readyCount = input<number>(2);
  readonly averagePaceMin = input<number>(18);

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
}
