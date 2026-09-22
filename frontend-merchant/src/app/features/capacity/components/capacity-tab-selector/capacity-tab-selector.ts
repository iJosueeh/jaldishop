import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matScheduleOutline, matEventBusyOutline } from '@ng-icons/material-symbols/outline';
import { CapacityTab } from '../../../../core/models/capacity.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matScheduleOutline,
      matEventBusyOutline,
    }),
  ],
  selector: 'app-capacity-tab-selector',
  styleUrl: './capacity-tab-selector.css',
  templateUrl: './capacity-tab-selector.html',
})
export class CapacityTabSelector {
  readonly activeTab = input<CapacityTab>('SCHEDULE');
  readonly exceptionsCount = input<number>(0);

  readonly tabChange = output<CapacityTab>();
}
