import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAlarmOutline,
  matCookieOutline,
  matEditOutline,
  matInventory2Outline,
  matMoreVertOutline,
  matScheduleOutline,
  matTakeoutDiningOutline,
  matTuneOutline,
  matVisibilityOffOutline,
  matVisibilityOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';
import { Product } from '../../../../../core/models/product.models';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, NgIcon],
  viewProviders: [
    provideIcons({
      matAlarmOutline,
      matCookieOutline,
      matEditOutline,
      matInventory2Outline,
      matMoreVertOutline,
      matScheduleOutline,
      matTakeoutDiningOutline,
      matTuneOutline,
      matVisibilityOffOutline,
      matVisibilityOutline,
      matWarningOutline,
    }),
  ],
  templateUrl: './product-card.html',
  styleUrl: './product-card.css',
})
export class ProductCard {
  product = input.required<Product>();

  editProduct = output<Product>();
  adjustQuota = output<Product>();
  toggleStatus = output<Product>();

  readonly displayPrice = computed(() => {
    const p = this.product();
    if (p.minPrice != null) {
      return `S/ ${p.minPrice.toFixed(2)}`;
    }
    if (p.variants && p.variants.length > 0) {
      const first = p.variants[0];
      return `S/ ${first.priceAmount.toFixed(2)}`;
    }
    return 'S/ 0.00';
  });

  readonly isPaused = computed(() => this.product().status === 'INACTIVE');
}
