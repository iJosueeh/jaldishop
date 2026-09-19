import { Component, computed, inject } from '@angular/core';
import { StoreService } from '../../../../core/services/store.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matCalendarMonthOutline,
  matAddCircleOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCalendarMonthOutline,
      matAddCircleOutline,
    }),
  ],
  selector: 'app-dashboard-header',
  styleUrl: './dashboard-header.css',
  templateUrl: './dashboard-header.html',
})
export class DashboardHeader {
  private readonly storeService = inject(StoreService);
  private readonly profileService = inject(ProfileService);

  readonly firstName = computed(() => {
    const profile = this.profileService.currentProfile();
    return profile?.firstName || 'Comerciante';
  })

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
