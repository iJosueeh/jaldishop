import { Component, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matAddCircleOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matAddCircleOutline })],
  selector: 'app-capacity-header',
  styleUrl: './capacity-header.css',
  templateUrl: './capacity-header.html',
})
export class CapacityHeader {
  readonly newSlot = output<void>();
}
