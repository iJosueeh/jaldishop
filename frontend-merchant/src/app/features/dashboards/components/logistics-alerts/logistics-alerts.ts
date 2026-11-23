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
import { InventoryService } from '../../../../core/services/inventory.service';

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
  private readonly inventoryService = inject(InventoryService);

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

  /**
   * Alertas de stock reales provistas por GET /api/v1/inventory/low-stock.
   * Elimina la equivalencia ficticia entre status INACTIVE y stock agotado.
   * Determina OUT_OF_STOCK si physicalQuantity <= 0; de lo contrario LOW_STOCK.
   */
  readonly stockAlerts = computed<StockAlertItem[]>(() => {
    return this.inventoryService.lowStock().map((item) => {
      const physical = item.physicalQuantity ?? 0;
      const available = item.availableQuantity ?? physical;
      const isOutOfStock = physical <= 0;
      const productName =
        item.presentationName && item.presentationName.trim().length > 0
          ? `${item.productName} (${item.presentationName.trim()})`
          : item.productName;

      return {
        id: item.variantId,
        productName,
        availableUnits: available,
        stockStatus: isOutOfStock ? 'OUT_OF_STOCK' : 'LOW_STOCK',
      };
    });
  });

  ngOnInit(): void {
    if (this.orderService.orders().length === 0) {
      this.orderService.loadOrders().subscribe({ error: () => {} });
    }
    if (!this.inventoryService.isLowStockLoaded()) {
      this.inventoryService.getLowStockInventory().subscribe({ error: () => {} });
    }
  }
}
