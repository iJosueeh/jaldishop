import { Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StockAlertItem, UpcomingDeliveryItem } from '../../../../core/models/dashboard.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocalShippingOutline,
  matInventory2Outline,
  matArrowForwardOutline,
} from '@ng-icons/material-symbols/outline';
import { OrderService } from '../../../../core/services/order.service';
import { ProductService } from '../../../../core/services/product.service';
import { StoreService } from '../../../../core/services/store.service';

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
export class LogisticsAlerts implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly storeService = inject(StoreService);

  readonly upcomingDeliveries = computed<UpcomingDeliveryItem[]>(() => {
    return this.orderService
      .orders()
      .filter((o) => o.deliveryMode === 'DELIVERY' && o.status !== 'COMPLETED' && o.status !== 'CANCELLED')
      .map((o) => ({
        id: o.id,
        customerName: o.customerName,
        deliveryMode: o.deliveryMode,
        scheduledTime: o.scheduledTime || '12:00:00',
        timeLabel: o.deliveryTimeLabel || 'Hoy',
        urgency: o.isUrgent ? 'CRITICAL' : 'NORMAL',
      }));
  });

  readonly stockAlerts = computed<StockAlertItem[]>(() => {
    return this.productService
      .products()
      .filter((p) => p.status === 'INACTIVE')
      .map((p) => ({
        id: p.id,
        productName: p.name,
        availableUnits: 0,
        stockStatus: 'OUT_OF_STOCK',
      }));
  });

  ngOnInit(): void {
    if (this.orderService.orders().length === 0) {
      this.orderService.loadOrders().subscribe();
    }
    const storeId = this.storeService.currentStore()?.id;
    if (storeId && this.productService.products().length === 0) {
      this.productService.loadProducts(storeId).subscribe();
    }
  }
}
