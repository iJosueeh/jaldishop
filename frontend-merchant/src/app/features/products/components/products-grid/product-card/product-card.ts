import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAlarmOutline,
  matEditOutline,
  matInventory2Outline,
  matOpenInNewOutline,
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
      matEditOutline,
      matInventory2Outline,
      matOpenInNewOutline,
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
  viewInventory = output<Product>();
  openPreview = output<Product>();

  onCardClick(): void {
    this.editProduct.emit(this.product());
  }

  onOpenPreview(event?: Event): void {
    event?.stopPropagation();
    this.openPreview.emit(this.product());
  }

  onToggleStatus(event?: Event): void {
    event?.stopPropagation();
    this.toggleStatus.emit(this.product());
  }

  onAdjustQuota(event?: Event): void {
    event?.stopPropagation();
    this.adjustQuota.emit(this.product());
  }

  onEdit(event?: Event): void {
    event?.stopPropagation();
    this.editProduct.emit(this.product());
  }

  readonly capacityBars = computed(() => {
    const p = this.product();
    if (p.quotaToday && p.quotaToday.total > 0) {
      const ratio = Math.max(0, Math.min(1, p.quotaToday.current / p.quotaToday.total));
      const filled = Math.round(ratio * 4);
      return [1, 2, 3, 4].map((bar) => ({
        active: bar <= filled,
        warning: ratio >= 0.8,
      }));
    }
    const isAvailable = p.status === 'ACTIVE' && !p.hasStockIssue;
    return [1, 2, 3, 4].map(() => ({
      active: isAvailable,
      warning: false,
    }));
  });

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

  readonly stockInfo = computed(() => {
    const p = this.product();
    if (p.hasStockIssue) {
      return {
        isOutOfStock: true,
        isControlled: true,
        label: 'Agotado',
        detail: p.stockIssueMessage || 'Sin existencias en tienda',
      };
    }

    const tracksStock =
      (p.variants && p.variants.some((v) => v.tracksInventory)) || p.stockCount !== undefined;

    if (tracksStock) {
      const qty = p.stockCount ?? p.preparedCount ?? 0;
      if (qty <= 0) {
        return {
          isOutOfStock: true,
          isControlled: true,
          label: 'Agotado',
          detail: 'Sin existencias en tienda',
        };
      }
      return {
        isOutOfStock: false,
        isControlled: true,
        label: `${qty} disponibles`,
        detail: 'Existencias físicas en tienda',
        count: qty,
      };
    }

    if (p.quotaToday && p.quotaToday.total > 0) {
      return {
        isOutOfStock: false,
        isControlled: true,
        label: `Cupo hoy: ${p.quotaToday.current} de ${p.quotaToday.total}`,
        detail: 'Capacidad diaria programada',
      };
    }

    return {
      isOutOfStock: false,
      isControlled: false,
      label: 'Bajo pedido / Sin límite',
      detail: 'Disponible para venta continua',
    };
  });

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
