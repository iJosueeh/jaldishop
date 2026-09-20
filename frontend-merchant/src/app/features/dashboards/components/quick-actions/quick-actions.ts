import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../../core/services/toast.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matBoltOutline,
  matPauseCircleOutline,
  matInventory2Outline,
  matStorefrontOutline,
  matTuneOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [RouterLink, NgIcon],
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
  private readonly toastService = inject(ToastService);

  handlePauseOrders(): void {
    this.toastService.info(
      'Puedes pausar la recepción de pedidos desactivando las franjas horarias en el módulo de Capacidad.',
    );
  }
}

