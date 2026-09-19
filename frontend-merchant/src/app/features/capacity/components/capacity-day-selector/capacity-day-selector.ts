import { Component, input, output } from '@angular/core';
import { CapacityConfiguration, DayScheduleOption } from '../../../../core/models/capacity.models';

@Component({
  imports: [],
  selector: 'app-capacity-day-selector',
  styleUrl: './capacity-day-selector.css',
  templateUrl: './capacity-day-selector.html',
})
export class CapacityDaySelector {
  readonly days = input.required<DayScheduleOption[]>();
  readonly selectedDay = input.required<number>();
  readonly allConfigurations = input.required<CapacityConfiguration[]>();

  readonly dayChange = output<number>();

  getActiveCount(dayOfWeek: number): number {
    return this.allConfigurations().filter(
      (c) => c.dayOfWeek === dayOfWeek && c.status === 'ACTIVE',
    ).length;
  }
}
