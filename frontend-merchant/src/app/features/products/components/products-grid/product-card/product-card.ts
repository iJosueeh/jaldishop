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
    if (p.minPrice != null && p.maxPrice != null && p.minPrice !== p.maxPrice) {
      return `S/ ${p.minPrice.toFixed(2)} - S/ ${p.maxPrice.toFixed(2)}`;
    }
    if (p.minPrice != null) {
      return `S/ ${p.minPrice.toFixed(2)}`;
    }
    if (p.variants && p.variants.length > 0) {
      const activeVars = p.variants.filter((v) => v.status === 'ACTIVE');
      const targetVars = activeVars.length > 0 ? activeVars : p.variants;
      const prices = targetVars.map((v) => v.priceAmount).filter((amt) => amt != null);
      if (prices.length > 0) {
        const min = Math.min(...prices);
        const max = Math.max(...prices);
        return min !== max ? `S/ ${min.toFixed(2)} - S/ ${max.toFixed(2)}` : `S/ ${min.toFixed(2)}`;
      }
    }
    return 'S/ 0.00';
  });

  readonly isPaused = computed(() => this.product().status === 'INACTIVE');

  readonly mainImageUrl = computed(() => {
    const p = this.product();
    if (p.imageUrl) return p.imageUrl;
    if (p.images && p.images.length > 0) {
      const primary = p.images.find((img) => img.isPrimary);
      return primary ? primary.imageUrl : p.images[0].imageUrl;
    }
    return null;
  });
}
