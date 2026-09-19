import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StockAlertItem, UpcomingDeliveryItem } from '../../../../core/models/dashboard.models';

@Component({
  imports: [RouterLink],
  selector: 'app-logistics-alerts',
  styleUrl: './logistics-alerts.css',
  templateUrl: './logistics-alerts.html',
})
export class LogisticsAlerts {
  readonly upcomingDeliveries = signal<UpcomingDeliveryItem[]>([]);
  readonly stockAlerts = signal<StockAlertItem[]>([]);
}
