import { Component, input, output } from '@angular/core';
import { CapacityException } from '../../../../core/models/capacity.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCalendarTodayOutline,
  matScheduleOutline,
  matAddOutline,
  matEventBusyOutline,
  matBlockOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCalendarTodayOutline,
      matScheduleOutline,
      matAddOutline,
      matEventBusyOutline,
      matBlockOutline,
    }),
  ],
  selector: 'app-capacity-exceptions-list',
  styleUrl: './capacity-exceptions-list.css',
  templateUrl: './capacity-exceptions-list.html',
})
export class CapacityExceptionsList {
  readonly exceptions = input<CapacityException[]>();
  readonly isLoading = input<boolean>(false);

  readonly toggleStatus = output<CapacityException>();
  readonly createException = output<void>();

  formatDate(dateStr: string): string {
    if (!dateStr) return '';

    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('es-PE', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  }

  formatTimeRange(startTime: string | null, endTime: string | null): string {
    if (!startTime || !endTime) {
      return 'Todo el día';
    }
    const cleanStart = startTime.substring(0, 5);
    const cleanEnd = endTime.substring(0, 5);
    return `${cleanStart} - ${cleanEnd}`;
  }
}
