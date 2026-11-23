import { Component, ElementRef, HostListener, inject, input, output, viewChild } from '@angular/core';
import { ProfileService } from '../../../../core/services/profile.service';
import { OrderService } from '../../../../core/services/order.service';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { NotificationCenter } from '../notification-center/notification-center';
import { ContextSwitcher } from '../../../../shared/components/context-switcher/context-switcher';
import {
  matMenuOutline,
  matSearchOutline,
  matSpeedOutline,
  matAddCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon, NotificationCenter, ContextSwitcher],
  providers: [
    provideIcons({
      matMenuOutline,
      matSearchOutline,
      matSpeedOutline,
      matAddCircleOutline,
    }),
  ],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly profileService = inject(ProfileService);
  readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  readonly toggleSidebar = output<void>();
  readonly operationRhythmMin = input<number | null>(0);
  readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  @HostListener('window:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.searchInput()?.nativeElement.focus();
    }
  }

  onSearch(query: string): void {
    this.orderService.setSearchQuery(query);
    if (query.trim() && !this.router.url.startsWith('/orders')) {
      this.router.navigate(['/orders']);
    }
  }

  handleNewOrder(): void {
    this.orderService.openCreateModal();
  }
}
