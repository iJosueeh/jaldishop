import { Component, inject, OnInit, signal } from '@angular/core';
import { Siderbar } from './components/siderbar/siderbar';
import { Header } from './components/header/header';
import { RouterOutlet } from '@angular/router';
import { StoreService } from '../../features/store/services/store.service';

@Component({
  imports: [Siderbar, Header, RouterOutlet],
  selector: 'app-merchant-layout',
  styleUrl: './merchant-layout.css',
  templateUrl: './merchant-layout.html',
})
export class MerchantLayout implements OnInit {
  private readonly storeService = inject(StoreService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  ngOnInit(): void {
    if (!this.storeService.hasStore()) {
      this.storeService.getMyStore().subscribe({
        error: (err) => console.error("Error al cargar la tienda del comerciante: ", err),
      });
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }
}
