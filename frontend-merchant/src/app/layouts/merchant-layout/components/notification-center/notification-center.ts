import { DatePipe } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCheckCircleOutline,
  matCloseOutline,
  matInfoOutline,
  matNotificationsOutline,
  matRefreshOutline,
  matShoppingBagOutline,
  matSyncOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';
import { Notification, NotificationType } from '../../../../core/models/notification.models';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [DatePipe, NgIcon],
  providers: [
    provideIcons({
      matNotificationsOutline,
      matRefreshOutline,
      matCloseOutline,
      matCheckCircleOutline,
      matShoppingBagOutline,
      matSyncOutline,
      matWarningOutline,
      matInfoOutline,
    }),
  ],
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.css',
})
export class NotificationCenter implements OnInit {
  private readonly elementRef = inject(ElementRef);
  readonly notificationService = inject(NotificationService);

  readonly isOpen = signal<boolean>(false);

  ngOnInit(): void {
    // Consulta inicial del conteo de no leídas para pintar el badge sin bloquear el renderizado
    this.notificationService.getUnreadCount().subscribe();
  }

  togglePanel(): void {
    const nextState = !this.isOpen();
    this.isOpen.set(nextState);

    if (nextState) {
      this.loadNotifications();
    }
  }

  close(): void {
    this.isOpen.set(false);
  }

  loadNotifications(): void {
    this.notificationService.getNotifications(0, 20).subscribe();
    this.notificationService.getUnreadCount().subscribe();
  }

  onMarkAsRead(notification: Notification, event: Event): void {
    event.stopPropagation();
    if (notification.status === 'READ' || this.notificationService.isMarkingRead(notification.id)) {
      return;
    }
    this.notificationService.markAsRead(notification.id).subscribe();
  }

  getIconName(type: NotificationType): string {
    switch (type) {
      case 'NEW_ORDER':
        return 'matShoppingBagOutline';
      case 'ORDER_STATUS_CHANGED':
        return 'matSyncOutline';
      case 'LOW_STOCK':
        return 'matWarningOutline';
      case 'SYSTEM':
      default:
        return 'matInfoOutline';
    }
  }

  getIconClasses(type: NotificationType): string {
    switch (type) {
      case 'NEW_ORDER':
        return 'text-emerald-700 bg-emerald-50 border-emerald-200/60';
      case 'ORDER_STATUS_CHANGED':
        return 'text-blue-700 bg-blue-50 border-blue-200/60';
      case 'LOW_STOCK':
        return 'text-amber-700 bg-amber-50 border-amber-200/60';
      case 'SYSTEM':
      default:
        return 'text-stone-700 bg-stone-100 border-stone-200';
    }
  }

  getTypeLabel(type: NotificationType): string {
    switch (type) {
      case 'NEW_ORDER':
        return 'Nuevo Pedido';
      case 'ORDER_STATUS_CHANGED':
        return 'Estado de Pedido';
      case 'LOW_STOCK':
        return 'Stock Crítico';
      case 'SYSTEM':
      default:
        return 'Sistema';
    }
  }

  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    if (this.isOpen()) {
      this.close();
    }
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent): void {
    if (!this.isOpen()) {
      return;
    }
    const clickedInside = this.elementRef.nativeElement.contains(event.target);
    if (!clickedInside) {
      this.close();
    }
  }
}
