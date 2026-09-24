import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matFilterAltOffOutline,
  matReceiptLongOutline,
} from '@ng-icons/material-symbols/outline';
import { MerchantOrder } from '../../../../core/models/order.models';
import { OrdersCard } from '../orders-card/orders-card';

@Component({
  selector: 'app-orders-grid',
  standalone: true,
  imports: [CommonModule, NgIcon, OrdersCard],
  viewProviders: [
    provideIcons({
      matAddOutline,
      matFilterAltOffOutline,
      matReceiptLongOutline,
    }),
  ],
  templateUrl: './orders-grid.html',
  styleUrl: './orders-grid.css',
})
export class OrdersGrid {
  orders = input<MerchantOrder[]>([]);
  isLoading = input<boolean>(false);
  isEmptyOrders = input<boolean>(false);
  isFilterEmpty = input<boolean>(false);

  statusAdvance = output<MerchantOrder>();
  whatsAppClick = output<MerchantOrder>();
  openDetails = output<MerchantOrder>();
  resetFilter = output<void>();
  createNewOrder = output<void>();
}
