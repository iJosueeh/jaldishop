import { Component, computed, inject } from '@angular/core';
import { StoreService } from '../../../store/services/store.service';

@Component({
  imports: [],
  selector: 'app-dashboard-header',
  styleUrl: './dashboard-header.css',
  templateUrl: './dashboard-header.html',
})
export class DashboardHeader {
  private readonly storeService = inject(StoreService);

  readonly storeName = computed(() => this.storeService.storeName());
  readonly storeStatus = computed(() => this.storeService.storeStatus());
  readonly isOpen = computed(() => this.storeStatus() === 'ACTIVE');

  readonly currentDate = computed(() => {
    const today = new Date();
    return today.toLocaleDateString('es-PE', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    });
  });

}
