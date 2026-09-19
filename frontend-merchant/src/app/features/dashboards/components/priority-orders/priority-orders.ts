import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardPriorityOrder } from '../../../../core/models/dashboard.models';

@Component({
  imports: [RouterLink],
  selector: 'app-priority-orders',
  styleUrl: './priority-orders.css',
  templateUrl: './priority-orders.html',
})
export class PriorityOrders {
  readonly orders = signal<DashboardPriorityOrder[]>([]);

  onMarkAsReady(order: DashboardPriorityOrder): void {
    this.orders.update((list) =>
      list.map((o) => (o.id === order.id ? { ...o, status: 'READY' } : o)),
    );
  }

  onNotifyCustomer(order: DashboardPriorityOrder): void {
    this.orders.update((list) => list.filter((o) => o.id !== order.id));
  }
}
