import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matReceiptLongOutline } from '@ng-icons/material-symbols/outline';
import { MerchantOrder } from '../../../../core/models/order.models';
import { OrdersCard } from '../orders-card/orders-card';

@Component({
  selector: 'app-orders-grid',
  standalone: true,
  imports: [CommonModule, NgIcon, OrdersCard],
  viewProviders: [
    provideIcons({
      matReceiptLongOutline,
    }),
  ],
  templateUrl: './orders-grid.html',
  styleUrl: './orders-grid.css',
})
export class OrdersGrid {
  orders = input<MerchantOrder[]>([]);

  statusAdvance = output<MerchantOrder>();
  whatsAppClick = output<MerchantOrder>();
  openDetails = output<MerchantOrder>();
  resetFilter = output<void>();
}
