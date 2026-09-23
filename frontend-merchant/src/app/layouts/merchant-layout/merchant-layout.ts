import { Component, inject, OnInit, signal } from '@angular/core';
import { Siderbar } from './components/siderbar/siderbar';
import { Header } from './components/header/header';
import { RouterOutlet } from '@angular/router';
import { StoreService } from '../../core/services/store.service';
import { ProfileService } from '../../core/services/profile.service';
import { OrderService } from '../../core/services/order.service';
import { CreateOrderModal } from '../../shared/components/create-order-modal/create-order-modal';
import { MerchantOrder } from '../../core/models/order.models';

@Component({
  imports: [Siderbar, Header, RouterOutlet, CreateOrderModal],
  selector: 'app-merchant-layout',
  styleUrl: './merchant-layout.css',
  templateUrl: './merchant-layout.html',
})
export class MerchantLayout implements OnInit {
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);
  readonly orderService = inject(OrderService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  ngOnInit(): void {
    if (!this.storeService.hasStore()) {
      this.storeService.getMyStore().subscribe({
        error: (err) => console.error('Error al cargar la tienda del comerciante: ', err),
      });
    }

    if (!this.profileService.currentProfile()) {
      this.profileService.getMyProfile().subscribe({
        error: (err) => console.error('Error al cargar el perfil del usuario: ', err),
      });
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  onOrderCreated(order: MerchantOrder): void {
    this.orderService.addOrder(order);
  }
}
