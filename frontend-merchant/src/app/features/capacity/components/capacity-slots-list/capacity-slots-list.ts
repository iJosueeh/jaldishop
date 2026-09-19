import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAlarmOutline,
  matShoppingBagOutline,
  matPauseCircleOutline,
  matPlayCircleOutline,
  matCalendarTodayOutline,
  matAddOutline,
} from '@ng-icons/material-symbols/outline';
import { CapacityConfiguration } from '../../../../core/models/capacity.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matAlarmOutline,
      matShoppingBagOutline,
      matPauseCircleOutline,
      matPlayCircleOutline,
      matCalendarTodayOutline,
      matAddOutline,
    }),
  ],
  selector: 'app-capacity-slots-list',
  styleUrl: './capacity-slots-list.css',
  templateUrl: './capacity-slots-list.html',
})
export class CapacitySlotsList {
  readonly slots = input.required<CapacityConfiguration[]>();
  readonly dayLabel = input.required<string>();
  readonly isLoading = input.required<boolean>();

  readonly toggleStatus = output<CapacityConfiguration>();
  readonly createSlot = output<void>();
}
