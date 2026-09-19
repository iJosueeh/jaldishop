import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StockAlertItem, UpcomingDeliveryItem } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocalShippingOutline,
  matInventory2Outline,
  matArrowForwardOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matLocalShippingOutline,
      matInventory2Outline,
      matArrowForwardOutline,
    }),
  ],
  selector: 'app-logistics-alerts',
  styleUrl: './logistics-alerts.css',
  templateUrl: './logistics-alerts.html',
})
export class LogisticsAlerts {
  readonly upcomingDeliveries = signal<UpcomingDeliveryItem[]>([]);
  readonly stockAlerts = signal<StockAlertItem[]>([]);
}
