import { Component, input } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matInventory2Outline,
  matScheduleOutline,
  matShieldOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matInventory2Outline,
      matScheduleOutline,
      matShieldOutline,
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-capacity-kpis',
  styleUrl: './capacity-kpis.css',
  templateUrl: './capacity-kpis.html',
})
export class CapacityKpis {
  readonly dayLabel = input.required<string>();
  readonly totalCapacity = input.required<number>();
  readonly totalSlotsCount = input.required<number>();
  readonly activeSlotsCount = input.required<number>();
}
