import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matArrowForwardOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-products-alert-banner',
  standalone: true,
  imports: [CommonModule, NgIcon],
  viewProviders: [
    provideIcons({
      matArrowForwardOutline,
      matWarningOutline,
    }),
  ],
  templateUrl: './products-alert-banner.html',
  styleUrl: './products-alert-banner.css',
})
export class ProductsAlertBanner {
  attentionCount = input<number>(0);
  attentionMessage = input<string>(
    '1 producto pausado automáticamente por falta de insumos (Cheesecake de Maracuyá requiere pulpa) y 2 productos con cupos limitados para el turno tarde.',
  );

  viewDetails = output<void>();
}
