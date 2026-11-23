import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { ConfirmModal } from '../../../../shared/components/confirm-modal/confirm-modal';
import {
  matBoltOutline,
  matPauseCircleOutline,
  matInventory2Outline,
  matStorefrontOutline,
  matTuneOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon, ConfirmModal],
  providers: [
    provideIcons({
      matBoltOutline,
      matPauseCircleOutline,
      matInventory2Outline,
      matStorefrontOutline,
      matTuneOutline,
    }),
  ],
  selector: 'app-quick-actions',
  styleUrl: './quick-actions.css',
  templateUrl: './quick-actions.html',
})
export class QuickActions {
  private readonly router = inject(Router);
  readonly isPauseModalOpen = signal<boolean>(false);

  handlePauseOrders(): void {
    this.isPauseModalOpen.set(true);
  }

  onConfirmPause(): void {
    this.isPauseModalOpen.set(false);
    this.router.navigate(['/capacity']);
  }

  onDismissPause(): void {
    this.isPauseModalOpen.set(false);
  }
}

