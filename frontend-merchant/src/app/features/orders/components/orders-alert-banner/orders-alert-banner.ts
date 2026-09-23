import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matNotificationsActiveOutline,
  matArrowForwardOutline,
} from '@ng-icons/material-symbols/outline';
import { MerchantOrder } from '../../../../core/models/order.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matNotificationsActiveOutline,
      matArrowForwardOutline,
    }),
  ],
  selector: 'app-orders-alert-banner',
  styleUrl: './orders-alert-banner.css',
  templateUrl: './orders-alert-banner.html',
})
export class OrdersAlertBanner {
  readonly urgentOrder = input<MerchantOrder | null>(null);
  readonly readyCount = input<number>(0);

  readonly viewUrgent = output<void>();
}
