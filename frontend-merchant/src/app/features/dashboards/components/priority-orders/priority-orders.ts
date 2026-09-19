import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardPriorityOrder } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matArrowForwardOutline,
  matRestaurantOutline,
  matChatOutline,
  matScheduleOutline,
  matCheckCircleOutline,
  matDoneAllOutline,
  matSendOutline,
  matVisibilityOutline,
  matTaskAltOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
  providers: [
    provideIcons({
      matArrowForwardOutline,
      matRestaurantOutline,
      matChatOutline,
      matScheduleOutline,
      matCheckCircleOutline,
      matDoneAllOutline,
      matSendOutline,
      matVisibilityOutline,
      matTaskAltOutline,
    }),
  ],
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
