import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matEditOutline,
  matFilterAltOffOutline,
  matInventory2Outline,
  matMoreVertOutline,
  matVisibilityOffOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';
import { Product } from '../../../../core/models/product.models';

@Component({
  selector: 'app-products-list',
  standalone: true,
  imports: [CommonModule, NgIcon],
  viewProviders: [
    provideIcons({
      matAddOutline,
      matEditOutline,
      matFilterAltOffOutline,
      matInventory2Outline,
      matMoreVertOutline,
      matVisibilityOffOutline,
      matVisibilityOutline,
    }),
  ],
  templateUrl: './products-list.html',
  styleUrl: './products-list.css',
})
export class ProductsList {
  products = input<Product[]>([]);
  isLoading = input<boolean>(false);
  isEmptyCatalog = input<boolean>(false);
  isFilterEmpty = input<boolean>(false);

  editProduct = output<Product>();
  toggleStatus = output<Product>();
  createNewProduct = output<void>();
  clearFilters = output<void>();

  getPrice(p: Product): string {
    if (p.minPrice != null && p.maxPrice != null && p.minPrice !== p.maxPrice) {
      return `S/ ${p.minPrice.toFixed(2)} - S/ ${p.maxPrice.toFixed(2)}`;
    }
    if (p.minPrice != null) return `S/ ${p.minPrice.toFixed(2)}`;
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
  }

  getImageUrl(p: Product): string | null {
    if (p.imageUrl) return p.imageUrl;
    if (p.images && p.images.length > 0) {
      const primary = p.images.find((img) => img.isPrimary);
      return primary ? primary.imageUrl : p.images[0].imageUrl;
    }
    return null;
  }
}
