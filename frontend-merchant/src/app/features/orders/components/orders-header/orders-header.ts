import { Component, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPrintOutline,
  matTuneOutline,
  matAddOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matPrintOutline,
      matTuneOutline,
      matAddOutline,
    }),
  ],
  selector: 'app-orders-header',
  styleUrl: './orders-header.css',
  templateUrl: './orders-header.html',
})
export class OrdersHeader {
  readonly printOrders = output<void>();
  readonly adjustPace = output<void>();
  readonly newOrder = output<void>();
}
