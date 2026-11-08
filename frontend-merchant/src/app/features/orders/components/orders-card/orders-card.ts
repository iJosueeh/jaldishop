import { Component, input, output } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matChatOutline,
  matStorefrontOutline,
  matPhotoCameraOutline,
  matTimerOutline,
  matScheduleOutline,
  matPersonOutline,
  matMopedOutline,
  matStoreOutline,
  matTwoWheelerOutline,
  matCheckOutline,
  matDoneAllOutline,
  matCheckCircleOutline,
  matVerifiedOutline,
  matReceiptOutline,
  matMoreVertOutline,
  matPendingOutline,
  matAttachmentOutline,
  matSendOutline,
  matInventory2Outline,
} from '@ng-icons/material-symbols/outline';
import { DeliveryMode, MerchantOrder, OrderStatus } from '../../../../core/models/order.models';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matChatOutline,
      matStorefrontOutline,
      matPhotoCameraOutline,
      matTimerOutline,
      matScheduleOutline,
      matPersonOutline,
      matMopedOutline,
      matStoreOutline,
      matTwoWheelerOutline,
      matCheckOutline,
      matDoneAllOutline,
      matCheckCircleOutline,
      matVerifiedOutline,
      matReceiptOutline,
      matMoreVertOutline,
      matPendingOutline,
      matAttachmentOutline,
      matSendOutline,
      matInventory2Outline,
    }),
  ],
  selector: 'app-orders-card',
  styleUrl: './orders-card.css',
  templateUrl: './orders-card.html',
})
export class OrdersCard {
  readonly order = input.required<MerchantOrder>();
  readonly isUpdating = input<boolean>(false);
  readonly isPendingVerification = input<boolean>(false);

  readonly statusAdvance = output<MerchantOrder>();
  readonly whatsAppClick = output<MerchantOrder>();
  readonly openDetails = output<MerchantOrder>();
  readonly verifyStatus = output<MerchantOrder>();

  getNextActionLabel(status: OrderStatus, deliveryMode?: DeliveryMode): string {
    switch (status) {
      case 'CONFIRMED':
        return 'Aceptar';
      case 'IN_PREPARATION':
        return 'Listo';
      case 'READY':
        return deliveryMode === 'DELIVERY' ? 'Despachar' : 'Entregar';
      case 'OUT_FOR_DELIVERY':
        return 'Entregar';
      default:
        return 'Detalle';
    }
  }

  getNextActionIcon(status: OrderStatus, deliveryMode?: DeliveryMode): string {
    switch (status) {
      case 'CONFIRMED':
        return 'matCheckCircleOutline';
      case 'IN_PREPARATION':
        return 'matDoneAllOutline';
      case 'READY':
        return deliveryMode === 'DELIVERY' ? 'matTwoWheelerOutline' : 'matCheckCircleOutline';
      case 'OUT_FOR_DELIVERY':
        return 'matCheckCircleOutline';
      default:
        return 'matCheckOutline';
    }
  }
}
